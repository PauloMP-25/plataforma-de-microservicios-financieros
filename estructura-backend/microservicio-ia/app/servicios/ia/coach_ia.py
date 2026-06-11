"""
servicios/coach_ia.py  ·  v8.0 — FASE 3: Structured Outputs (LUKA)
══════════════════════════════════════════════════════════════════════════════
Coach Financiero — Motor de Ejecución de Prompts con métricas de tokens.

Cambios v8 (FASE 3):
  - _llamar_gemini_api acepta esquema_salida: Optional[Type[BaseModel]].
    Si está presente → Structured Output (response_mime_type + response_schema).
    Si es None       → comportamiento idéntico al v7 (str plano + Markdown).
  - obtener_consejo_ia propaga esquema_salida hacia _llamar_gemini_api.
  - La cuota Redis se descuenta ANTES de la llamada a Gemini, siempre.
  - El Circuit Breaker (pybreaker) sigue activo sobre _llamar_gemini_api.
  - El fallback sigue devolviendo str (MotorReglasLocal); el orquestador
    distingue fallback vs. consejo real por el campo usando_fallback.

Sin cambios:
  - _verificar_cuota_diaria: intacta.
  - _calcular_costo_usd: intacta.
  - _auditar_consumo_tokens: intacta.
  - _ejecutar_fallback: intacta.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import json
import logging
import asyncio
import re
import unicodedata
import uuid
from datetime import datetime, date
from typing import Dict, Any, Optional, Tuple, Type, Union

import google.generativeai as genai
import pybreaker
from pydantic import BaseModel

from app.configuracion import Configuracion, obtener_configuracion
from app.modelos.esquemas import EstadoCoach, NombreModulo
from app.persistencia.cache_redis import CacheRedis
from app.utilidades.excepciones import LimiteDiarioExcedidoError
from app.libreria_comun.modelos.contexto import ContextoEstrategicoIADTO
from app.libreria_comun.modelos.eventos import EventoTransaccionalDTO
from app.mensajeria.publicador_auditoria import publicador_auditoria
from app.servicios.ia.fallbacks.gestor_fallbacks import GestorFallbacks

logger = logging.getLogger(__name__)

config = obtener_configuracion()
gemini_breaker = pybreaker.CircuitBreaker(
    fail_max=config.cb_sliding_window_size // 2,
    reset_timeout=config.cb_wait_duration_open_state_seconds
)


class CoachIA:
    """
    Motor de ejecución para consultas de IA.
    Aplica cuotas, resiliencia con Gemini y Structured Outputs selectivos.
    """

    def __init__(self) -> None:
        config = obtener_configuracion()
        genai.configure(api_key=config.gemini_api_key)
        self._modelo = genai.GenerativeModel(config.gemini_modelo)
        self._config_generacion_default = genai.types.GenerationConfig(
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
        contexto: Optional[ContextoEstrategicoIADTO] = None,
        # ── NUEVO v8: esquema para Structured Outputs ─────────────────────────
        esquema_salida: Optional[Type[BaseModel]] = None,
    ) -> Tuple[Union[str, dict], EstadoCoach, bool]:
        """
        Punto de entrada principal para obtener un consejo de Gemini.

        Parámetros nuevos (v8):
          esquema_salida : Clase Pydantic (no instancia) usada como response_schema.
                           Si es None → comportamiento legacy (str plano).
                           Si tiene valor → Structured Output (retorna dict).

        Retorna
        -------
        Tuple de (consejo, estado_coach, usando_fallback).
          consejo: str  si esquema_salida es None o si hay fallback.
                   dict si esquema_salida fue provisto y Gemini respondió bien.
        """
        # 1. Cortafuegos: módulo decidió que no hace falta IA
        if "[SKIP_IA]" in prompt:
            return prompt.replace("[SKIP_IA]", "").strip(), EstadoCoach.EXITOSO, False

        # 2. Verificar cuota (siempre antes de llamar a Gemini)
        try:
            self._verificar_cuota_diaria(usuario_id, modulo, rol)
        except LimiteDiarioExcedidoError:
            raise
        except Exception as e:
            logger.warning("[COACH-IA] Cuota no verificable (Redis down): %s", e)

        # 3. Llamada a Gemini con Circuit Breaker
        try:
            # pybreaker.call necesita una callable sin args adicionales.
            # Usamos una lambda para capturar prompt y esquema_salida del closure.
            consejo_raw, in_tokens, out_tokens = gemini_breaker.call(
                lambda: self._llamar_gemini_api(prompt, esquema_salida)
            )

            total_tokens = in_tokens + out_tokens
            costo_usd = self._calcular_costo_usd(in_tokens, out_tokens)

            logger.info(
                "\n========================================================\n"
                "[CONSEJO-IA-TOKENS] Consumo de Tokens para el consejo:\n"
                "  - Usuario ID: %s\n"
                "  - Módulo: %s\n"
                "  - Structured Output: %s\n"
                "  - Tokens Entrada: %d | Salida: %d | Total: %d\n"
                "  - Costo USD: $%.6f\n"
                "========================================================",
                usuario_id, modulo.value,
                esquema_salida.__name__ if esquema_salida else "No",
                in_tokens, out_tokens, total_tokens, costo_usd,
            )

            # Acumular costo diario en Redis
            try:
                today = datetime.now().date()
                clave_costo = f"ia:costo:diario:{today}"
                costo_acumulado = self._cache_redis.obtener(clave_costo)
                nuevo_costo = (float(costo_acumulado) if costo_acumulado else 0.0) + costo_usd
                self._cache_redis.guardar(clave_costo, str(nuevo_costo), ex=172800)
            except Exception as e:
                logger.warning("[COACH-IA] No se pudo actualizar costo diario en Redis: %s", e)

            await self._auditar_consumo_tokens(usuario_id, modulo.value, total_tokens, costo_usd)

            return consejo_raw, EstadoCoach.EXITOSO, False

        except pybreaker.CircuitBreakerError:
            logger.error("[COACH-IA] Circuit Breaker ABIERTO — Gemini no disponible.")
            fallback = self._ejecutar_fallback(
                usuario_id, modulo, datos_para_hash, "Breaker Abierto", nombres, contexto
            )
            return fallback, EstadoCoach.NO_DISPONIBLE, True

        except Exception as exc:
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
            fallback = self._ejecutar_fallback(
                usuario_id, modulo, datos_para_hash, str(exc), nombres, contexto
            )
            return fallback, estado, True

    # ── Llamada a Gemini ──────────────────────────────────────────────────────

    def _llamar_gemini_api(
        self,
        prompt: str,
        esquema_salida: Optional[Type[BaseModel]] = None,
    ) -> Tuple[Union[str, dict], int, int]:
        """
        Ejecuta la llamada real a Gemini.

        Bifurcación según esquema_salida:

        Rama A — esquema_salida es None (comportamiento legacy):
          - Añade instrucción de Markdown al prompt.
          - Usa GenerationConfig estándar (text/plain).
          - Retorna (str, in_tokens, out_tokens).

        Rama B — esquema_salida es una clase Pydantic:
          - Configura response_mime_type="application/json".
          - Configura response_schema=esquema_salida.
          - NO añade instrucción de Markdown (el prompt del módulo
            ya incluye las instrucciones de esquema JSON).
          - Parsea respuesta.text como JSON.
          - Retorna (dict, in_tokens, out_tokens).

        En ambas ramas se extraen las métricas de tokens de usage_metadata.
        """
        config = obtener_configuracion()

        if esquema_salida is None:
            # ── Rama A: Comportamiento legacy (str plano + Markdown) ──────────
            prompt_final = (
                f"{prompt}\n\n"
                f"IMPORTANTE: Responde DIRECTAMENTE en formato Markdown. "
                f"NO uses emojis. NO envuelvas la respuesta en JSON ni en bloques de código. "
                f"El texto que escribas será mostrado directamente al usuario."
            )
            generation_config = self._config_generacion_default

            respuesta = self._modelo.generate_content(
                prompt_final,
                generation_config=generation_config,
            )

            usage = respuesta.usage_metadata
            consejo = self._limpiar_texto(respuesta.text)

            logger.info(
                "[COACH-IA] Consejo legacy generado: %d chars | %d→%d tokens.",
                len(consejo), usage.prompt_token_count, usage.candidates_token_count,
            )
            return consejo, usage.prompt_token_count, usage.candidates_token_count

        else:
            # ── Rama B: Structured Output (JSON schema) ───────────────────────
            # response_schema acepta la clase Pydantic directamente (SDK >= 0.5)
            generation_config_structured = genai.types.GenerationConfig(
                max_output_tokens=config.gemini_max_tokens,
                temperature=config.gemini_temperatura,
                response_mime_type="application/json",
                response_schema=esquema_salida,
            )

            respuesta = self._modelo.generate_content(
                prompt,
                generation_config=generation_config_structured,
            )

            usage = respuesta.usage_metadata

            # Gemini garantiza JSON válido cuando response_schema está activo,
            # pero parseamos dentro de try/except como defensa adicional.
            try:
                consejo_dict = json.loads(respuesta.text)
            except json.JSONDecodeError as e:
                logger.error(
                    "[COACH-IA] Structured Output no es JSON válido: %s | raw: %s",
                    e, respuesta.text[:200],
                )
                # Re-lanzar para que el caller active el fallback
                raise

            logger.info(
                "[COACH-IA] Structured Output generado: %d campos | %d→%d tokens.",
                len(consejo_dict), usage.prompt_token_count, usage.candidates_token_count,
            )
            return consejo_dict, usage.prompt_token_count, usage.candidates_token_count

    # ── Métodos auxiliares (sin cambios respecto a v7) ────────────────────────

    def _calcular_costo_usd(self, in_tokens: int, out_tokens: int) -> float:
        config = obtener_configuracion()
        costo_in = (in_tokens / 1_000_000) * config.gemini_costo_input_1m
        costo_out = (out_tokens / 1_000_000) * config.gemini_costo_output_1m
        return round(costo_in + costo_out, 6)

    async def _auditar_consumo_tokens(
        self, usuario_id: str, modulo: str, tokens: int, costo: float
    ):
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
            logger.info("[AUDITORIA-IA] %s consumió %d tokens ($%s)", usuario_id, tokens, costo)
        except Exception as e:
            logger.error("[AUDITORIA-IA] Error al publicar consumo: %s", e)

    def _limpiar_texto(self, texto: str) -> str:
        if not texto:
            return ""
        texto = unicodedata.normalize("NFKC", texto)
        texto = re.sub(r"\n{3,}", "\n\n", texto)
        return texto.strip()

    def _ejecutar_fallback(
        self,
        usuario_id: str,
        modulo: NombreModulo,
        datos: Dict[str, Any],
        error: str,
        nombres: str,
        contexto: Optional[ContextoEstrategicoIADTO] = None,
    ) -> str:
        logger.info("[FALLBACK] Generando consejo estático para %s (Causa: %s)", modulo.value, error)
        return GestorFallbacks.generar_fallback(modulo, datos, nombres, contexto)

    def _verificar_cuota_diaria(
        self, usuario_id: str, modulo: NombreModulo, rol: str
    ) -> None:
        config = obtener_configuracion()
        rol_upper = rol.upper()

        tipo_pool = "clasificacion" if modulo == NombreModulo.AUTO_CLASIFICACION else "analitica"

        if tipo_pool == "analitica":
            limite = 20 if rol_upper == "PRO" else (50 if rol_upper == "PREMIUM" else 5)
        else:
            limite = 10 if rol_upper == "PRO" else (20 if rol_upper == "PREMIUM" else 3)

        fecha_actual = datetime.now()
        anio, semana, _ = fecha_actual.isocalendar()
        clave = f"ia:cuota:{tipo_pool}:{usuario_id}:{anio}:W{semana}"

        try:
            intentos = self._cache_redis.obtener_cuota_actual(clave)

            if intentos >= limite:
                raise LimiteDiarioExcedidoError(
                    mensaje=(
                        f"Has agotado tus {limite} consultas semanales de {tipo_pool}. "
                        f"Tu plan {rol_upper} se renueva el próximo lunes."
                    )
                )

            self._cache_redis.incrementar_cuota(clave, ttl_segundos=604800)
            logger.info("[CUOTA-%s] %s: %d/%d", tipo_pool.upper(), usuario_id, intentos + 1, limite)

        except LimiteDiarioExcedidoError:
            raise
        except Exception as e:
            logger.warning("[COACH-IA] Error al verificar cuota (Redis down): %s", e)
