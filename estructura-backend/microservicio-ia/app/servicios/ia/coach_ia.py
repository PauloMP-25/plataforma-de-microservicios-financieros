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
import json
from typing import Optional, Tuple, Any, Dict
from datetime import datetime, date
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
from app.utilidades.excepciones import LimiteDiarioExcedidoError
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.libreria_comun.modelos.eventos import EventoTransaccionalDTO
from app.mensajeria.publicador_auditoria import publicador_auditoria
from app.servicios.ia.motor_reglas import MotorReglasLocal

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
            response_mime_type="application/json",
        )
        self._cache_redis = CacheRedis()

    async def obtener_consejo_ia(
        self,
        usuario_id: str,
        modulo: NombreModulo,
        prompt: str,
        datos_para_hash: Dict[str, Any],
        rol: str = "FREE",
        nombres: str = "estudiante",
        contexto: Optional[ContextoEstrategicoIADTO] = None
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

        # 3. Verificar Cuota Diaria (Gobernanza) — lanza LimiteDiarioExcedidoError si se excede
        try:
            self._verificar_cuota_diaria(usuario_id, modulo, rol)
        except LimiteDiarioExcedidoError:
            raise  # El handler global la captura → HTTP 429
        except Exception as e:
            logger.warning(f"[COACH-IA] Cuota no verificable (Redis down): {e}")
            # Si Redis está caído, permitimos la consulta (fail-open)

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
            logger.error("[COACH-IA] Circuit Breaker ABIERTO — Gemini no disponible.")
            return self._ejecutar_fallback(usuario_id, modulo, datos_para_hash, "Breaker Abierto", nombres, contexto), EstadoCoach.NO_DISPONIBLE, True
        except Exception as exc:
            # Discriminar tipo de error Gemini para trazabilidad
            tipo_error = type(exc).__name__
            if "ResourceExhausted" in tipo_error or "429" in str(exc):
                estado = EstadoCoach.CUOTA_AGOTADA
            elif "InvalidArgument" in tipo_error or "400" in str(exc):
                estado = EstadoCoach.AUTH_ERROR
            elif "DeadlineExceeded" in tipo_error or "timeout" in str(exc).lower():
                estado = EstadoCoach.TIMEOUT
            else:
                estado = EstadoCoach.NO_DISPONIBLE
            logger.error("[COACH-IA] Gemini %s: %s", tipo_error, exc)
            return self._ejecutar_fallback(usuario_id, modulo, datos_para_hash, str(exc), nombres, contexto), estado, True

    def _llamar_gemini_api(self, prompt: str) -> Tuple[str, int, int]:
        # FASE 6: Exigir esquema JSON estricto para evitar fallos de parseo
        prompt_json = (
            f"{prompt}\n\n"
            f"IMPORTANTE: Debes responder ÚNICAMENTE con un objeto JSON válido "
            f"que contenga exactamente esta estructura: {{\"consejo\": \"<Tu consejo formateado en markdown>\"}}."
        )
        
        respuesta = self._modelo.generate_content(
            prompt_json,
            generation_config=self._config_generacion,
        )
        
        # Extraer métricas de tokens
        usage = respuesta.usage_metadata
        in_tokens = usage.prompt_token_count
        out_tokens = usage.candidates_token_count
        
        # Parsear JSON de forma segura
        texto_crudo = self._limpiar_texto(respuesta.text)
        texto_limpio = texto_crudo.strip()
        
        # Eliminar bloques de código markdown ```json ... ``` si están presentes
        if texto_limpio.startswith("```"):
            idx_salto = texto_limpio.find("\n")
            if idx_salto != -1:
                texto_limpio = texto_limpio[idx_salto:].strip()
            if texto_limpio.endswith("```"):
                texto_limpio = texto_limpio[:-3].strip()

        try:
            datos_json = json.loads(texto_limpio)
            consejo_final = datos_json.get("consejo", texto_limpio)
        except json.JSONDecodeError:
            logger.warning("[COACH-IA] Gemini no devolvió JSON válido. Intentando extraer el campo 'consejo' manualmente.")
            # Intento de extracción manual como contingencia
            match = re.search(r'"consejo"\s*:\s*"(.*?)"', texto_limpio, re.DOTALL)
            if match:
                consejo_final = match.group(1).replace('\\"', '"').replace('\\n', '\n')
            else:
                consejo_final = texto_limpio
            
        return consejo_final.strip(), in_tokens, out_tokens

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
                fecha=date.today()
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

    def _ejecutar_fallback(self, usuario_id: str, modulo: NombreModulo, datos: Dict[str, Any], error: str, nombres: str, contexto: Optional[ContextoEstrategicoIADTO] = None) -> str:
        # FASE 8: Motor de Reglas Local (Graceful Degradation)
        logger.info(f"[FALLBACK] Generando consejo estático para {modulo.value} (Causa: {error})")
        return MotorReglasLocal.generar_fallback(modulo, datos, nombres, contexto)

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
                    f"Has agotado tus {limite} consultas semanales de {tipo_pool}. "
                    f"Tu plan {rol_upper} se renueva el próximo lunes."
                )
                raise LimiteDiarioExcedidoError(mensaje=mensaje_error)
            
            self._cache_redis.guardar(clave, intentos_int + 1, ex=604800) # 7 días
            logger.info(f"[CUOTA-{tipo_pool.upper()}] {usuario_id}: {intentos_int + 1}/{limite}")
            
        except ValueError:
            raise
        except Exception as e:
            logger.warning(f"[COACH-IA] Error al verificar cuota (Redis down): {e}")