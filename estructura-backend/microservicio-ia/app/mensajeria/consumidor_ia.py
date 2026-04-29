"""
mensajeria/consumidor_ia.py
══════════════════════════════════════════════════════════════════════════════
Consumidor RabbitMQ para la cola `cola.ia.procesamiento`.

Flujo:
  1.  Recibe mensaje JSON del exchange `exchange.ia`.
  2.  Deserializa el cuerpo en EventoAnalisisIA (nuevo contrato).
  3.  Delega al CoachIA para generar el consejo con Gemini.
  4.  Publica el consejo en `cola.mensajeria.whatsapp`.
  5.  ACK o NACK según el resultado (sin pérdida de mensajes).

Principios de diseño:
  - La lógica del callback es mínima: deserializar → procesar → publicar.
  - Toda la lógica de negocio vive en CoachIA e IngenierioPrompt.
  - Fallos de Gemini hacen NACK sin requeue para evitar bucles infinitos.
  - Fallos de deserialización hacen ACK + log (el mensaje es irrecuperable).
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

import json
import logging
from typing import Optional

import pika
import pika.exceptions
from pika import BasicProperties
from pika.adapters.blocking_connection import BlockingChannel
from pika.spec import Basic

from app.configuracion import obtener_configuracion
from app.modelos.evento_analisis import EventoAnalisisIA
from app.servicios.coach_ia import CoachIA

logger = logging.getLogger(__name__)

# ── Constantes de mensajería ──────────────────────────────────────────────────
EXCHANGE_IA              = self._config.rabbitmq_exchange_ia  # "exchange.ia"
COLA_ENTRADA             = self._config.rabbitmq_cola_entrada  # "cola.ia.procesamiento"
COLA_WHATSAPP            = self._config.rabbitmq_cola_salida_ws  # "cola.mensajeria.whatsapp"
EXCHANGE_DEFAULT         = ""          # Exchange por defecto de RabbitMQ
ROUTING_KEY_WHATSAPP     = COLA_WHATSAPP


class ConsumidorIA:
    """
    Consumidor bloqueante (pika BlockingConnection) para `cola.ia.procesamiento`.

    Uso típico en main.py o un hilo separado:
        consumidor = ConsumidorIA()
        consumidor.iniciar()   # bloquea el hilo
    """

    def __init__(self) -> None:
        self._config   = obtener_configuracion()
        self._coach    = CoachIA()
        self._conexion: Optional[pika.BlockingConnection] = None
        self._canal:    Optional[BlockingChannel]         = None

    # ── Ciclo de vida ─────────────────────────────────────────────────────────

    def iniciar(self) -> None:
        """Conecta al broker y comienza a consumir mensajes (bloqueante)."""
        self._conectar()
        self._configurar_colas()

        logger.info(
            "[CONSUMIDOR] Esperando mensajes en '%s'. Ctrl+C para detener.",
            COLA_ENTRADA,
        )
        try:
            self._canal.start_consuming()
        except KeyboardInterrupt:
            logger.info("[CONSUMIDOR] Interrupción recibida. Cerrando...")
            self._cerrar()

    def detener(self) -> None:
        """Detiene el consumidor de forma limpia."""
        if self._canal and self._canal.is_open:
            self._canal.stop_consuming()
        self._cerrar()

    # ── Conexión y configuración ──────────────────────────────────────────────

    def _conectar(self) -> None:
        """Establece la conexión y el canal con el broker RabbitMQ."""
        credenciales = pika.PlainCredentials(
            self._config.rabbitmq_usuario,
            self._config.rabbitmq_password,
        )
        parametros = pika.ConnectionParameters(
            host=self._config.rabbitmq_host,
            port=self._config.rabbitmq_puerto,
            virtual_host=self._config.rabbitmq_vhost,
            credentials=credenciales,
            heartbeat=60,
            blocked_connection_timeout=300,
        )
        self._conexion = pika.BlockingConnection(parametros)
        self._canal    = self._conexion.channel()

        # QoS: procesa de a 1 mensaje para no saturar si Gemini es lento
        self._canal.basic_qos(prefetch_count=1)
        logger.info(
            "[CONSUMIDOR] Conectado a RabbitMQ en %s:%d",
            self._config.rabbitmq_host,
            self._config.rabbitmq_puerto,
        )

    def _configurar_colas(self) -> None:
        """Declara el exchange, las colas y el binding de forma idempotente."""
        # Exchange de entrada (durable: sobrevive reinicios del broker)
        self._canal.exchange_declare(
            exchange=EXCHANGE_IA,
            exchange_type="direct",
            durable=True,
        )

        # Cola de entrada: donde llegan los eventos de transacciones
        self._canal.queue_declare(queue=COLA_ENTRADA, durable=True)
        self._canal.queue_bind(
            queue=COLA_ENTRADA,
            exchange=EXCHANGE_IA,
            routing_key=COLA_ENTRADA,
        )

        # Cola de salida: donde publicamos los consejos para WhatsApp
        self._canal.queue_declare(queue=COLA_WHATSAPP, durable=True)

        # Registrar el callback
        self._canal.basic_consume(
            queue=COLA_ENTRADA,
            on_message_callback=self._callback,
            auto_ack=False,   # ACK manual para garantizar entrega
        )
        logger.info(
            "[CONSUMIDOR] Colas configuradas: entrada='%s', salida='%s'",
            COLA_ENTRADA,
            COLA_WHATSAPP,
        )

    def _cerrar(self) -> None:
        """Cierra el canal y la conexión de forma segura."""
        try:
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
                logger.info("[CONSUMIDOR] Conexión RabbitMQ cerrada.")
        except Exception as exc:
            logger.warning("[CONSUMIDOR] Error al cerrar conexión: %s", exc)

    # ── Callback principal ────────────────────────────────────────────────────

    def _callback(
        self,
        canal:      BlockingChannel,
        metodo:     Basic.Deliver,
        propiedades: BasicProperties,
        cuerpo:     bytes,
    ) -> None:
        """
        Se ejecuta por cada mensaje recibido en `cola.ia.procesamiento`.

        Estrategia de ACK/NACK:
          - Deserialización inválida  → ACK  (el mensaje es irrecuperable)
          - Gemini falla              → NACK sin requeue (evita bucle)
          - Publicación falla         → NACK con requeue (reintento posible)
          - Éxito completo            → ACK
        """
        logger.info(
            "[CALLBACK] Mensaje recibido (delivery_tag=%s, %d bytes)",
            metodo.delivery_tag,
            len(cuerpo),
        )

        # ── Paso 1: Deserializar ──────────────────────────────────────────────
        evento = self._deserializar(cuerpo, metodo.delivery_tag, canal)
        if evento is None:
            return  # ACK ya emitido dentro de _deserializar

        # ── Paso 2: Generar consejo con Gemini ────────────────────────────────
        consejo = self._coach.generar_consejo(evento)
        if consejo is None:
            logger.error(
                "[CALLBACK] Gemini no generó consejo para '%s'. NACK sin requeue.",
                evento.transaccion.descripcion,
            )
            canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=False)
            return

        # ── Paso 3: Publicar en cola.mensajeria.whatsapp ──────────────────────
        publicado = self._publicar_consejo(canal, evento, consejo)
        if not publicado:
            canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=True)
            return

        # ── Paso 4: ACK exitoso ───────────────────────────────────────────────
        canal.basic_ack(delivery_tag=metodo.delivery_tag)
        logger.info(
            "[CALLBACK] Procesamiento completo para '%s'. ACK enviado.",
            evento.transaccion.descripcion,
        )

    # ── Métodos de soporte ────────────────────────────────────────────────────

    def _deserializar(
        self,
        cuerpo: bytes,
        delivery_tag: int,
        canal: BlockingChannel,
    ) -> Optional[EventoAnalisisIA]:
        """
        Intenta deserializar el cuerpo del mensaje en EventoAnalisisIA.
        Si falla, emite ACK (mensaje descartado) y retorna None.
        """
        try:
            evento = EventoAnalisisIA.desde_json(cuerpo)
            logger.debug(
                "[DESERIALIZAR] Evento válido: transacción='%s', monto=%.2f, "
                "tiene_perfil=%s, tiene_metas=%s, tiene_limite=%s",
                evento.transaccion.descripcion,
                evento.transaccion.monto,
                evento.contexto.tiene_perfil,
                evento.contexto.tiene_metas,
                evento.contexto.tiene_limite_activo,
            )
            return evento

        except (ValueError, KeyError, json.JSONDecodeError) as exc:
            logger.error(
                "[DESERIALIZAR] Mensaje inválido (delivery_tag=%s). "
                "Descartando con ACK. Error: %s. Cuerpo: %s",
                delivery_tag,
                str(exc),
                cuerpo[:200],  # truncamos para no saturar logs
            )
            canal.basic_ack(delivery_tag=delivery_tag)
            return None

    def _publicar_consejo(
        self,
        canal:   BlockingChannel,
        evento:  EventoAnalisisIA,
        consejo: str,
    ) -> bool:
        """
        Publica el mensaje de WhatsApp en `cola.mensajeria.whatsapp`.

        El payload incluye el consejo + metadatos del evento para que
        el microservicio de mensajería sepa a quién enviarle el mensaje.

        Returns:
            True si la publicación fue exitosa, False en caso contrario.
        """
        try:
            payload = {
                "consejo":     consejo,
                "descripcion": evento.transaccion.descripcion,
                "monto":       evento.transaccion.monto,
                "categoria":   evento.transaccion.categoria,
                "tipo":        evento.transaccion.tipo.value,
            }

            # Si hay perfil, añadimos usuario_id para el servicio de WhatsApp
            if evento.contexto.tiene_perfil and evento.contexto.perfil_financiero:
                payload["tono_ia"] = evento.contexto.perfil_financiero.tono_ia.value

            canal.basic_publish(
                exchange=EXCHANGE_DEFAULT,
                routing_key=ROUTING_KEY_WHATSAPP,
                body=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
                properties=BasicProperties(
                    delivery_mode=2,          # Persistente
                    content_type="application/json",
                    content_encoding="utf-8",
                ),
            )
            logger.info(
                "[PUBLICAR] Consejo publicado en '%s' para '%s'.",
                COLA_WHATSAPP,
                evento.transaccion.descripcion,
            )
            return True

        except Exception as exc:
            logger.error(
                "[PUBLICAR] Error al publicar en '%s': %s",
                COLA_WHATSAPP,
                str(exc),
                exc_info=True,
            )
            return False