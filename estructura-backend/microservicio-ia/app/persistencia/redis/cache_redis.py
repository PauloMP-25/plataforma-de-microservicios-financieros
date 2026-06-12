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
            password_to_use = config.redis_password or None
            self._client = redis.Redis(
                host=config.redis_host,
                port=config.redis_port,
                db=config.redis_db,
                password=password_to_use,
                ssl=config.redis_ssl,
                decode_responses=True
            )
            self._client.ping()
            logger.info("[CACHE-REDIS] Conexión establecida con éxito.")
        except Exception as e:
            err_msg = str(e)
            if "without any password configured" in err_msg or "no password is set" in err_msg:
                logger.warning(f"[CACHE-REDIS] El servidor Redis no requiere contraseña. Reintentando sin contraseña... ({e})")
                try:
                    self._client = redis.Redis(
                        host=config.redis_host,
                        port=config.redis_port,
                        db=config.redis_db,
                        password=None,
                        ssl=config.redis_ssl,
                        decode_responses=True
                    )
                    self._client.ping()
                    logger.info("[CACHE-REDIS] Conexión establecida con éxito (sin contraseña).")
                    return
                except Exception as retry_e:
                    logger.error(f"[CACHE-REDIS] Fallo en reintento de conexión sin contraseña: {retry_e}")
            else:
                logger.error(f"[CACHE-REDIS] Error al conectar: {e}")
            self._client = None

    def obtener_firma(self, clave: str) -> Optional[str]:
        """Obtiene la firma de una consulta previa para verificar duplicidad."""
        if not self._client:
            return None
        try:
            return self._client.get(clave)
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al obtener firma {clave}: {e}")
            return None

    def registrar_consulta(self, clave_firma: str, descripcion: str, ttl: int = 604800):
        """Registra la firma de una consulta con su descripción y un TTL de 7 días."""
        if not self._client:
            return
        try:
            self._client.setex(clave_firma, ttl, descripcion)
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al registrar consulta {clave_firma}: {e}")

    def obtener_cuota_actual(self, clave: str) -> int:
        """Devuelve el contador de cuota actual."""
        if not self._client:
            return 0
        try:
            valor = self._client.get(clave)
            return int(valor) if valor else 0
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al obtener cuota {clave}: {e}")
            return 0

    def incrementar_cuota(self, clave: str, ttl_segundos: int) -> int:
        """Incrementa de forma atómica la cuota diaria/semanal y asegura el TTL con pipeline."""
        if not self._client:
            return 0
        try:
            pipeline = self._client.pipeline()
            pipeline.incr(clave)
            pipeline.expire(clave, ttl_segundos)
            resultados = pipeline.execute()
            return resultados[0]
        except Exception as e:
            logger.warning(f"[CACHE-REDIS] Error al incrementar cuota {clave}: {e}")
            return 0

    def flush_firmas_usuario(self, usuario_id: str) -> int:
        """Elimina todas las firmas de consulta para un usuario específico (para volver a consultar)."""
        if not self._client:
            return 0
        try:
            patron = f"ia:firma:{usuario_id}:*"
            cursor = 0
            total_eliminadas = 0
            while True:
                cursor, keys = self._client.scan(cursor, match=patron, count=100)
                if keys:
                    self._client.delete(*keys)
                    total_eliminadas += len(keys)
                if cursor == 0:
                    break
            logger.info(f"[CACHE-REDIS] flush_firmas_usuario: {total_eliminadas} firmas eliminadas para usuario {usuario_id}.")
            return total_eliminadas
        except Exception as e:
            logger.error(f"[CACHE-REDIS] Error en flush_firmas_usuario para {usuario_id}: {e}")
            return 0

    def flush_cuotas(self) -> int:
        """Elimina todos los contadores de cuota (ia:cuota:*)."""
        if not self._client:
            return 0
        try:
            patron = "ia:cuota:*"
            cursor = 0
            total_eliminadas = 0
            while True:
                cursor, keys = self._client.scan(cursor, match=patron, count=100)
                if keys:
                    self._client.delete(*keys)
                    total_eliminadas += len(keys)
                if cursor == 0:
                    break
            logger.info(f"[CACHE-REDIS] flush_cuotas: {total_eliminadas} cuotas eliminadas.")
            return total_eliminadas
        except Exception as e:
            logger.error(f"[CACHE-REDIS] Error en flush_cuotas: {e}")
            return 0

    def flush_ia_cache(self, patron: str = "ia:*") -> int:
        """
        Elimina TODAS las claves que coincidan con el patrón dado.
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
