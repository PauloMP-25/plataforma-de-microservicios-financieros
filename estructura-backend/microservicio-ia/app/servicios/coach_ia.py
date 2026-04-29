"""
servicios/coach_ia.py
══════════════════════════════════════════════════════════════════════════════
Servicio que orquesta la llamada a Google Generative AI (Gemini).

Responsabilidad: recibir un EventoAnalisisIA, construir el prompt
mediante IngenierioPrompt y retornar el consejo generado como string.

No conoce RabbitMQ; solo sabe hablar con Gemini.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import logging
from typing import Optional

import google.generativeai as genai

from app.configuracion import obtener_configuracion
from google.api_core import exceptions as google_exceptions
from app.excepciones import AnalisisFinancieroError, GeminiCuotaExcedidaError, GeminiAutenticacionError
from app.modelos.evento_analisis import EventoAnalisisIA
from app.servicios.ingeniero_prompt import Ingeniero_Prompt

logger = logging.getLogger(__name__)


class CoachIA:
    """
    Wrapper alrededor de Google Generative AI para el coach financiero.

    Instanciar una sola vez y reutilizar (el cliente de Gemini es thread-safe).
    """

    def __init__(self) -> None:
        config = obtener_configuracion()
        genai.configure(api_key=config.gemini_api_key)
        self._modelo = genai.GenerativeModel(config.gemini_modelo)
        self._ingeniero = Ingeniero_Prompt()
        logger.info("[COACH-IA] Modelo Gemini '%s' inicializado.", config.gemini_modelo)

    def generar_consejo(self, evento: EventoAnalisisIA) -> Optional[str]:
        """
        Genera el consejo financiero personalizado para el evento recibido.

        Args:
            evento: EventoAnalisisIA deserializado desde RabbitMQ.

        Returns:
            Texto del consejo listo para WhatsApp, o None si Gemini falla.
        """
        try:
            prompt = self._ingeniero.construir(evento)
            logger.debug(
                "[COACH-IA] Enviando prompt para transacción '%s' (S/ %.2f)",
                evento.transaccion.descripcion,
                evento.transaccion.monto,
            )

            respuesta = self._modelo.generate_content(prompt)
            consejo = respuesta.text.strip()

            logger.info(
                "[COACH-IA] Consejo generado (%d chars) para '%s'",
                len(consejo),
                evento.transaccion.descripcion,
            )
            return consejo
        
        except google_exceptions.ResourceExhausted:
            logger.warning("[COACH-IA] Cuota de Gemini agotada (429).")
            raise GeminiCuotaExcedidaError()
            
        except google_exceptions.Unauthenticated:
            logger.error("[COACH-IA] Error de autenticación. Revisa la API Key.")
            raise GeminiAutenticacionError()
            
        except (google_exceptions.InvalidArgument, google_exceptions.DeadlineExceeded) as exc:
            logger.error(f"[COACH-IA] Error técnico en la petición: {str(exc)}")
            raise AnalisisFinancieroError("Error al conectar con el motor de IA.", detalles=str(exc))

        except Exception as exc:
            logger.error(f"[COACH-IA] Error crítico no controlado: {str(exc)}", exc_info=True)
            return None