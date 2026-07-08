"""
mensajeria/escuchador_eventos_usuario_ia.py  ·  v1.0
══════════════════════════════════════════════════════════════════════════════
Escuchador RabbitMQ en ms-ia para inicialización del caché del dashboard 
post-login (Pull) y actualización reactiva mediante .append de transacciones.

Flujo de Login:
  ms-usuario → exchange.usuario.eventos (usuario.login.exitoso)
    → cola.ia.login.eventos → EscuchadorEventosUsuarioIA
    → HTTP GET ms-nucleo-financiero (obtener historial completo del año)
    → Redis (HSET usuario:{usuarioId} transacciones <JSON>)

Flujo de Transacciones:
  ms-nucleo-financiero → exchange.financiero (financiero.transaccion.registrada)
    → cola.ia.transacciones.eventos → EscuchadorEventosUsuarioIA
    → Redis (HSET usuario:{usuarioId} transacciones [append nuevo evento])
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
import asyncio
import os
import ssl as ssl_lib
import pika
from typing import Optional
from datetime import datetime

from app.configuracion import obtener_configuracion
from app.clientes.luka_clients import ClienteFinanciero

logger = logging.getLogger(__name__)

EXCHANGE_USUARIO_EVENTOS = "exchange.usuario.eventos"
ROUTING_KEY_LOGIN_EXITOSO = "usuario.login.exitoso"
COLA_IA_LOGIN_EVENTOS = "cola.ia.login.eventos"
ROUTING_KEY_LOGOUT_EXITOSO = "usuario.logout.exitoso"
COLA_IA_LOGOUT_EVENTOS = "cola.ia.logout.eventos"

EXCHANGE_FINANCIERO = "exchange.financiero"
ROUTING_KEY_TRANSACCION_REGISTRADA = "financiero.transaccion.registrada"
COLA_IA_TRANSACCIONES_EVENTOS = "cola.ia.transacciones.eventos"

REDIS_TTL_SECONDS = 3600  # 1 hora


class EscuchadorEventosUsuarioIA:
    """
    Escucha eventos RabbitMQ para precalentar e incrementar de forma reactiva
    el caché modular en Redis bajo la clave Hash de usuario 'usuario:{usuarioId}'.
    """

    def __init__(self, redis_client) -> None:
        self._config = obtener_configuracion()
        self._redis = redis_client
        self._conexion: Optional[pika.BlockingConnection] = None
        self._canal: Optional[pika.adapters.blocking_connection.BlockingChannel] = None

    def iniciar(self) -> None:
        """Conecta con RabbitMQ y comienza a consumir eventos."""
        try:
            self._conectar()
            self._declarar_topologia()
            logger.info(
                "[USER-EVENTS-IA] Iniciado. Escuchando colas de login y transacciones."
            )
            self._canal.start_consuming()
        except pika.exceptions.AMQPConnectionError as exc:
            logger.critical("[USER-EVENTS-IA] Error de conexión con RabbitMQ: %s", exc)
            raise
        except KeyboardInterrupt:
            self.detener()
        finally:
            self._cerrar_conexion()

    def detener(self) -> None:
        """Detiene de forma limpia el consumidor."""
        try:
            if self._canal and self._canal.is_open:
                self._conexion.add_callback_threadsafe(self._canal.stop_consuming)
            logger.info("[USER-EVENTS-IA] Deteniendo consumidor...")
        except Exception as exc:
            logger.warning("[USER-EVENTS-IA] Error al detener: %s", exc)

    def _cerrar_conexion(self) -> None:
        try:
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
                logger.info("[USER-EVENTS-IA] Conexión cerrada.")
        except Exception as exc:
            logger.warning("[USER-EVENTS-IA] Error al cerrar conexión: %s", exc)

    def _conectar(self) -> None:
        credenciales = pika.PlainCredentials(
            self._config.rabbitmq_usuario,
            self._config.rabbitmq_password,
        )
        
        ssl_options = None
        if os.getenv("RABBITMQ_SSL_ENABLED", "true").lower() == "true":
            ssl_context = ssl_lib.create_default_context()
            ssl_context.check_hostname = False
            ssl_context.verify_mode = ssl_lib.CERT_NONE
            ssl_options = pika.SSLOptions(ssl_context)
            
        puerto = int(os.getenv("RABBITMQ_PUERTO", str(self._config.rabbitmq_puerto)))
        
        parametros = pika.ConnectionParameters(
            host=self._config.rabbitmq_host,
            port=puerto,
            virtual_host=self._config.rabbitmq_vhost,
            credentials=credenciales,
            ssl_options=ssl_options,
            client_properties={"connection_name": "ms-ia-user-events-caching"},
            heartbeat=self._config.rabbitmq_heartbeat,
            blocked_connection_timeout=self._config.rabbitmq_timeout,
        )
        self._conexion = pika.BlockingConnection(parametros)
        self._canal = self._conexion.channel()
        self._canal.basic_qos(prefetch_count=1)

    def _declarar_topologia(self) -> None:
        # ── 1. Cola e Exchange de Login ─────────────────────────────────────
        self._canal.exchange_declare(
            exchange=EXCHANGE_USUARIO_EVENTOS,
            exchange_type="topic",
            durable=True,
        )
        self._canal.queue_declare(
            queue=COLA_IA_LOGIN_EVENTOS,
            durable=True,
            arguments={
                "x-dead-letter-exchange": "exchange.usuario.eventos.dlq",
                "x-dead-letter-routing-key": "cola.ia.login.eventos.dlq",
                "x-message-ttl": 300000,
            }
        )
        self._canal.queue_bind(
            queue=COLA_IA_LOGIN_EVENTOS,
            exchange=EXCHANGE_USUARIO_EVENTOS,
            routing_key=ROUTING_KEY_LOGIN_EXITOSO,
        )

        self._canal.queue_declare(
            queue=COLA_IA_LOGOUT_EVENTOS,
            durable=True,
            arguments={
                "x-dead-letter-exchange": "exchange.usuario.eventos.dlq",
                "x-dead-letter-routing-key": "cola.ia.logout.eventos.dlq",
                "x-message-ttl": 300000,
            }
        )
        self._canal.queue_bind(
            queue=COLA_IA_LOGOUT_EVENTOS,
            exchange=EXCHANGE_USUARIO_EVENTOS,
            routing_key=ROUTING_KEY_LOGOUT_EXITOSO,
        )

        # ── 2. Cola e Exchange de Transacciones ─────────────────────────────
        self._canal.exchange_declare(
            exchange=EXCHANGE_FINANCIERO,
            exchange_type="topic",
            durable=True,
        )
        self._canal.queue_declare(
            queue=COLA_IA_TRANSACCIONES_EVENTOS,
            durable=True,
            arguments={
                "x-dead-letter-exchange": "exchange.financiero.dlq",
                "x-dead-letter-routing-key": "cola.ia.transacciones.eventos.dlq",
                "x-message-ttl": 300000,
            }
        )
        self._canal.queue_bind(
            queue=COLA_IA_TRANSACCIONES_EVENTOS,
            exchange=EXCHANGE_FINANCIERO,
            routing_key=ROUTING_KEY_TRANSACCION_REGISTRADA,
        )

        # ── 3. Asignar callbacks ────────────────────────────────────────────
        self._canal.basic_consume(
            queue=COLA_IA_LOGIN_EVENTOS,
            on_message_callback=self._callback_login,
        )
        self._canal.basic_consume(
            queue=COLA_IA_LOGOUT_EVENTOS,
            on_message_callback=self._callback_logout,
        )
        self._canal.basic_consume(
            queue=COLA_IA_TRANSACCIONES_EVENTOS,
            on_message_callback=self._callback_transaccion,
        )

    def _callback_login(self, canal, metodo, propiedades, cuerpo) -> None:
        tag = metodo.delivery_tag
        try:
            evento = json.loads(cuerpo)
            usuario_id = evento.get("usuarioId")
            
            if not usuario_id:
                logger.warning("[USER-EVENTS-IA] Evento de login sin usuarioId. Ignorando.")
                canal.basic_ack(delivery_tag=tag)
                return

            logger.info(
                "[USER-EVENTS-IA] Login exitoso detectado para usuario=%s. Precalentando transacciones anuales en Redis.",
                usuario_id
            )

            # Inicializar transacciones del año en Redis de manera asíncrona
            asyncio.run(self._inicializar_transacciones_anuales(usuario_id))
            canal.basic_ack(delivery_tag=tag)

        except Exception as exc:
            logger.error("[USER-EVENTS-IA] Error en callback de login: %s", exc, exc_info=True)
            canal.basic_nack(delivery_tag=tag, requeue=False)

    def _callback_logout(self, canal, metodo, propiedades, cuerpo) -> None:
        tag = metodo.delivery_tag
        try:
            evento = json.loads(cuerpo)
            usuario_id = evento.get("usuarioId")
            
            if not usuario_id:
                logger.warning("[USER-EVENTS-IA] Evento de logout sin usuarioId. Ignorando.")
                canal.basic_ack(delivery_tag=tag)
                return

            logger.info(
                "[USER-EVENTS-IA] Logout exitoso detectado para usuario=%s. Limpiando caché en Redis.",
                usuario_id
            )

            # Borrar la clave principal
            hash_key = f"usuario:{usuario_id}"
            self._redis.delete(hash_key)
            canal.basic_ack(delivery_tag=tag)

        except Exception as exc:
            logger.error("[USER-EVENTS-IA] Error en callback de logout: %s", exc, exc_info=True)
            canal.basic_nack(delivery_tag=tag, requeue=False)

    def _callback_transaccion(self, canal, metodo, propiedades, cuerpo) -> None:
        tag = metodo.delivery_tag
        try:
            evento = json.loads(cuerpo)
            usuario_id = evento.get("usuarioId")
            
            if not usuario_id:
                logger.warning("[USER-EVENTS-IA] Evento de transacción sin usuarioId. Ignorando.")
                canal.basic_ack(delivery_tag=tag)
                return

            logger.info(
                "[USER-EVENTS-IA] Recibida nueva transacción para usuario=%s. Realizando .append reactivo en Redis.",
                usuario_id
            )

            self._hacer_append_transaccion(usuario_id, evento)
            canal.basic_ack(delivery_tag=tag)

        except Exception as exc:
            logger.error("[USER-EVENTS-IA] Error en callback de transacción: %s", exc, exc_info=True)
            canal.basic_nack(delivery_tag=tag, requeue=False)

    async def _inicializar_transacciones_anuales(self, usuario_id: str) -> None:
        cliente_financiero = None
        try:
            anio_actual = datetime.now().year
            cliente_financiero = ClienteFinanciero()
            
            # Consultar historial completo de transacciones del año al microservicio financiero
            # Usando token=None para que se aplique X-Internal-Token de llamada inter-servicio
            logger.info("[USER-EVENTS-IA] Solicitando transacciones del año %d al ms-financiero...", anio_actual)
            respuesta = await cliente_financiero.obtener_historial_transacciones_async(
                usuario_id=usuario_id,
                token=None,
                tamanio=10000,
                anio_inicio=anio_actual,
                mes_inicio=1,
                dia_inicio=1
            )
            
            transacciones = respuesta.get("datos", {}).get("content", []) if isinstance(respuesta.get("datos"), dict) else respuesta.get("datos", [])
            if not isinstance(transacciones, list):
                transacciones = []

            # Mapear y ordenar de más reciente a más antiguo
            transacciones.sort(key=lambda t: t.get("fechaTransaccion", ""), reverse=True)

            hash_key = f"usuario:{usuario_id}"
            campo = "transacciones"

            # Guardar en Redis en el campo 'transacciones' del Hash de usuario
            self._redis.hset(hash_key, campo, json.dumps(transacciones, ensure_ascii=False))
            self._redis.expire(hash_key, REDIS_TTL_SECONDS)

            logger.info(
                "[USER-EVENTS-IA] Caché inicializada para %s. Total de transacciones del año en caché: %d",
                hash_key, len(transacciones)
            )

        except Exception as exc:
            logger.error("[USER-EVENTS-IA] Error al inicializar transacciones anuales en Redis: %s", exc, exc_info=True)
        finally:
            if cliente_financiero:
                await cliente_financiero.close()

    def _hacer_append_transaccion(self, usuario_id: str, evento: dict) -> None:
        try:
            hash_key = f"usuario:{usuario_id}"
            campo = "transacciones"

            # 1. Obtener la lista actual de transacciones del caché de Redis
            cached = self._redis.hget(hash_key, campo)
            lista_transacciones = []

            if cached:
                try:
                    lista_transacciones = json.loads(cached)
                    if not isinstance(lista_transacciones, list):
                        lista_transacciones = []
                except Exception as json_err:
                    logger.warning("[USER-EVENTS-IA] JSON malformado en caché de transacciones, se creará nueva lista: %s", json_err)
            
            # 2. Agregar la nueva transacción (.append) al inicio (más reciente)
            lista_transacciones.insert(0, evento)

            # 3. Guardar de nuevo en Redis en el campo correspondiente del Hash de usuario
            self._redis.hset(hash_key, campo, json.dumps(lista_transacciones, ensure_ascii=False))
            self._redis.expire(hash_key, REDIS_TTL_SECONDS)

            logger.info(
                "[USER-EVENTS-IA] Transacción %s agregada en Redis para %s. Total transacciones en caché: %d",
                evento.get("transaccionId", "N/A"), hash_key, len(lista_transacciones)
            )

        except Exception as exc:
            logger.error("[USER-EVENTS-IA] Error al realizar .append de transacción en Redis: %s", exc)
