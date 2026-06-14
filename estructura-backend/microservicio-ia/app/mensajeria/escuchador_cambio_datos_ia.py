import json
import logging
from typing import Optional
import pika
from app.configuracion import obtener_configuracion
from app.persistencia.redis.cache_redis import CacheRedis

logger = logging.getLogger(__name__)

class EscuchadorCambioDatosIA:
    """
    Escucha eventos de cambio en datos financieros (ahorro, transacciones)
    e invalida la caché correspondiente (las firmas en Redis) para forzar un nuevo análisis de Gemini.
    """
    def __init__(self):
        self._config = obtener_configuracion()
        self._cache_redis = CacheRedis()
        self._conexion = None
        self._canal = None

    def iniciar(self):
        try:
            self._conectar()
            # Escuchamos eventos de ahorro cambiado
            self._canal.exchange_declare(
                exchange="exchange.cliente.actualizaciones",
                exchange_type="topic",
                durable=True
            )
            self._canal.queue_declare(queue="cola.ia.invalidacion.cache", durable=True)
            self._canal.queue_bind(
                queue="cola.ia.invalidacion.cache",
                exchange="exchange.cliente.actualizaciones",
                routing_key="ahorro.dato.cambiado"
            )
            self._canal.basic_consume(
                queue="cola.ia.invalidacion.cache",
                on_message_callback=self._callback
            )
            logger.info("[SYNC-CACHE] Escuchando cambios en datos para invalidación...")
            self._canal.start_consuming()
        except Exception as e:
            logger.error(f"[SYNC-CACHE] Error: {e}")
        finally:
            self._cerrar_interno()

    def _conectar(self):
        credenciales = pika.PlainCredentials(self._config.rabbitmq_usuario, self._config.rabbitmq_password)
        parametros = pika.ConnectionParameters(
            host=self._config.rabbitmq_host,
            port=self._config.rabbitmq_puerto,
            virtual_host=self._config.rabbitmq_vhost,
            credentials=credenciales
        )
        self._conexion = pika.BlockingConnection(parametros)
        self._canal = self._conexion.channel()

    def _callback(self, canal, metodo, propiedades, cuerpo):
        try:
            datos = json.loads(cuerpo)
            cliente_id = datos.get("clienteId") or propiedades.headers.get("clienteId")
            
            if cliente_id:
                logger.info(f"[SYNC-CACHE] Invalidando caché para cliente: {cliente_id}")
                self._invalidar_cache(cliente_id)
            
            canal.basic_ack(delivery_tag=metodo.delivery_tag)
        except Exception as e:
            logger.error(f"[SYNC-CACHE] Error en callback: {e}")
            canal.basic_ack(delivery_tag=metodo.delivery_tag)

    def _invalidar_cache(self, cliente_id: str):
        """Elimina de Redis las firmas del cliente."""
        try:
            claves_borradas = self._cache_redis.flush_firmas_usuario(cliente_id)
            logger.info(f"[SYNC-CACHE] Firmas de consulta invalidadas para cliente {cliente_id}: {claves_borradas} firmas eliminadas.")
        except Exception as e:
            logger.error(f"[SYNC-CACHE] Error al invalidar: {e}")

    def detener(self):
        if self._canal and self._canal.is_open:
            try:
                self._conexion.add_callback_threadsafe(self._canal.stop_consuming)
                logger.info("[SYNC-CACHE] Solicitando detención del escuchador de cambios...")
            except Exception as e:
                logger.warning(f"[SYNC-CACHE] Error al detener threadsafe: {e}")

    def _cerrar_interno(self):
        try:
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
                logger.info("[SYNC-CACHE] Conexión RabbitMQ cerrada.")
        except Exception as e:
            logger.warning(f"[SYNC-CACHE] Error al cerrar conexión: {e}")
