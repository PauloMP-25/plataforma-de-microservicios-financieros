import logging
from typing import List, Dict, Any, Optional
from datetime import datetime
from app.clientes.base_client import BaseLukaClient
from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)
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
        tamanio: int = 10000,
        mes: Optional[int] = None,
        anio: Optional[int] = None,
        dia_inicio: Optional[int] = None,
        mes_inicio: Optional[int] = None,
        anio_inicio: Optional[int] = None,
        dia_fin: Optional[int] = None,
        mes_fin: Optional[int] = None,
        anio_fin: Optional[int] = None,
        desde_exacto: Optional[str] = None,
        hasta_exacto: Optional[str] = None
    ) -> Dict[str, Any]:
        """Recupera el historial de transacciones (concurrente)."""
        endpoint = f"/api/v1/financiero/transacciones/historial"
        
        desde_str = None
        hasta_str = None
        
        if desde_exacto and hasta_exacto:
            desde_str = desde_exacto
            hasta_str = hasta_exacto
        elif anio_inicio and mes_inicio:
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
            "tamanio": tamanio,
            "pagina": 0,
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
        # Intento de obtener de Redis para optimizar y evitar consultas repetidas al ms-cliente
        from app.persistencia.redis.cache_redis import CacheRedis
        import json
        
        cache = CacheRedis()
        clave_cache = f"ia:perfil:{usuario_id}"
        cached_data = cache.obtener(clave_cache)
        if cached_data:
            try:
                logger.info(f"[CACHE-HIT] Perfil del usuario {usuario_id} obtenido de Redis.")
                return json.loads(cached_data)
            except Exception as e:
                logger.warning(f"[CACHE-ERROR] Error decodificando perfil de Redis para {usuario_id}: {e}")

        # Si no está en caché, llamar a ms-cliente
        endpoint = f"/api/v1/clientes/interno/contexto-financiero/{usuario_id}"
        respuesta = await self.call("GET", endpoint, token=token)
        datos = respuesta.get("datos", {})
        
        # Guardar en Redis con TTL de 1 hora (3600 segundos)
        if datos:
            try:
                cache.guardar(clave_cache, json.dumps(datos), ex=3600)
                logger.info(f"[CACHE-SET] Perfil del usuario {usuario_id} guardado en Redis.")
            except Exception as e:
                logger.warning(f"[CACHE-ERROR] Error guardando perfil en Redis para {usuario_id}: {e}")
                
        return datos

    async def obtener_presupuesto_activo_async(
        self,
        usuario_id: str,
        token: str
    ) -> Optional[Dict[str, Any]]:
        """
        Obtiene el límite de gasto (presupuesto) activo del usuario desde ms-cliente.
        Usa Redis como caché con TTL de 30 minutos para no sobrecargar ms-cliente.

        Returns:
            Dict con 'montoLimite' y 'nombre', o None si no hay presupuesto activo.
        """
        from app.persistencia.redis.cache_redis import CacheRedis
        import json

        cache = CacheRedis()
        clave_cache = f"ia:presupuesto_activo:{usuario_id}"
        cached_data = cache.obtener(clave_cache)
        if cached_data:
            try:
                logger.info(f"[CACHE-HIT] Presupuesto activo de {usuario_id} obtenido de Redis.")
                valor = json.loads(cached_data)
                # None se serializa como "null"
                return valor if valor else None
            except Exception as e:
                logger.warning(f"[CACHE-ERROR] Error decodificando presupuesto de Redis para {usuario_id}: {e}")

        endpoint = "/api/v1/clientes/limites/activo"
        try:
            respuesta = await self.call("GET", endpoint, token=token)
            datos = respuesta.get("datos", None)
            # Guardar en Redis (TTL 30 min = 1800 seg)
            try:
                cache.guardar(clave_cache, json.dumps(datos, default=str), ex=1800)
                logger.info(f"[CACHE-SET] Presupuesto activo de {usuario_id} guardado en Redis (30 min).")
            except Exception as e:
                logger.warning(f"[CACHE-ERROR] Error guardando presupuesto en Redis: {e}")
            return datos
        except Exception as e:
            logger.warning(f"[PRESUPUESTO] No se pudo obtener presupuesto activo para {usuario_id}: {e}")
            return None

# Funciones de utilidad para inyección de dependencias
def obtener_cliente_financiero() -> ClienteFinanciero:
    return ClienteFinanciero()

def obtener_cliente_perfil() -> ClientePerfil:
    return ClientePerfil()
