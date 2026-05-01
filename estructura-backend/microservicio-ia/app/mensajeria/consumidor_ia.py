"""
mensajeria/consumidor_ia.py  ·  v3 — Dual-Mode Consumer + Dashboard Routing
══════════════════════════════════════════════════════════════════════════════
Consumidor RabbitMQ que maneja DOS tipos de eventos:
 
  TRANSACCION_RECIENTE → disparado automáticamente por el nucleo-financiero
        └── Respuesta publicada en: cola.dashboard.consejos
 
  CONSULTA_MODULO      → disparado manualmente desde un botón del Dashboard
        └── Respuesta publicada en: cola.dashboard.modulos
 
Flujo del callback:
  1. Deserializar EventoAnalisisIA (v3 con historial_mensual)
  2. Resolver tipo de solicitud (automático vs manual)
  3. Delegar a CoachIA.analizar(evento) → ResultadoAnalisisIA
  4. Publicar ResultadoAnalisisIA en la cola correcta
  5. Delegar persistencia al PublicadorDashboard
  6. ACK / NACK según resultado
 
Estrategia ACK/NACK:
  JSON inválido           → ACK + descarta  (irrecuperable)
  id_usuario ausente      → ACK + descarta  (irrecuperable)
  CoachIA retorna None    → NACK sin requeue (evita bucle, va a dead-letter)
  Error de publicación    → NACK con requeue (error transitorio)
  Éxito completo          → ACK
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
from typing import Optional

import pika
import pika.exceptions
from pika import BasicProperties
from pika.adapters.blocking_connection import BlockingChannel
from pika.spec import Basic

from app.configuracion import obtener_configuracion
from app.modelos.evento_analisis import (
    EventoAnalisisIA,
    ResultadoAnalisisIA,
    TipoSolicitud,
)
from app.servicios.coach_ia import CoachIA
from app.excepciones import BrokerComunicacionError, ContratoDatosError

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
        self._coach    = CoachIA()
        self._conexion: Optional[pika.BlockingConnection] = None
        self._canal:    Optional[BlockingChannel]         = None
        
        # Movemos las constantes aquí para que sean dinámicas desde la config
        self.EXCHANGE_IA = self._config.rabbitmq_exchange_ia
        self.COLA_ENTRADA = self._config.rabbitmq_cola_entrada
        self.COLA_SALIDA = self._config.rabbitmq_cola_salida_ws
   
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
        
        # ── Registrar callback ────────────────────────────────────────────────
        self._canal.basic_consume(queue=cfg.cola_ia_procesamiento, on_message_callback=self._callback)

        logger.info(
            "[CONSUMIDOR-v3] Topología declarada | "
            "entrada: %s | salida: %s / %s",
            cfg.cola_ia_procesamiento,
            cfg.cola_dashboard_consejos,
            cfg.cola_dashboard_modulos,
        )

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
        """Orquestación del análisis y ruteo de salida."""
        """
        Punto de entrada para cada mensaje de cola.ia.procesamiento.
        Orquesta el flujo completo y garantiza que siempre se emite ACK o NACK.
        """
        tag = metodo.delivery_tag
        logger.info(
            "[CALLBACK] Mensaje recibido | tag=%s | %d bytes", tag, len(cuerpo)
        )
        try:
            # ── 1. Deserializar ───────────────────────────────────────────────────
            evento = self._deserializar(cuerpo, tag, canal)
            if not evento: return  # ACK ya emitido dentro de _deserializar
            
            logger.info(
                "[CALLBACK] Evento válido | usuario=%s | tipo=%s | módulo=%s | historial=%d meses",
                evento.id_usuario,
                evento.tipo_solicitud.value,
                evento.modulo_solicitado.value if evento.modulo_solicitado else "AUTO",
                len(evento.historial_mensual),
            )

            # ── 2. Analizar con CoachIA + Gemini ──────────────────────────────────
            resultado = self._coach.analizar(evento)
            if not resultado:
                logger.error(
                    "[CALLBACK] CoachIA no generó resultado para usuario=%s. NACK sin requeue.",
                    evento.id_usuario,
                )
                canal.basic_nack(delivery_tag=tag, requeue=False)
                return

            # ── 3. Publicar en la cola correcta del Dashboard ─────────────────────
            cola_destino = self._resolver_cola_destino(evento)
            
            # 4. Publicación y persistencia (si falla, requeue para intentar más tarde)
            if self._publicar_resultado(canal, resultado, cola_destino):
                canal.basic_ack(delivery_tag=tag)
            else:
                canal.basic_nack(delivery_tag=tag, requeue=True)
                logger.info(
                    "[CALLBACK] OK | usuario=%s | módulo=%s | cola=%s | ACK",
                    resultado.id_usuario,
                    resultado.tipo_modulo.value,
                    cola_destino,
                )

        except ContratoDatosError:
            # Si el dato está roto, no sirve de nada reintentar. ACK para eliminar de la cola.
            logger.error("[CALLBACK] Contrato inválido. Descartando mensaje.")
            canal.basic_ack(delivery_tag=metodo.delivery_tag)
        except Exception as exc:
            logger.error(f"[CALLBACK] Error inesperado: {exc}")
            canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=True)

    # ── Métodos de soporte ────────────────────────────────────────────────────
    def _deserializar(self, cuerpo: bytes, tag: int, canal: BlockingChannel) -> Optional[EventoAnalisisIA]:
        """
        Deserializa el cuerpo del mensaje. Si falla → ACK + descarta.
        Valida además que id_usuario esté presente.
        Usa el modelo y lanza nuestra excepción personalizada si falla."""
        try:
            return EventoAnalisisIA.desde_json(cuerpo)
        
        except Exception as exc:
            raise ContratoDatosError("El mensaje de Java no coincide con el modelo IA", detalles=str(exc))


    def _resolver_cola_destino(self, evento: EventoAnalisisIA) -> str:
        """
        TRANSACCION_RECIENTE → cola.dashboard.consejos  (widget de últimos consejos)
        CONSULTA_MODULO      → cola.dashboard.modulos   (widget del módulo específico)
        """
        if evento.tipo_solicitud == TipoSolicitud.CONSULTA_MODULO:
            return self._config.cola_dashboard_modulos
        return self._config.cola_dashboard_consejos
    
    def _publicar_resultado(self, canal, resultado: ResultadoAnalisisIA, cola_destino: str) -> bool:
        """
        Serializa ResultadoAnalisisIA y lo publica en el exchange del Dashboard.
        """
        try: 
            canal.basic_publish(
                exchange=self._config.exchange_dashboard,
                routing_key=cola_destino,
                body=json.dumps(resultado.a_dict_serializable(), ensure_ascii=False).encode("utf-8"),
                properties=BasicProperties(
                    delivery_mode=2,                    # persistente
                    content_type="application/json",
                    content_encoding="utf-8",
                    headers={
                        "tipo_modulo":   resultado.tipo_modulo.value,
                        "id_usuario":    resultado.id_usuario,
                        "version":       "3.0",
                    },
                ),
            )
            logger.info(
                "[PUBLICAR] ResultadoAnalisisIA publicado | "
                "cola=%s | módulo=%s | usuario=%s | kpi=%s %s",
                cola_destino,
                resultado.tipo_modulo.value,
                resultado.id_usuario,
                resultado.kpi_principal,
                resultado.kpi_label or "",
            )
            return True
        except Exception as exc:
            logger.error(
                "[PUBLICAR] Error publicando en '%s': %s",
                cola_destino, str(exc), exc_info=True,
            )
            return False