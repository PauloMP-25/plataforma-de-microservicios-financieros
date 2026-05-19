from typing import List, Dict, Any, Optional
from datetime import datetime
from app.clientes.base_client import BaseLukaClient
from app.configuracion import obtener_configuracion

config = obtener_configuracion()

class ClienteFinanciero(BaseLukaClient):
    """Cliente para comunicarse con ms-nucleo-financiero."""
    def __init__(self):
        super().__init__(
            base_url=config.url_nucleo_financiero, 
            service_name="ms-nucleo-financiero"
        )

    async def obtener_historial_transacciones_async(
        self, 
        usuario_id: str, 
        token: str, 
        tamanio: int = 200,
        mes: Optional[int] = None,
        anio: Optional[int] = None
    ) -> Dict[str, Any]:
        """Recupera el historial de transacciones (concurrente)."""
        endpoint = f"/api/v1/transacciones/historial"
        
        desde_str = None
        hasta_str = None
        if mes and anio:
            desde_str = datetime(anio, mes, 1, 0, 0, 0).isoformat()
            if mes == 12:
                hasta_str = datetime(anio + 1, 1, 1, 0, 0, 0).isoformat()
            else:
                hasta_str = datetime(anio, mes + 1, 1, 0, 0, 0).isoformat()

        params = {
            "usuarioId": usuario_id,
            "size": tamanio,
            "page": 0,
        }
        if desde_str: params["desde"] = desde_str
        if hasta_str: params["hasta"] = hasta_str

        respuesta = await self.call("GET", endpoint, token=token, params=params)
        return respuesta


class ClientePerfil(BaseLukaClient):
    """Cliente para comunicarse con ms-cliente."""
    def __init__(self):
        super().__init__(
            base_url=config.url_cliente, 
            service_name="ms-cliente"
        )

    async def obtener_perfil_usuario_async(
        self, 
        usuario_id: str, 
        token: str
    ) -> Dict[str, Any]:
        """Obtiene el perfil y metas del usuario (concurrente)."""
        endpoint = f"/api/v1/perfil/estrategico/{usuario_id}"
        respuesta = await self.call("GET", endpoint, token=token)
        return respuesta.get("datos", {})

# Funciones de utilidad para inyección de dependencias
def obtener_cliente_financiero() -> ClienteFinanciero:
    return ClienteFinanciero()

def obtener_cliente_perfil() -> ClientePerfil:
    return ClientePerfil()
