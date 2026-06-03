"""
servicios/coach_ia.py  ·  v7.0 — Restauración y Control de Costos en Redis
══════════════════════════════════════════════════════════════════════════════
Coach Financiero — Motor de Ejecución de Prompts con métricas de tokens.

Responsabilidades:
  - Gestionar la conexión con Google Gemini.
  - Aplicar gobernanza de cuotas en Redis.
  - Calcular consumo de tokens y costo en USD.
  - Notificar auditoría de consumo (Traceability).
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations
import logging
import asyncio
import re
from typing import Optional, Tuple, Any, Dict
from datetime import datetime, date
import google.generativeai as genai
import pybreaker
import unicodedata
import uuid

from app.configuracion import Configuracion, obtener_configuracion
from app.modelos.esquemas import EstadoCoach, NombreModulo
from app.persistencia.cache_redis import CacheRedis
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
    Aplica cuotas y resiliencia con Gemini.
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
        rol: str = "FREE",
        nombres: str = "estudiante",
        contexto: Optional[ContextoEstrategicoIADTO] = None
    ) -> Tuple[str, EstadoCoach, bool]:
        """
        Punto de entrada principal para obtener un consejo de Gemini.
        """
        # 1. Cortafuegos: Si el módulo decidió que no hace falta IA
        if "[SKIP_IA]" in prompt:
            return prompt.replace("[SKIP_IA]", "").strip(), EstadoCoach.EXITOSO, False

        # 2. Verificar Cuota Diaria (Gobernanza) — lanza LimiteDiarioExcedidoError si se excede
        try:
            self._verificar_cuota_diaria(usuario_id, modulo, rol)
        except LimiteDiarioExcedidoError:
            raise  # El handler global la captura → HTTP 429
        except Exception as e:
            logger.warning(f"[COACH-IA] Cuota no verificable (Redis down): {e}")
            # Si Redis está caído, permitimos la consulta (fail-open)

        # 3. Ejecución: Llamada a Gemini con protección
        try:
            # Gemini breaker call needs to handle the tuple return
            consejo_texto, in_tokens, out_tokens = gemini_breaker.call(self._llamar_gemini_api, prompt)
            
            total_tokens = in_tokens + out_tokens
            costo_usd = self._calcular_costo_usd(in_tokens, out_tokens)

            # Log para los dueños del sistema (Consola)
            logger.info(
                "\n========================================================\n"
                "[CONSEJO-IA-TOKENS] Consumo de Tokens para el consejo:\n"
                f"  - Usuario ID: {usuario_id}\n"
                f"  - Módulo: {modulo.value}\n"
                f"  - Tokens de Entrada (Prompt): {in_tokens}\n"
                f"  - Tokens de Salida (Respuesta): {out_tokens}\n"
                f"  - Total Tokens: {total_tokens}\n"
                f"  - Costo USD: ${costo_usd:.6f}\n"
                "========================================================"
            )

            # Registrar/incrementar costo acumulado diario en Redis
            try:
                today = datetime.now().date()
                clave_costo = f"ia:costo:diario:{today}"
                costo_acumulado = self._cache_redis.obtener(clave_costo)
                nuevo_costo = (float(costo_acumulado) if costo_acumulado else 0.0) + costo_usd
                self._cache_redis.guardar(clave_costo, str(nuevo_costo), ex=172800)  # 2 días TTL
            except Exception as e:
                logger.warning(f"[COACH-IA] No se pudo actualizar costo diario en Redis: {e}")
            
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
        prompt_final = (
            f"{prompt}\n\n"
            f"IMPORTANTE: Responde DIRECTAMENTE en formato Markdown. "
            f"NO uses emojis. NO envuelvas la respuesta en JSON ni en bloques de código. "
            f"El texto que escribas será mostrado directamente al usuario."
        )

        respuesta = self._modelo.generate_content(
            prompt_final,
            generation_config=self._config_generacion,
        )

        # Extraer métricas de tokens
        usage = respuesta.usage_metadata
        in_tokens = usage.prompt_token_count
        out_tokens = usage.candidates_token_count

        consejo_final = self._limpiar_texto(respuesta.text)

        logger.info(
            "[COACH-IA] Consejo generado: %d caracteres | %d tokens entrada | %d tokens salida.",
            len(consejo_final), in_tokens, out_tokens
        )
        return consejo_final, in_tokens, out_tokens

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
                entidad_afectada="ia_cuotas",
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

    def _ejecutar_fallback(self, usuario_id: str, modulo: NombreModulo, datos: Dict[str, Any], error: str, nombres: str, contexto: Optional[ContextoEstrategicoIADTO] = None) -> str:
        logger.info(f"[FALLBACK] Generando consejo estático para {modulo.value} (Causa: {error})")
        return MotorReglasLocal.generar_fallback(modulo, datos, nombres, contexto)

    def _verificar_cuota_diaria(self, usuario_id: str, modulo: NombreModulo, rol: str) -> None:
        config = obtener_configuracion()
        rol_upper = rol.upper()
        
        tipo_pool = "clasificacion" if modulo == NombreModulo.AUTO_CLASIFICACION else "analitica"
        
        if tipo_pool == "analitica":
            if rol_upper == "PRO":
                limite = 20
            elif rol_upper == "PREMIUM":
                limite = 50
            else:
                limite = 5
        else: # clasificacion
            if rol_upper == "PRO":
                limite = 10
            elif rol_upper == "PREMIUM":
                limite = 20
            else:
                limite = 3
        
        fecha_actual = datetime.now()
        anio, semana, _ = fecha_actual.isocalendar()
        clave = f"ia:cuota:{tipo_pool}:{usuario_id}:{anio}:W{semana}"
        
        try:
            intentos = self._cache_redis.obtener_cuota_actual(clave)
            
            if intentos >= limite:
                mensaje_error = (
                    f"Has agotado tus {limite} consultas semanales de {tipo_pool}. "
                    f"Tu plan {rol_upper} se renueva el próximo lunes."
                )
                raise LimiteDiarioExcedidoError(mensaje=mensaje_error)
            
            self._cache_redis.incrementar_cuota(clave, ttl_segundos=604800)  # 7 días TTL
            logger.info(f"[CUOTA-{tipo_pool.upper()}] {usuario_id}: {intentos + 1}/{limite}")
            
        except LimiteDiarioExcedidoError:
            raise
        except Exception as e:
            logger.warning(f"[COACH-IA] Error al verificar cuota (Redis down): {e}")