"""
libreria_comun/excepciones/handler.py  ·  v3.0 — FASE 3 (LUKA-COACH V4)
══════════════════════════════════════════════════════════════════════════════
Manejador global de excepciones para FastAPI.

Captura (en orden de prioridad):
  1. LukaException y subclases → mapeo automático por codigo_error
  2. pydantic.ValidationError  → 422 / DATOS_INVALIDOS
  3. google.api_core.exceptions → 502 / ERROR_SERVICIO_EXTERNO
  4. Exception genérica        → 500 / ERROR_INTERNO_SERVIDOR

Mapeo codigo_error → HTTP Status (espejo de CodigoError.java):
  HISTORIAL_INSUFICIENTE  → 400
  DATOS_INVALIDOS         → 400  (también ValidationError de Pydantic)
  ERROR_VALIDACION        → 400
  ACCESO_DENEGADO         → 403
  ACCESO_NO_AUTORIZADO    → 401
  RECURSO_NO_ENCONTRADO   → 404
  LIMITE_DIARIO_EXCEDIDO  → 429
  ERROR_SERVICIO_EXTERNO  → 502
  ERROR_INTERNO           → 500
  ERROR_INTERNO_SERVIDOR  → 500
  (cualquier otro)        → 400
══════════════════════════════════════════════════════════════════════════════
"""
import logging
from fastapi import Request, status
from fastapi.responses import JSONResponse
from pydantic import ValidationError

from app.libreria_comun.respuesta.resultado_api import ResultadoApi
from app.libreria_comun.excepciones.base import LukaException

logger = logging.getLogger("libreria_comun.excepciones")

# ── Tabla de mapeo codigo_error → HTTP Status ─────────────────────────────────
_MAPA_HTTP: dict[str, int] = {
    # 400 Bad Request
    "HISTORIAL_INSUFICIENTE":   status.HTTP_400_BAD_REQUEST,
    "DATOS_INVALIDOS":          status.HTTP_400_BAD_REQUEST,
    "ERROR_VALIDACION":         status.HTTP_400_BAD_REQUEST,
    "DATOS_MALFORMADOS":        status.HTTP_400_BAD_REQUEST,
    "VALOR_INVALIDO":           status.HTTP_400_BAD_REQUEST,
    # 401 Unauthorized
    "ACCESO_NO_AUTORIZADO":     status.HTTP_401_UNAUTHORIZED,
    # 403 Forbidden
    "ACCESO_DENEGADO":          status.HTTP_403_FORBIDDEN,
    # 404 Not Found
    "RECURSO_NO_ENCONTRADO":    status.HTTP_404_NOT_FOUND,
    # 429 Too Many Requests
    "LIMITE_DIARIO_EXCEDIDO":   status.HTTP_429_TOO_MANY_REQUESTS,
    # 502 Bad Gateway
    "ERROR_SERVICIO_EXTERNO":   status.HTTP_502_BAD_GATEWAY,
    # 500 Internal Server Error
    "ERROR_INTERNO":            status.HTTP_500_INTERNAL_SERVER_ERROR,
    "ERROR_INTERNO_SERVIDOR":   status.HTTP_500_INTERNAL_SERVER_ERROR,
}


def _http_status(codigo_error: str) -> int:
    """Resuelve el HTTP Status para un codigo_error dado. Default: 400."""
    return _MAPA_HTTP.get(codigo_error, status.HTTP_400_BAD_REQUEST)


async def luka_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """
    Manejador global de excepciones para FastAPI.
    Registrado en main.py con app.add_exception_handler().
    """
    ruta = str(request.url.path)

    # ── 1. Excepciones de dominio LUKA ───────────────────────────────────────
    if isinstance(exc, LukaException):
        http_status = _http_status(exc.codigo_error)
        nivel = "warning" if http_status < 500 else "error"
        getattr(logger, nivel)(
            "[%s] %s | %s | %s",
            exc.codigo_error,
            http_status,
            ruta,
            exc.mensaje,
        )
        resultado = ResultadoApi.error_res(
            mensaje=exc.mensaje,
            codigo_error=exc.codigo_error,
            detalles=exc.detalles,
            ruta=ruta,
        )
        return JSONResponse(
            status_code=http_status,
            content=resultado.model_dump(by_alias=True),
        )

    # ── 2. Errores de validación Pydantic (request body malformado) ───────────
    if isinstance(exc, ValidationError):
        detalles = [f"{' → '.join(str(loc) for loc in e['loc'])}: {e['msg']}" for e in exc.errors()]
        logger.warning("[DATOS_INVALIDOS] 422 | %s | %d error(es) de validación", ruta, len(detalles))
        resultado = ResultadoApi.error_res(
            mensaje="Los datos de la petición no son válidos. Revisa los campos indicados.",
            codigo_error="DATOS_INVALIDOS",
            detalles=detalles,
            ruta=ruta,
        )
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content=resultado.model_dump(by_alias=True),
        )

    # ── 3. Errores de Google Gemini API ──────────────────────────────────────
    try:
        import google.api_core.exceptions as google_exc
        if isinstance(exc, google_exc.GoogleAPICallError):
            logger.error("[ERROR_SERVICIO_EXTERNO] Gemini API error | %s | %s", ruta, str(exc))
            resultado = ResultadoApi.error_res(
                mensaje="El servicio de inteligencia artificial no está disponible en este momento.",
                codigo_error="ERROR_SERVICIO_EXTERNO",
                detalles=[type(exc).__name__],
                ruta=ruta,
            )
            return JSONResponse(
                status_code=status.HTTP_502_BAD_GATEWAY,
                content=resultado.model_dump(by_alias=True),
            )
    except ImportError:
        pass  # google-api-core no instalado, se salta

    # ── 4. Fallback: error técnico no controlado ─────────────────────────────
    logger.error(
        "[ERROR_INTERNO_SERVIDOR] Error no controlado | %s | %s: %s",
        ruta,
        type(exc).__name__,
        str(exc),
        exc_info=True,  # Stack trace completo solo en logs, nunca en la respuesta
    )
    resultado = ResultadoApi.error_res(
        mensaje="Ha ocurrido un error interno. Nuestro equipo ha sido notificado.",
        codigo_error="ERROR_INTERNO_SERVIDOR",
        # NO exponemos detalles técnicos en producción
        ruta=ruta,
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=resultado.model_dump(by_alias=True),
    )

async def http_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Manejador global para HTTPException (Starlette/FastAPI)"""
    ruta = str(request.url.path)
    
    # Extraer el detalle. Si es un diccionario (ej: validador_jwt.py), extraer valores
    mensaje = str(exc.detail)
    codigo_error = "ERROR_HTTP"
    
    if isinstance(exc.detail, dict):
        mensaje = exc.detail.get("mensaje", mensaje)
        codigo_error = exc.detail.get("codigoError", codigo_error)
        
    resultado = ResultadoApi.error_res(
        mensaje=mensaje,
        codigo_error=codigo_error,
        ruta=ruta,
    )
    return JSONResponse(
        status_code=exc.status_code,
        content=resultado.model_dump(by_alias=True),
    )

