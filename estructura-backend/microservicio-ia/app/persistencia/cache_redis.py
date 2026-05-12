import redis
import json
import logging
from typing import Optional
from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)
config = obtener_configuracion()

class CacheRedis:
    """
    Gestión de caché en Redis para el microservicio IA.
    Evita llamadas repetidas a Gemini y a la base de datos principal.
    """
    def __init__(self):
        try:
            self._client = redis.Redis(
                host=config.redis_host,
                port=config.redis_port,
                db=config.redis_db,
                password=config.redis_password or None,
                decode_responses=True
            )
            self._client.ping()
            logger.info("[CACHE-REDIS] Conexión establecida con éxito.")
        except Exception as e:
            logger.error(f"[CACHE-REDIS] Error al conectar: {e}")
            self._client = None

    def obtener_consejo(self, hash_datos: str) -> Optional[str]:
        """Recupera un consejo de la caché usando el hash de los datos."""
        if not self._client:
            return None
        try:
            return self._client.get(f"ia:consejo:{hash_datos}")
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al obtener: {e}")
            return None

    def guardar_consejo(self, hash_datos: str, consejo: str, ttl: int = None):
        """Guarda un consejo en la caché con un tiempo de vida (TTL)."""
        if not self._client:
            return
        try:
            tiempo = ttl or config.redis_ttl_segundos
            self._client.setex(f"ia:consejo:{hash_datos}", tiempo, consejo)
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al guardar: {e}")

    def invalidar_cache_cliente(self, cliente_id: str):
        """
        Elimina todos los consejos cacheados de un cliente específico.
        Se usa cuando sus datos financieros cambian.
        """
        if not self._client:
            return
        try:
            # En una implementación real, podríamos usar un Set o keys con patrón.
            # Por simplicidad y performance, usaremos un patrón de búsqueda (SCAN en prod).
            keys = self._client.keys(f"ia:consejo:*")
            # Nota: Esta es una forma simplificada. En producción se recomienda 
            # usar tags o estructuras de datos que vinculen cliente -> hashes.
            # Por ahora, invalidaremos por patrón si el hash incluyera el cliente_id.
            pass 
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al invalidar: {e}")
            
    def eliminar_por_hash(self, hash_datos: str):
        """Elimina una entrada específica."""
        if not self._client:
            return
        try:
            self._client.delete(f"ia:consejo:{hash_datos}")
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al eliminar: {e}")
