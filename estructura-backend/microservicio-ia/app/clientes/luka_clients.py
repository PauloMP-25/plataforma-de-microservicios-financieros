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
        anio: Optional[int] = None,
        dia_inicio: Optional[int] = None,
        mes_inicio: Optional[int] = None,
        anio_inicio: Optional[int] = None,
        dia_fin: Optional[int] = None,
        mes_fin: Optional[int] = None,
        anio_fin: Optional[int] = None
    ) -> Dict[str, Any]:
        """Recupera el historial de transacciones (concurrente)."""
        endpoint = f"/api/v1/financiero/transacciones/historial"
        
        desde_str = None
        hasta_str = None
        
        if anio_inicio and mes_inicio:
            dia_i = dia_inicio or 1
            desde_dt = datetime(anio_inicio, mes_inicio, dia_i, 0, 0, 0)
            desde_str = desde_dt.isoformat()
            
            if anio_fin and mes_fin:
                import calendar
                dia_f = dia_fin or calendar.monthrange(anio_fin, mes_fin)[1]
                ultimo_dia_mes = calendar.monthrange(anio_fin, mes_fin)[1]
                dia_f_resuelto = min(dia_f, ultimo_dia_mes)
                hasta_dt = datetime(anio_fin, mes_fin, dia_f_resuelto, 23, 59, 59)
                hasta_str = hasta_dt.isoformat()
            else:
                hasta_str = datetime.now().isoformat()
        elif mes and anio:
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
        endpoint = f"/api/v1/clientes/interno/contexto-financiero/{usuario_id}"
        respuesta = await self.call("GET", endpoint, token=token)
        return respuesta.get("datos", {})

# Funciones de utilidad para inyección de dependencias
def obtener_cliente_financiero() -> ClienteFinanciero:
    return ClienteFinanciero()

def obtener_cliente_perfil() -> ClientePerfil:
    return ClientePerfil()
