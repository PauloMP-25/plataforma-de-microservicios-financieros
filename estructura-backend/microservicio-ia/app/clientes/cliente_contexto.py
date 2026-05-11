"""
clientes/cliente_contexto.py  ·  v1.0
══════════════════════════════════════════════════════════════════════════════
Cliente HTTP para reconstrucción de caché (Plan B / Pull Fallback).

Si al procesar una solicitud de chat la clave Redis `ia:contexto:{usuarioId}`
está vacía, este cliente realiza una llamada HTTP GET al endpoint interno
del ms-cliente para obtener el ContextoEstrategicoIADTO y reconstruir la
caché antes de responder.

Endpoint:
    GET {url_cliente}/api/v1/clientes/interno/contexto-financiero/{usuarioId}

@author Paulo Moron
@version 1.1.0
@since 2026-05-10
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
from typing import Optional

import httpx

from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)
config = obtener_configuracion()

REDIS_KEY_PREFIX = "ia:contexto:"
REDIS_TTL_SECONDS = 3600


class ClienteContexto:
    """
    Cliente HTTP síncrono para obtener el contexto financiero del ms-cliente.
    Implementa el patrón "Pull Fallback": si Redis no tiene el contexto
    del usuario, este cliente consulta al ms-cliente para reconstruirlo.
    """

    def __init__(self, redis_client=None):
        """
        Inicializa el cliente con la URL del ms-cliente y un cliente Redis
        opcional para escritura de caché.

        Args:
            redis_client: Instancia de redis.Redis (opcional). Si se provee,
                          el resultado se cachea en Redis automáticamente.
        """
        self.url_base = config.url_cliente
        self.timeout = httpx.Timeout(15.0, connect=5.0)
        self._redis = redis_client

    def obtener_contexto_ia(
        self, usuario_id: str, token: str
    ) -> Optional[dict]:
        """
        Obtiene el contexto estratégico de IA para un usuario.

        Flujo:
            1. Consulta Redis (`ia:contexto:{usuarioId}`).
            2. Si hay cache hit → retorna el JSON parseado.
            3. Si hay cache miss → consulta HTTP al ms-cliente.
            4. Si el HTTP responde → cachea en Redis y retorna.
            5. Si el HTTP falla → retorna None (degradación elegante).

        Args:
            usuario_id: UUID del usuario en formato string.
            token:      JWT del usuario para autenticación inter-servicio.

        Returns:
            Diccionario con el contexto financiero, o None si no disponible.
        """
        # ── 1. Intentar desde Redis (cache hit) ──────────────────────────────
        if self._redis:
            try:
                redis_key = f"{REDIS_KEY_PREFIX}{usuario_id}"
                cached = self._redis.get(redis_key)
                if cached:
                    logger.info(
                        "[CONTEXTO-PULL] Cache HIT para usuario=%s",
                        usuario_id,
                    )
                    return json.loads(cached)
                logger.info(
                    "[CONTEXTO-PULL] Cache MISS para usuario=%s — "
                    "realizando consulta HTTP al ms-cliente.",
                    usuario_id,
                )
            except Exception as exc:
                logger.warning(
                    "[CONTEXTO-PULL] Error leyendo Redis: %s. "
                    "Continuando con consulta HTTP.",
                    exc,
                )

        # ── 2. Consulta HTTP al ms-cliente (Plan B) ──────────────────────────
        url = (
            f"{self.url_base}/api/v1/clientes/interno/"
            f"contexto-financiero/{usuario_id}"
        )
        headers = {"Authorization": f"Bearer {token}"}

        try:
            with httpx.Client(timeout=self.timeout) as cliente:
                respuesta = cliente.get(url, headers=headers)
                respuesta.raise_for_status()
                contexto = respuesta.json()

                logger.info(
                    "[CONTEXTO-PULL] Contexto obtenido vía HTTP para "
                    "usuario=%s — nombres='%s'",
                    usuario_id,
                    contexto.get("nombres", "N/A"),
                )

                # ── 3. Cachear en Redis para futuras consultas ───────────────
                if self._redis:
                    try:
                        redis_key = f"{REDIS_KEY_PREFIX}{usuario_id}"
                        self._redis.setex(
                            name=redis_key,
                            time=REDIS_TTL_SECONDS,
                            value=json.dumps(contexto, ensure_ascii=False),
                        )
                        logger.info(
                            "[CONTEXTO-PULL] Caché reconstruida: %s (TTL=%ds)",
                            redis_key,
                            REDIS_TTL_SECONDS,
                        )
                    except Exception as exc:
                        logger.warning(
                            "[CONTEXTO-PULL] Error escribiendo en Redis: %s. "
                            "El contexto se usará sin cachear.",
                            exc,
                        )

                return contexto

        except httpx.ConnectError:
            logger.error(
                "[CONTEXTO-PULL] No se pudo conectar al ms-cliente en %s. "
                "El chat continuará sin contexto personalizado.",
                self.url_base,
            )
            return None
        except httpx.TimeoutException:
            logger.error(
                "[CONTEXTO-PULL] Timeout al consultar contexto para "
                "usuario=%s.",
                usuario_id,
            )
            return None
        except httpx.HTTPStatusError as exc:
            logger.error(
                "[CONTEXTO-PULL] HTTP %d al obtener contexto: %s",
                exc.response.status_code,
                exc.response.text,
            )
            return None
        except Exception as exc:
            logger.error(
                "[CONTEXTO-PULL] Error inesperado: %s",
                exc,
                exc_info=True,
            )
            return None
