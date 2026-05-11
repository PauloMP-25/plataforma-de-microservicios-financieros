from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from app.libreria_comun.seguridad.contexto import set_correlation_id
from uuid import uuid4
import logging

logger = logging.getLogger("libreria_comun.seguridad.middleware")

class TraceabilityMiddleware(BaseHTTPMiddleware):
    """
    Middleware de Trazabilidad LUKA.
    Captura y propaga el X-Correlation-ID a través de todo el flujo asíncrono.
    """
    async def dispatch(self, request: Request, call_next):
        # 1. Capturar del header (enviado por API Gateway) o generar fallback
        correl_id = request.headers.get("X-Correlation-ID")
        
        if not correl_id:
            correl_id = str(uuid4())
            logger.debug(f"[TRACE] Generando nuevo Correlation-ID: {correl_id}")
        else:
            logger.debug(f"[TRACE] Capturado Correlation-ID: {correl_id}")

        # 2. Persistir en el contexto asíncrono (ContextVar)
        token = set_correlation_id(correl_id)
        
        try:
            # 3. Procesar la petición
            response = await call_next(request)
            
            # 4. Devolver el ID en los headers de respuesta para debugging
            response.headers["X-Correlation-ID"] = correl_id
            return response
        finally:
            # Limpieza opcional (aunque ContextVar lo maneja por tarea)
            pass
