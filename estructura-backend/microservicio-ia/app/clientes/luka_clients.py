from typing import List, Dict, Any, Optional
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

    async def obtener_transacciones(
        self, 
        usuario_id: str, 
        token: str, 
        tamanio: int = 200
    ) -> List[Dict[str, Any]]:
        """Recupera el historial de transacciones del usuario."""
        endpoint = f"/api/v1/transacciones/usuario/{usuario_id}"
        params = {"size": tamanio}
        
        # El núcleo financiero devuelve un ResultadoApi con la lista en 'datos'
        respuesta = await self.call("GET", endpoint, token=token, params=params)
        return respuesta.get("datos", [])

class ClientePerfil(BaseLukaClient):
    """Cliente para comunicarse con ms-cliente."""
    def __init__(self):
        super().__init__(
            base_url=config.url_cliente, 
            service_name="ms-cliente"
        )

    async def obtener_perfil_estrategico(
        self, 
        usuario_id: str, 
        token: str
    ) -> Dict[str, Any]:
        """Obtiene el perfil y metas del usuario para el coach IA."""
        endpoint = f"/api/v1/perfil/estrategico/{usuario_id}"
        
        respuesta = await self.call("GET", endpoint, token=token)
        return respuesta.get("datos", {})

# Funciones de utilidad para inyección de dependencias
def obtener_cliente_financiero() -> ClienteFinanciero:
    return ClienteFinanciero()

def obtener_cliente_perfil() -> ClientePerfil:
    return ClientePerfil()
