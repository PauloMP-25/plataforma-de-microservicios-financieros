import logging
from fastapi import Request, status
from fastapi.responses import JSONResponse
from app.libreria_comun.respuesta.resultado_api import ResultadoApi
from app.libreria_comun.excepciones.base import LukaException

logger = logging.getLogger("libreria_comun.excepciones")

async def luka_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """
    Manejador global de excepciones para FastAPI.
    Transforma excepciones de negocio en un esquema ResultadoApi (camelCase)
    idéntico al de Spring Boot.
    """
    
    if isinstance(exc, LukaException):
        logger.warning(f"[NEGOCIO] {exc.codigo_error} | {request.url.path} | {exc.mensaje}")
        
        # Mapeo lógico de códigos de error a Status HTTP
        status_code = status.HTTP_400_BAD_REQUEST
        if exc.codigo_error == "RECURSO_NO_ENCONTRADO":
            status_code = status.HTTP_404_NOT_FOUND
        elif exc.codigo_error == "ACCESO_DENEGADO":
            status_code = status.HTTP_403_FORBIDDEN
        elif exc.codigo_error == "ERROR_INTERNO":
            status_code = status.HTTP_500_INTERNAL_SERVER_ERROR
        elif "SERVICIO_EXTERNO" in exc.codigo_error:
            status_code = status.HTTP_502_BAD_GATEWAY
            
        resultado = ResultadoApi.error_res(
            mensaje=exc.mensaje,
            codigo_error=exc.codigo_error,
            detalles=exc.detalles
        )
        return JSONResponse(
            status_code=status_code,
            content=resultado.model_dump(by_alias=True)
        )

    # Fallback para errores técnicos no controlados
    logger.error(f"[CRITICO] Error no controlado en {request.url.path}: {str(exc)}", exc_info=True)
    
    resultado = ResultadoApi.error_res(
        mensaje="Ha ocurrido un error interno en el servidor.",
        codigo_error="ERROR_INTERNO",
        detalles=[str(exc)]
    )
    
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=resultado.model_dump(by_alias=True)
    )
