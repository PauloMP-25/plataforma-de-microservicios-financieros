"""
mensajeria/escuchador_sincronizacion_ia.py  ·  v2.0
══════════════════════════════════════════════════════════════════════════════
Consumidor RabbitMQ dedicado a la sincronización en tiempo real del contexto
financiero del cliente. Escucha la cola `cola.ia.sincronizacion.contexto`
y actualiza la caché Redis `ia:contexto:{usuarioId}` inmediatamente.

v2.0 — Mejoras de producción:
  - Modelo Pydantic con mapeo camelCase → snake_case (populate_by_name).
  - Reintentos exponenciales (3x) si Redis no está disponible.
  - NACK sin requeue tras agotar reintentos → DLQ captura el mensaje.

Flujo:
    ms-cliente (COMMIT) → @TransactionalEventListener → RabbitMQ
        → cola.ia.sincronizacion.contexto → EscuchadorSincronizacionIA
        → Redis (ia:contexto:{usuarioId})

@author Paulo Moron
@version 2.0.0
@since 2026-05-10
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
import time
from decimal import Decimal
from typing import Optional

import pika
import pika.exceptions
from pika.adapters.blocking_connection import BlockingChannel
from pydantic import BaseModel, Field, ConfigDict

from app.configuracion import obtener_configuracion

logger = logging.getLogger(__name__)

# ── Constantes alineadas con libreria-comun (Java) ──────────────────────────
EXCHANGE_CLIENTE_ACTUALIZACIONES = "exchange.cliente.actualizaciones"
COLA_IA_SINCRONIZACION_CONTEXTO = "cola.ia.sincronizacion.contexto"
ROUTING_KEY_PERFIL_ACTUALIZADO = "cliente.perfil.actualizado"
REDIS_KEY_PREFIX = "ia:contexto:"
REDIS_TTL_SECONDS = 3600  # 1 hora, alineado con ms-cliente


# ── Modelo Pydantic con mapeo camelCase → snake_case ─────────────────────────

def _camel_to_snake(name: str) -> str:
    """Convierte camelCase a snake_case."""
    import re
    s1 = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1_\2", name)
    return re.sub(r"([a-z\d])([A-Z])", r"\1_\2", s1).lower()


class ContextoEstrategicoIA(BaseModel):
    """
    Modelo Pydantic que refleja el ContextoEstrategicoIADTO de Java.
    Acepta tanto camelCase (del JSON de Spring) como snake_case.
    """
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=_camel_to_snake,
    )

    nombres: str = Field(default="Usuario")
    ocupacion: str = Field(default="No especificado")
    ingreso_mensual: Decimal = Field(
        default=Decimal("0"),
        alias="ingresoMensual",
    )
    tono_ia: str = Field(
        default="Amigable",
        alias="tonoIA",
    )
    porcentaje_meta_principal: Decimal = Field(
        default=Decimal("0"),
        alias="porcentajeMetaPrincipal",
    )
    nombre_meta_principal: str = Field(
        default="Sin metas activas",
        alias="nombreMetaPrincipal",
    )
    porcentaje_alerta_gasto: int = Field(
        default=80,
        alias="porcentajeAlertaGasto",
    )


class EscuchadorSincronizacionIA:
    """
    Consumidor bloqueante que sincroniza el contexto financiero del cliente
    desde RabbitMQ hacia Redis en tiempo real.

    Uso desde main.py en un hilo daemon:
        escuchador = EscuchadorSincronizacionIA(redis_client)
        hilo = threading.Thread(target=escuchador.iniciar, daemon=True)
        hilo.start()
    """

    # ── Parámetros de resiliencia ────────────────────────────────────────────
    MAX_REINTENTOS = 3
    BACKOFF_BASE_SECONDS = 1  # 1s, 2s, 4s (exponencial)

    def __init__(self, redis_client) -> None:
        """
        Inicializa el escuchador con la configuración de RabbitMQ y un
        cliente Redis inyectado.

        Args:
            redis_client: Instancia de redis.Redis o compatible.
        """
        self._config = obtener_configuracion()
        self._redis = redis_client
        self._conexion: Optional[pika.BlockingConnection] = None
        self._canal: Optional[BlockingChannel] = None

    # ── Ciclo de vida ────────────────────────────────────────────────────────

    def iniciar(self) -> None:
        """Conecta al broker y comienza a consumir mensajes (bloqueante)."""
        try:
            self._conectar()
            self._declarar_topologia()
            logger.info(
                "[SYNC-IA] Escuchando sincronización de contexto en: %s",
                COLA_IA_SINCRONIZACION_CONTEXTO,
            )
            self._canal.start_consuming()
        except pika.exceptions.AMQPConnectionError as exc:
            logger.critical("[SYNC-IA] Error de conexión con RabbitMQ: %s", exc)
            raise
        except KeyboardInterrupt:
            self.detener()

    def detener(self) -> None:
        """Detiene el consumidor de forma limpia."""
        try:
            if self._canal and self._canal.is_open:
                self._canal.stop_consuming()
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
            logger.info("[SYNC-IA] Consumidor detenido correctamente.")
        except Exception as exc:
            logger.warning("[SYNC-IA] Error al detener consumidor: %s", exc)

    # ── Conexión ─────────────────────────────────────────────────────────────

    def _conectar(self) -> None:
        """Establece la conexión y el canal con RabbitMQ."""
        credenciales = pika.PlainCredentials(
            self._config.rabbitmq_usuario,
            self._config.rabbitmq_password,
        )
        parametros = pika.ConnectionParameters(
            host=self._config.rabbitmq_host,
            port=self._config.rabbitmq_puerto,
            virtual_host=self._config.rabbitmq_vhost,
            credentials=credenciales,
            client_properties={"connection_name": "ms-ia-sync-contexto"},
            heartbeat=self._config.rabbitmq_heartbeat,
            blocked_connection_timeout=self._config.rabbitmq_timeout,
        )
        self._conexion = pika.BlockingConnection(parametros)
        self._canal = self._conexion.channel()
        self._canal.basic_qos(prefetch_count=1)

    def _declarar_topologia(self) -> None:
        """
        Declara el exchange y la cola de sincronización de forma idempotente.
        La topología debe coincidir con la del ms-cliente (Java).
        """
        self._canal.exchange_declare(
            exchange=EXCHANGE_CLIENTE_ACTUALIZACIONES,
            exchange_type="topic",
            durable=True,
        )
        self._canal.queue_declare(
            queue=COLA_IA_SINCRONIZACION_CONTEXTO, durable=True,
            arguments={
                "x-dead-letter-exchange": "exchange.cliente.actualizaciones.dlq",
                "x-dead-letter-routing-key": "cola.ia.sincronizacion.error",
                "x-message-ttl": 600000,
            }
        )
        self._canal.queue_bind(
            queue=COLA_IA_SINCRONIZACION_CONTEXTO,
            exchange=EXCHANGE_CLIENTE_ACTUALIZACIONES,
            routing_key=ROUTING_KEY_PERFIL_ACTUALIZADO,
        )
        self._canal.basic_consume(
            queue=COLA_IA_SINCRONIZACION_CONTEXTO,
            on_message_callback=self._callback,
        )

    # ── Callback principal ───────────────────────────────────────────────────

    def _callback(self, canal, metodo, propiedades, cuerpo) -> None:
        """
        Procesa el mensaje de sincronización recibido.
        Valida con Pydantic y actualiza Redis con reintentos exponenciales.

        Args:
            canal:       Canal de RabbitMQ.
            metodo:      Metadatos de entrega (delivery_tag, routing_key, etc.).
            propiedades: Propiedades del mensaje AMQP.
            cuerpo:      Payload serializado en JSON (bytes).
        """
        tag = metodo.delivery_tag
        try:
            # 1. Deserializar y validar con Pydantic (camelCase → snake_case)
            datos_raw = json.loads(cuerpo)
            contexto = ContextoEstrategicoIA.model_validate(datos_raw)

            logger.info(
                "[SYNC-IA] Contexto recibido — nombres='%s', ocupacion='%s', "
                "ingreso=S/ %s, tono='%s'",
                contexto.nombres,
                contexto.ocupacion,
                contexto.ingreso_mensual,
                contexto.tono_ia,
            )

            # 2. Extraer usuarioId del header AMQP
            usuario_id = None
            if propiedades.headers:
                usuario_id = propiedades.headers.get("usuarioId")

            if not usuario_id:
                logger.warning(
                    "[SYNC-IA] Mensaje sin header 'usuarioId'. Descartando."
                )
                canal.basic_ack(delivery_tag=tag)
                return

            # 3. Serializar en snake_case para que Python lo consuma cómodamente
            redis_key = f"{REDIS_KEY_PREFIX}{usuario_id}"
            json_value = contexto.model_dump_json()

            # 4. Guardar en Redis con reintentos
            if self._guardar_en_redis_con_reintentos(redis_key, json_value):
                logger.info(
                    "[SYNC-IA] Redis actualizado exitosamente: %s (TTL=%ds)",
                    redis_key, REDIS_TTL_SECONDS,
                )
                canal.basic_ack(delivery_tag=tag)
            else:
                logger.error(
                    "[SYNC-IA] Falló escritura en Redis tras %d reintentos. "
                    "NACK sin requeue → DLQ.",
                    self.MAX_REINTENTOS,
                )
                # NACK sin requeue: el mensaje irá a la DLQ
                canal.basic_nack(delivery_tag=tag, requeue=False)

        except json.JSONDecodeError as exc:
            logger.error("[SYNC-IA] JSON malformado descartado: %s", exc)
            canal.basic_ack(delivery_tag=tag)
        except Exception as exc:
            logger.error(
                "[SYNC-IA] Error inesperado procesando sincronización: %s",
                exc, exc_info=True,
            )
            canal.basic_nack(delivery_tag=tag, requeue=False)

    # ── Resiliencia: Reintentos con backoff exponencial ──────────────────────

    def _guardar_en_redis_con_reintentos(
        self, clave: str, valor: str
    ) -> bool:
        """
        Intenta guardar en Redis con reintentos exponenciales.

        Args:
            clave: Clave Redis (ej: ia:contexto:uuid).
            valor: Valor JSON serializado del contexto (snake_case).

        Returns:
            True si la escritura fue exitosa, False si agotó reintentos.
        """
        for intento in range(1, self.MAX_REINTENTOS + 1):
            try:
                self._redis.setex(
                    name=clave,
                    time=REDIS_TTL_SECONDS,
                    value=valor,
                )
                return True
            except Exception as exc:
                espera = self.BACKOFF_BASE_SECONDS * (2 ** (intento - 1))
                logger.warning(
                    "[SYNC-IA] Intento %d/%d — Redis falló: %s. "
                    "Reintentando en %ds...",
                    intento, self.MAX_REINTENTOS, exc, espera,
                )
                time.sleep(espera)
        return False
