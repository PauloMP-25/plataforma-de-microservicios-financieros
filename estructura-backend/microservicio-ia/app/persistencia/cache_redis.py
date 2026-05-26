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
                ssl=config.redis_ssl,
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

    def invalidar_cache_cliente(self, cliente_id: str) -> int:
        """
        Elimina todos los consejos cacheados con patrón ia:consejo:*.
        Retorna el número de claves eliminadas.
        """
        return self.flush_ia_cache()

    def eliminar_por_hash(self, hash_datos: str):
        """Elimina una entrada específica."""
        if not self._client:
            return
        try:
            self._client.delete(f"ia:consejo:{hash_datos}")
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al eliminar: {e}")

    def flush_ia_cache(self, patron: str = "ia:*") -> int:
        """
        Elimina TODAS las claves que coincidan con el patrón dado.
        Por defecto elimina toda la cache de la IA (ia:consejo:*, ia:cuota:*, etc.).
        Usa SCAN para no bloquear Redis en producción.
        Retorna el número de claves eliminadas.
        """
        if not self._client:
            logger.warning("[CACHE-REDIS] flush_ia_cache: cliente Redis no disponible.")
            return 0
        try:
            cursor = 0
            total_eliminadas = 0
            while True:
                cursor, keys = self._client.scan(cursor, match=patron, count=100)
                if keys:
                    self._client.delete(*keys)
                    total_eliminadas += len(keys)
                if cursor == 0:
                    break
            logger.info("[CACHE-REDIS] flush_ia_cache: %d clave(s) eliminadas (patron='%s').", total_eliminadas, patron)
            return total_eliminadas
        except Exception as e:
            logger.error(f"[CACHE-REDIS] Error en flush_ia_cache: {e}")
            return 0

    # ── Métodos Genéricos ──────────────────────────────────────────────────────

    def obtener(self, clave: str) -> Optional[str]:
        """Obtiene un valor plano por su clave."""
        if not self._client:
            return None
        try:
            return self._client.get(clave)
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al obtener clave {clave}: {e}")
            return None

    def guardar(self, clave: str, valor: any, ex: int = None):
        """Guarda un valor plano con tiempo de expiración opcional."""
        if not self._client:
            return
        try:
            self._client.set(clave, valor, ex=ex)
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al guardar clave {clave}: {e}")
