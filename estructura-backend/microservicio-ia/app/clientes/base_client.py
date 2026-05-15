import httpx
import logging
from typing import Optional, Any, Dict
from app.libreria_comun.seguridad.contexto import get_correlation_id
from app.libreria_comun.excepciones.base import ServicioExternoError

logger = logging.getLogger(__name__)

class BaseLukaClient:
    """
    Cliente base asíncrono para la red de microservicios LUKA.
    Inyecta automáticamente X-Correlation-ID y Authorization.
    """
    def __init__(self, base_url: str, service_name: str):
        self.base_url = base_url.rstrip("/")
        self.service_name = service_name
        self._client = httpx.AsyncClient(
            timeout=10.0,
            headers={"Accept": "application/json"}
        )

    async def call(
        self, 
        method: str, 
        endpoint: str, 
        token: Optional[str] = None,
        **kwargs
    ) -> Any:
        """
        Ejecuta la petición HTTP con propagación de contexto.
        """
        url = f"{self.base_url}/{endpoint.lstrip('/')}"
        
        # 1. Preparar headers de trazabilidad y seguridad
        headers = kwargs.get("headers", {})
        headers["X-Correlation-ID"] = get_correlation_id()
        
        if token:
            headers["Authorization"] = f"Bearer {token}"
        
        kwargs["headers"] = headers

        try:
            logger.debug(f"[HTTP] {method} {url} | Trace: {headers['X-Correlation-ID']}")
            response = await self._client.request(method, url, **kwargs)
            
            # 2. Manejo de errores 4xx/5xx compatible con ResultadoApi
            if response.status_code >= 400:
                logger.warning(f"[HTTP-ERROR] {self.service_name} respondió {response.status_code}")
                raise ServicioExternoError(
                    mensaje=f"Error al consultar el servicio {self.service_name}",
                    codigo_error=f"ERROR_SERVICIO_EXTERNO",
                    detalles={
                        "status": response.status_code,
                        "service": self.service_name,
                        "response": response.text[:200]
                    }
                )
                
            return response.json()

        except httpx.ConnectError:
            logger.error(f"[HTTP-FATAL] {self.service_name} no disponible")
            raise ServicioExternoError(
                mensaje=f"El servicio {self.service_name} está temporalmente fuera de línea.",
                codigo_error="SERVICIO_NO_DISPONIBLE"
            )
        except Exception as exc:
            if isinstance(exc, ServicioExternoError): raise exc
            logger.error(f"[HTTP-FATAL] Error en cliente {self.service_name}: {str(exc)}")
            raise ServicioExternoError(
                mensaje="Error técnico en la comunicación inter-servicios.",
                codigo_error="ERROR_INTERNO_CLIENTE"
            )

    async def close(self):
        """Cierra el pool de conexiones."""
        await self._client.aclose()
