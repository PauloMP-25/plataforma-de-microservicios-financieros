"""
servicios/coach_ia.py  ·  v6.0 — Control de Costos y Traceability
══════════════════════════════════════════════════════════════════════════════
Coach Financiero — Motor de Ejecución de Prompts con métricas de tokens.

Responsabilidades:
  - Gestionar la conexión con Google Gemini.
  - Aplicar doble capa de caché (Redis + PostgreSQL).
  - Calcular consumo de tokens y costo en USD.
  - Notificar auditoría de consumo (Traceability).
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations
import logging
import asyncio
from typing import Optional, Tuple, Any, Dict
from datetime import datetime
import google.generativeai as genai
import pybreaker
import unicodedata
import re
import uuid

from app.configuracion import Configuracion, obtener_configuracion
from app.modelos.esquemas import EstadoCoach, NombreModulo
from app.persistencia.cache_redis import CacheRedis
from app.persistencia.database import SessionLocal
from app.persistencia.modelos_db import IaAnalisisCache
from app.utilidades.hash_util import generar_hash_datos
from app.libreria_comun.modelos.eventos import EventoTransaccionalDTO
from app.mensajeria.publicador_auditoria import publicador_auditoria

logger = logging.getLogger(__name__)

# Configuración del Circuit Breaker
config = obtener_configuracion()
gemini_breaker = pybreaker.CircuitBreaker(
    fail_max=config.cb_sliding_window_size // 2,
    reset_timeout=config.cb_wait_duration_open_state_seconds
)

class CoachIA:
    """
    Motor de ejecución para consultas de IA. 
    Aplica caché, resiliencia y gobernanza de cuotas.
    """

    def __init__(self) -> None:
        config = obtener_configuracion()
        genai.configure(api_key=config.gemini_api_key)
        self._modelo = genai.GenerativeModel(config.gemini_modelo)
        self._config_generacion = genai.types.GenerationConfig(
            max_output_tokens=config.gemini_max_tokens,
            temperature=config.gemini_temperatura,
        )
        self._cache_redis = CacheRedis()

    async def obtener_consejo_ia(
        self,
        usuario_id: str,
        modulo: NombreModulo,
        prompt: str,
        datos_para_hash: Dict[str, Any],
        rol: str = "FREE"
    ) -> Tuple[str, EstadoCoach, bool]:
        """
        Punto de entrada principal para obtener un consejo.
        Ahora es asíncrono para soportar auditoría AMQP.
        """
        # 1. Cortafuegos: Si el módulo decidió que no hace falta IA
        if "[SKIP_IA]" in prompt:
            return prompt.replace("[SKIP_IA]", "").strip(), EstadoCoach.EXITOSO, False

        # 2. Caché: Generar hash único para esta consulta
        hash_datos = generar_hash_datos(datos_para_hash)
        consejo, estado, fallback = self._obtener_de_cache(hash_datos)
        
        if consejo:
            return consejo, estado, fallback

        # 3. Verificar Cuota Diaria (Gobernanza)
        try:
            self._verificar_cuota_diaria(usuario_id, modulo, rol)
        except ValueError as e:
            logger.warning(f"[COACH-IA] Cuota bloqueada: {e}")
            return str(e), EstadoCoach.NO_DISPONIBLE, False

        # 4. Ejecución: Llamada a Gemini con protección
        try:
            # Gemini breaker call needs to handle the tuple return
            consejo_texto, in_tokens, out_tokens = gemini_breaker.call(self._llamar_gemini_api, prompt)
            
            total_tokens = in_tokens + out_tokens
            costo_usd = self._calcular_costo_usd(in_tokens, out_tokens)

            # Persistir éxito
            self._cache_redis.guardar_consejo(hash_datos, consejo_texto)
            self._guardar_en_db(hash_datos, usuario_id, modulo.value, prompt, consejo_texto, False, total_tokens, costo_usd)
            
            # Auditoría de tokens (Traceability)
            await self._auditar_consumo_tokens(usuario_id, modulo.value, total_tokens, costo_usd)

            return consejo_texto, EstadoCoach.EXITOSO, False

        except pybreaker.CircuitBreakerError:
            logger.error(f"[COACH-IA] Circuit Breaker ABIERTO.")
            return self._ejecutar_fallback(usuario_id, modulo, datos_para_hash, "Breaker Abierto"), EstadoCoach.TIMEOUT, True
        except Exception as exc:
            logger.error(f"[COACH-IA] Error en Gemini: {exc}")
            return self._ejecutar_fallback(usuario_id, modulo, datos_para_hash, str(exc)), EstadoCoach.TIMEOUT, True

    def _llamar_gemini_api(self, prompt: str) -> Tuple[str, int, int]:
        respuesta = self._modelo.generate_content(
            prompt,
            generation_config=self._config_generacion,
        )
        
        # Extraer métricas de tokens
        usage = respuesta.usage_metadata
        in_tokens = usage.prompt_token_count
        out_tokens = usage.candidates_token_count
        
        return self._limpiar_texto(respuesta.text), in_tokens, out_tokens

    def _calcular_costo_usd(self, in_tokens: int, out_tokens: int) -> float:
        config = obtener_configuracion()
        costo_in = (in_tokens / 1_000_000) * config.gemini_costo_input_1m
        costo_out = (out_tokens / 1_000_000) * config.gemini_costo_output_1m
        return round(costo_in + costo_out, 6)

    async def _auditar_consumo_tokens(self, usuario_id: str, modulo: str, tokens: int, costo: float):
        """Envía un evento de auditoría transaccional para el consumo de tokens."""
        try:
            evento = EventoTransaccionalDTO(
                usuario_id=usuario_id,
                entidad_id=str(uuid.uuid4()),
                servicio_origen="MS-IA",
                entidad_afectada="ia_analisis_cache",
                descripcion=f"CONSUMO_TOKENS_GEMINI_{modulo}",
                valor_anterior="0",
                valor_nuevo=str(tokens),
                fecha=datetime.now()
            )
            await publicador_auditoria.publicar_evento(evento)
            logger.info(f"[AUDITORIA-IA] {usuario_id} consumió {tokens} tokens (${costo})")
        except Exception as e:
            logger.error(f"[AUDITORIA-IA] Error al publicar consumo: {e}")

    def _limpiar_texto(self, texto: str) -> str:
        if not texto: return ""
        texto = unicodedata.normalize('NFKC', texto)
        texto = re.sub(r'\n{3,}', '\n\n', texto)
        return texto.strip()

    def _obtener_de_cache(self, hash_datos: str) -> Tuple[Optional[str], EstadoCoach, bool]:
        try:
            res = self._cache_redis.obtener_consejo(hash_datos)
            if res: return res, EstadoCoach.EXITOSO, False
        except: pass

        try:
            with SessionLocal() as db:
                c = db.query(IaAnalisisCache).filter(IaAnalisisCache.hash_datos == hash_datos).first()
                if c:
                    self._cache_redis.guardar_consejo(hash_datos, c.consejo_gemini)
                    return c.consejo_gemini, EstadoCoach.EXITOSO, c.usando_fallback
        except Exception as e:
            logger.warning(f"[COACH-IA] DB Cache No disponible: {e}")
        return None, EstadoCoach.NO_DISPONIBLE, False

    def _guardar_en_db(self, hash_datos: str, cliente_id: str, modulo: str, prompt: str, consejo: str, fallback: bool, tokens: int = 0, costo: float = 0.0):
        try:
            with SessionLocal() as db:
                nuevo = IaAnalisisCache(
                    hash_datos=hash_datos, cliente_id=cliente_id, modulo=modulo,
                    prompt_usado=prompt, consejo_gemini=consejo, usando_fallback=fallback,
                    tokens_usados=tokens, costo_usd=costo
                )
                db.merge(nuevo)
                db.commit()
        except Exception as e:
            logger.warning(f"[COACH-IA] No se pudo persistir en DB: {e}")

    def _ejecutar_fallback(self, usuario_id: str, modulo: NombreModulo, datos: Dict[str, Any], error: str) -> str:
        return f"Paulo, por ahora no puedo darte un consejo detallado, pero sigo analizando tus {modulo.value}. ¡Mantén la disciplina!"

    def _verificar_cuota_diaria(self, usuario_id: str, modulo: NombreModulo, rol: str) -> None:
        config = obtener_configuracion()
        rol_upper = rol.upper()
        if rol_upper == "PRO":
            limite = config.cuota_clasif_pro_semanal
        elif rol_upper == "PREMIUM":
            limite = config.cuota_clasif_premium_semanal
        else:
            limite = 1
        
        tipo_pool = "clasificacion" if modulo == NombreModulo.AUTO_CLASIFICACION else "analitica"
        
        fecha_actual = datetime.now()
        anio, semana, _ = fecha_actual.isocalendar()
        clave = f"ia:cuota:{tipo_pool}:{usuario_id}:{anio}:W{semana}"
        
        try:
            intentos = self._cache_redis.obtener(clave)
            intentos_int = int(intentos) if intentos else 0
            
            if intentos_int >= limite:
                mensaje_error = (
                    f"Has agotado tus {limite} intentos semanales de {tipo_pool}. "
                    f"Tu plan {rol_upper} se renovará el próximo lunes."
                )
                raise ValueError(mensaje_error)
            
            self._cache_redis.guardar(clave, intentos_int + 1, ex=604800) # 7 días
            logger.info(f"[CUOTA-{tipo_pool.upper()}] {usuario_id}: {intentos_int + 1}/{limite}")
            
        except ValueError:
            raise
        except Exception as e:
            logger.warning(f"[COACH-IA] Error al verificar cuota (Redis down): {e}")