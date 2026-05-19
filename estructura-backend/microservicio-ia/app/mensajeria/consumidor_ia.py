"""
mensajeria/consumidor_ia.py  ·  v4 — Refactorizado para IA Centrada en Datos
══════════════════════════════════════════════════════════════════════════════
Consumidor RabbitMQ adaptado a la v4 de LUKA. 
Maneja la orquestación asíncrona disparada por el nucleo-financiero o el Dashboard.

Cambios v4:
  - Usa 'RespuestaModulo' en lugar del antiguo 'ResultadoAnalisisIA'.
  - Soporta el campo 'estado_coach' para informar fallos de Gemini al Dashboard.
  - Integrado con 'ServicioAnalisis' (o el nuevo CoachIA refactorizado).
  - Mapeo de colas dinámico según la configuración centralizada.
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
from typing import Optional

import pika
import pika.exceptions
import asyncio
from pika import BasicProperties
from pika.adapters.blocking_connection import BlockingChannel
from pika.spec import Basic

from app.configuracion import obtener_configuracion
from app.modelos.esquemas import (
    NombreModulo,
    RespuestaModulo,
    PeticionConFiltroFecha,  # DTO genérico para eventos RabbitMQ
)
from app.servicios.core.servicio_analisis import ServicioAnalisis
from app.servicios.ia.coach_ia import CoachIA
from app.utilidades.excepciones import BrokerComunicacionError, ContratoDatosError

logger = logging.getLogger(__name__)

class ConsumidorIA:
    """
    Consumidor bloqueante (pika BlockingConnection).
 
    Uso desde main.py en un hilo daemon:
        consumidor = ConsumidorIA()
        hilo = threading.Thread(target=consumidor.iniciar, daemon=True)
        hilo.start()
    """

    def __init__(self) -> None:
        self._config   = obtener_configuracion()
        self._coach    = CoachIA()          # Para clasificación on-the-fly
        self._servicio = ServicioAnalisis() # Orquestador principal para el _callback
        self._conexion: Optional[pika.BlockingConnection] = None
        self._canal:    Optional[BlockingChannel]         = None
        
        # Accesos directos a la config para los callbacks de publicación
        self.EXCHANGE_IA = self._config.exchange_ia
        self.COLA_ENTRADA = self._config.cola_ia_procesamiento
        self.COLA_SALIDA = self._config.cola_dashboard_consejos
   
    # ── Ciclo de vida ─────────────────────────────────────────────────────────
    def iniciar(self) -> None:
        """Conecta al broker y comienza a consumir mensajes (bloqueante)."""
        try:
            self._conectar()
            self._declarar_topologia()
            logger.info(f"[CONSUMIDOR-v3] Escuchando en {self._config.cola_ia_procesamiento}")
            self._canal.start_consuming()

        except pika.exceptions.AMQPConnectionError as exc:
            logger.critical(f"[CONSUMIDOR] Error físico con RabbitMQ: {exc}")
            raise BrokerComunicacionError("Conexión con el broker perdida.", detalles=str(exc))
        except KeyboardInterrupt:
            self.detener()

    def detener(self) -> None:
        """Detiene el consumidor de forma limpia desde otro hilo."""
        self._activo.clear()
        if self._canal and self._canal.is_open:
            # stop_consuming es thread-safe en pika si se llama via add_callback_threadsafe
            self._conexion.add_callback_threadsafe(self._canal.stop_consuming)
        self._cerrar()

    # ── Conexión y configuración ──────────────────────────────────────────────
    def _conectar(self) -> None:
        """Establece la conexión y el canal con el broker RabbitMQ."""
        try:
            credenciales = pika.PlainCredentials(
                self._config.rabbitmq_usuario,
                self._config.rabbitmq_password,
            )
            parametros = pika.ConnectionParameters(
                host=self._config.rabbitmq_host,
                port=self._config.rabbitmq_puerto,
                virtual_host=self._config.rabbitmq_vhost,
                credentials=credenciales,
                #Nombre de la aplicación para identificación en RabbitMQ (opcional pero útil)
                client_properties={"connection_name": self._config.rabbitmq_connection_name},
                heartbeat=self._config.rabbitmq_heartbeat,
                blocked_connection_timeout=self._config.rabbitmq_timeout,
            )
            self._conexion = pika.BlockingConnection(parametros)
            self._canal = self._conexion.channel()
            self._canal.basic_qos(prefetch_count=1)
        except Exception as exc:
            raise BrokerComunicacionError("Fallo en la autenticación o parámetros de RabbitMQ", detalles=str(exc))

    def _declarar_topologia(self) -> None:
        """
        Declara todos los exchanges y colas de forma idempotente.
        Topología v3:
          exchange.ia        (entrada)
          exchange.dashboard (salida hacia Dashboard)
        """
        cfg = self._config

        # ── Exchange de entrada ───────────────────────────────────────────────
        self._canal.exchange_declare(
            exchange=cfg.exchange_ia, exchange_type="direct", durable=True
        )
        self._canal.queue_declare(queue=cfg.cola_ia_procesamiento, durable=True)
        self._canal.queue_bind(
            queue=cfg.cola_ia_procesamiento,
            exchange=cfg.exchange_ia,
            routing_key=cfg.cola_ia_procesamiento,
        )

        # ── Exchange de salida (Dashboard) ────────────────────────────────────
        self._canal.exchange_declare(
            exchange=cfg.exchange_dashboard, exchange_type="direct", durable=True
        )
        # Cola consejos automáticos
        for cola in [cfg.cola_dashboard_consejos, cfg.cola_dashboard_modulos]:
            self._canal.queue_declare(queue=cola, durable=True)
            self._canal.queue_bind(queue=cola, exchange=cfg.exchange_dashboard, routing_key=cola)
        
        # ── Cola de Clasificación On-the-Fly ──────────────────────────────────
        self._canal.queue_declare(queue=cfg.cola_ia_clasificacion, durable=True)
        self._canal.queue_bind(
            queue=cfg.cola_ia_clasificacion,
            exchange=cfg.exchange_ia,
            routing_key=cfg.cola_ia_clasificacion
        )

        # ── Registrar callbacks ───────────────────────────────────────────────
        self._canal.basic_consume(queue=cfg.cola_ia_procesamiento, on_message_callback=self._callback)
        self._canal.basic_consume(queue=cfg.cola_ia_clasificacion, on_message_callback=self._callback_clasificacion)

        logger.info(
            "[CONSUMIDOR-v4] Topología declarada | "
            "entrada: %s, %s | salida: %s",
            cfg.cola_ia_procesamiento, cfg.cola_ia_clasificacion,
            cfg.exchange_dashboard
        )

    def _callback_clasificacion(self, ch, method, properties, body):
        """Procesa solicitudes de clasificación en tiempo real (Sincronizado con asyncio)."""
        try:
            datos = json.loads(body)
            from app.modelos.esquemas import SolicitudClasificacionDTO, NombreModulo
            solicitud = SolicitudClasificacionDTO(**datos)
            
            # 1. Verificar Cuota (Asumimos el rol PRO para usuarios habilitados en esta cola)
            # Nota: En una implementación real, el mensaje debería traer el rol del usuario.
            rol_usuario = datos.get("rol", "PRO") 
            self._coach._verificar_cuota_diaria(datos.get("usuario_id", "N/A"), NombreModulo.AUTO_CLASIFICACION, rol_usuario)
            
            from app.servicios.ia.clasificador_ia import ClasificadorIAService
            clasificador = ClasificadorIAService()
            
            # 2. Ejecutar la corrutina asíncrona de forma síncrona
            respuesta = asyncio.run(clasificador.clasificar(solicitud))
            
            ch.basic_publish(
                exchange=self.EXCHANGE_IA,
                routing_key="ia.clasificacion.resultado",
                body=json.dumps(respuesta.model_dump())
            )
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"[CLASIFICADOR] Sugerencias enviadas para ID: {solicitud.id_temporal}")
            
        except Exception as e:
            logger.error(f"[CONSUMIDOR] Error en clasificación: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    #-- Cierre de conexión ───────────────────────────────────────────────────
    def _cerrar(self) -> None:
        """Cierra el canal y la conexión de forma segura."""
        try:
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
                logger.info("[CONSUMIDOR] Conexión RabbitMQ cerrada.")
        except Exception as exc:
            logger.warning("[CONSUMIDOR] Error al cerrar conexión: %s", exc)

    # ── Callback principal ────────────────────────────────────────────────────

    def _callback(self, canal, metodo, propiedades, cuerpo) -> None:
        """Procesa el mensaje recibido."""
        tag = metodo.delivery_tag
        try:
            # 1. Deserializar usando los nuevos esquemas v4
            # Nota: El mensaje de Rabbit debe convertirse en un DTO de Petición
            datos = json.loads(cuerpo)
            peticion = PeticionConFiltroFecha(**datos)
            
            logger.info(f"[CALLBACK] Analizando usuario {peticion.usuario_id}...")

            # 2. Determinar módulo desde el mensaje (el productor debe incluir 'modulo')
            nombre_modulo_str = datos.get("modulo", NombreModulo.REPORTE_COMPLETO.value)
            try:
                modulo = NombreModulo(nombre_modulo_str)
            except ValueError:
                logger.warning("[CALLBACK] Módulo desconocido '%s', usando REPORTE_COMPLETO", nombre_modulo_str)
                modulo = NombreModulo.REPORTE_COMPLETO

            # 3. Delegar análisis al orquestador principal (motor analítico + Gemini)
            resultado: RespuestaModulo = asyncio.run(
                self._servicio.procesar_modulo(modulo, peticion, "rabbitmq")
            )

            # 4. Resolver ruteo (módulo específico vs. consejo automático)
            cola_destino = (
                self._config.cola_dashboard_modulos
                if peticion.mes
                else self._config.cola_dashboard_consejos
            )

            # 4. Publicar y confirmar
            if self._publicar_resultado(canal, resultado, cola_destino):
                canal.basic_ack(delivery_tag=tag)
            else:
                canal.basic_nack(delivery_tag=tag, requeue=True)

        except (json.JSONDecodeError, TypeError, ContratoDatosError) as exc:
            logger.error(f"[CALLBACK] Mensaje malformado descartado: {exc}")
            canal.basic_ack(delivery_tag=tag) # Ack para limpiar basura
        except Exception as exc:
            logger.error(f"[CALLBACK] Error inesperado: {exc}", exc_info=True)
            canal.basic_nack(delivery_tag=tag, requeue=True)

    def _publicar_resultado(self, canal, resultado: RespuestaModulo, cola_destino: str) -> bool:
        """Serializa y publica el objeto RespuestaModulo."""
        try:
            body = json.dumps(resultado.a_dict_serializable(), ensure_ascii=False).encode("utf-8")
            canal.basic_publish(
                exchange=self._config.exchange_dashboard,
                routing_key=cola_destino,
                body=body,
                properties=BasicProperties(
                    delivery_mode=2,
                    content_type="application/json",
                    headers={
                        "modulo": resultado.modulo.value,
                        "estado_coach": resultado.estado_coach.value
                    }
                ),
            )
            return True
        except Exception as exc:
            logger.error(f"[PUBLICAR] Fallo: {exc}")
            return False