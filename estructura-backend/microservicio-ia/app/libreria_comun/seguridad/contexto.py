"""
libreria_comun/seguridad/contexto.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Propagación del X-Correlation-ID a través del flujo asíncrono de FastAPI.

Por qué ContextVar y no threading.local():
  FastAPI es asíncrono (asyncio). En un entorno async, una corutina puede
  pausarse y reanudar en un hilo diferente, lo que hace que threading.local()
  no sea confiable. ContextVar es la solución estándar de Python para
  variables de contexto en código async (PEP 567).

Flujo de vida del Correlation-ID:
  1. TraceabilityMiddleware captura el header X-Correlation-ID de la petición
     (o genera uno nuevo con uuid4 si no viene del API Gateway).
  2. El ID se almacena en esta ContextVar, que vive durante toda la corutina.
  3. Cada fase del pipeline llama a get_correlation_id() para incluirlo en logs.
  4. Si el microservicio llama a otro (ms-financiero, ms-cliente), el ID se
     inyecta en el header X-Correlation-ID de la petición saliente.

Uso:
    # En el middleware (al inicio de cada request):
    token = set_correlation_id("mi-uuid-desde-gateway")

    # En cualquier punto del pipeline (logs, llamadas HTTP):
    correl_id = get_correlation_id()
    logger.info("[PIPELINE] Procesando | trace=%s", correl_id)
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from contextvars import ContextVar, Token
from uuid import uuid4

# ── Variable de contexto global ───────────────────────────────────────────────
# El valor por defecto es un UUID aleatorio como fallback de seguridad.
# Esto garantiza que get_correlation_id() nunca retorne None, incluso si
# el middleware no fue ejecutado (ej: tests unitarios, consumidores RabbitMQ).
_correlation_id: ContextVar[str] = ContextVar(
    "correlation_id",
    default="no-trace-id",
)


def get_correlation_id() -> str:
    """
    Retorna el X-Correlation-ID activo para la corutina/hilo actual.

    Garantía: nunca retorna None. Si no fue configurado por el middleware,
    retorna el valor por defecto "no-trace-id" (visible en logs como señal
    de que falta configuración del middleware).

    Returns:
        String con el Correlation-ID (UUID o valor configurado por el middleware).
    """
    return _correlation_id.get()


def set_correlation_id(valor: str) -> Token:
    """
    Establece el X-Correlation-ID para la corutina actual.

    Llamado por TraceabilityMiddleware al inicio de cada petición HTTP
    y por los consumidores RabbitMQ al procesar cada mensaje.

    Args:
        valor: El Correlation-ID extraído del header X-Correlation-ID
               o generado con uuid4() como fallback.

    Returns:
        Token que permite restaurar el valor anterior con reset().
        Útil en tests para aislar los IDs entre casos de prueba.

    Ejemplo en tests:
        token = set_correlation_id("test-abc-123")
        try:
            servicio.run_pipeline(...)
        finally:
            _correlation_id.reset(token)  # Restaura el estado anterior
    """
    if not valor or not valor.strip():
        valor = f"generated-{uuid4()}"
    return _correlation_id.set(valor.strip())


def generar_nuevo_id() -> str:
    """
    Genera un nuevo Correlation-ID único y lo establece como el activo.

    Útil como fallback cuando el header X-Correlation-ID no viene en la
    petición (ej: llamadas directas desde el frontend sin API Gateway).

    Returns:
        String con el nuevo UUID generado.
    """
    nuevo_id = str(uuid4())
    set_correlation_id(nuevo_id)
    return nuevo_id