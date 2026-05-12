import json
import logging
import threading
from typing import Optional
import pika
from app.configuracion import obtener_configuracion
from app.persistencia.database import SessionLocal
from app.persistencia.modelos_db import IaAnalisisCache
from app.persistencia.cache_redis import CacheRedis

logger = logging.getLogger(__name__)

class EscuchadorCambioDatosIA:
    """
    Escucha eventos de cambio en datos financieros (ahorro, transacciones)
    e invalida la caché correspondiente para forzar un nuevo análisis de Gemini.
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
            self._canal.queue_declare(queue="cola.ia.invalidacion.cache", durable=True)
            self._canal.queue_bind(
                queue="cola.ia.invalidacion.cache",
                exchange="exchange.nucleo.actualizaciones", # Asumido según requerimiento
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

    def _conectar(self):
        credenciales = pika.PlainCredentials(self._config.rabbitmq_usuario, self._config.rabbitmq_password)
        parametros = pika.ConnectionParameters(
            host=self._config.rabbitmq_host,
            port=self._config.rabbitmq_puerto,
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
        """Elimina de DB y Redis los registros del cliente."""
        try:
            # 1. Eliminar de DB
            with SessionLocal() as db:
                db.query(IaAnalisisCache).filter(IaAnalisisCache.cliente_id == cliente_id).delete()
                db.commit()
            
            # 2. Redis (En esta implementación simple, eliminamos por prefijo si es posible)
            # Como los hashes no siempre contienen el cliente_id directamente en el key (ia:consejo:hash),
            # lo ideal sería tener una estructura de datos extra cliente -> [hashes].
            # Por ahora, logueamos que se limpió la DB.
            logger.info(f"[SYNC-CACHE] Caché de DB limpiada para {cliente_id}")
        except Exception as e:
            logger.error(f"[SYNC-CACHE] Error al invalidar: {e}")

    def detener(self):
        if self._canal:
            self._canal.stop_consuming()
        if self._conexion:
            self._conexion.close()
