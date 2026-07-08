import json
import logging
from typing import Optional
import os
import ssl as ssl_lib
from datetime import datetime
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
            
            # Escuchamos eventos de transacciones registradas para invalidar el caché YTD
            self._canal.exchange_declare(
                exchange="exchange.financiero",
                exchange_type="topic",
                durable=True
            )
            self._canal.queue_bind(
                queue="cola.ia.invalidacion.cache",
                exchange="exchange.financiero",
                routing_key="financiero.transaccion.registrada"
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
            ssl_options=ssl_options
        )
        self._conexion = pika.BlockingConnection(parametros)
        self._canal = self._conexion.channel()

    def _callback(self, canal, metodo, propiedades, cuerpo):
        try:
            datos = json.loads(cuerpo)
            cliente_id = datos.get("clienteId") or datos.get("usuarioId") or propiedades.headers.get("clienteId") or propiedades.headers.get("usuarioId")
            
            routing_key = metodo.routing_key
            if routing_key == "financiero.transaccion.registrada" and cliente_id:
                logger.info(f"[SYNC-CACHE] Evento transaccion registrada recibido para cliente: {cliente_id}")
                self._procesar_append_transaccion(cliente_id, datos)
            elif cliente_id:
                logger.info(f"[SYNC-CACHE] Invalidando caché para cliente: {cliente_id}")
                self._invalidar_cache(cliente_id)
            
            canal.basic_ack(delivery_tag=metodo.delivery_tag)
        except Exception as e:
            logger.error(f"[SYNC-CACHE] Error en callback: {e}")
            canal.basic_ack(delivery_tag=metodo.delivery_tag)

    def _procesar_append_transaccion(self, cliente_id: str, datos: dict):
        """Añade la nueva transacción al caché YTD en Redis para evitar consultarlo de nuevo."""
        try:
            # 1. Invalidar firmas del coach de Gemini, ya que los datos financieros cambiaron
            self._cache_redis.flush_firmas_usuario(cliente_id)
            
            # 2. Mapear datos recibidos al esquema utilizado por preparador_datos
            nueva_tx = {
                "id": datos.get("transaccionId"),
                "usuarioId": datos.get("usuarioId"),
                "monto": datos.get("monto"),
                "tipo": datos.get("tipo"),
                "fechaTransaccion": datos.get("fechaTransaccion"),
                "categoriaNombre": datos.get("categoriaNombre"),
                "metodoPago": datos.get("metodoPago"),
                "descripcion": datos.get("descripcion"),
                "estado": datos.get("estado")
            }
            
            # Extraer año de la transacción o usar el año actual
            fecha_str = nueva_tx.get("fechaTransaccion")
            anio = fecha_str[:4] if fecha_str and len(fecha_str) >= 4 else str(datetime.now().year)
            
            key_cache = f"ia:raw_tx:{cliente_id}:YTD_{anio}"
            cached_data = self._cache_redis.obtener(key_cache)
            
            if cached_data:
                datos_list = json.loads(cached_data)
                if isinstance(datos_list, list):
                    # Verificar duplicados por ID de transacción
                    if not any(tx.get("id") == nueva_tx["id"] for tx in datos_list if isinstance(tx, dict)):
                        datos_list.append(nueva_tx)
                        self._cache_redis.guardar(key_cache, json.dumps(datos_list), ex=10800)
                        logger.info(f"[SYNC-CACHE] Transacción {nueva_tx['id']} agregada exitosamente al caché YTD ({key_cache}).")
                    else:
                        logger.info(f"[SYNC-CACHE] Transacción {nueva_tx['id']} ya existe en el caché. Ignorado.")
            else:
                logger.info(f"[SYNC-CACHE] No hay caché YTD activo para el cliente {cliente_id}. Se generará en la próxima consulta.")
        except Exception as e:
            logger.error(f"[SYNC-CACHE] Error al procesar append de transacción: {e}")

    def _invalidar_cache(self, cliente_id: str):
        """Elimina de Redis las firmas del cliente y el caché YTD de transacciones."""
        try:
            # 1. Eliminar firmas de consulta de Gemini
            claves_borradas = self._cache_redis.flush_firmas_usuario(cliente_id)
            
            # 2. Eliminar caché YTD de transacciones crudas
            patron_tx = f"ia:raw_tx:{cliente_id}:*"
            claves_tx_borradas = self._cache_redis.flush_ia_cache(patron_tx)
            
            logger.info(
                f"[SYNC-CACHE] Caché invalidado para cliente {cliente_id}: "
                f"{claves_borradas} firmas y {claves_tx_borradas} claves de transacciones eliminadas."
            )
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
