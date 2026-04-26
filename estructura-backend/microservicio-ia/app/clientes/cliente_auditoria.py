"""
cliente_auditoria.py — Cliente HTTP asíncrono para el microservicio-auditoria

Usa httpx.AsyncClient (equivalente Python de RestTemplate + @Async de Java).
El envío es no bloqueante: un fallo de auditoría nunca interrumpe el flujo.
"""

import httpx
import logging
from datetime import datetime
from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)

MODULO = "MICROSERVICIO-IA"


async def enviar_evento_auditoria(
    nombre_usuario: str,
    accion: str,
    detalles: str,
    ip_origen: str | None = None,
) -> None:
    """
    Envía un evento de auditoría de forma asíncrona y no bloqueante.

    En Java usabas @Async + RestTemplate.
    En Python usamos async/await + httpx para el mismo efecto.

    Args:
        nombre_usuario: Identificador del usuario (nombre o ID)
        accion:         Código de la acción (ej: "ANALISIS_IA_COMPLETADO")
        detalles:       Descripción detallada del evento
        ip_origen:      IP del cliente (opcional)
    """
    config = obtener_configuracion()

    payload = {
        "fechaHora": datetime.now().isoformat(),
        "nombreUsuario": nombre_usuario,
        "accion": accion,
        "modulo": MODULO,
        "ipOrigen": ip_origen,
        "detalles": detalles,
    }

    try:
        # timeout=5.0: no esperamos más de 5 segundos por auditoría
        async with httpx.AsyncClient(timeout=5.0) as client:
            respuesta = await client.post(
                config.auditoria_service_url,
                json=payload
            )
            respuesta.raise_for_status()
            logger.debug(
                "[AUDITORIA] Evento enviado: accion=%s, usuario=%s",
                accion, nombre_usuario
            )

    except httpx.TimeoutException:
        # No propagamos: la auditoría es informativa
        logger.error(
            "[AUDITORIA] Timeout al enviar evento (no bloqueante): accion=%s",
            accion
        )
    except httpx.HTTPStatusError as ex:
        logger.error(
            "[AUDITORIA] Error HTTP %s al enviar evento: %s",
            ex.response.status_code, str(ex)
        )
    except Exception as ex:
        logger.error(
            "[AUDITORIA] Fallo inesperado al enviar evento (no bloqueante): %s",
            str(ex)
        )
