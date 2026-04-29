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
from app.excepciones import BrokerComunicacionError, ContratoDatosError, GeminiError
logger = logging.getLogger(__name__)

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
        
        # Movemos las constantes aquí para que sean dinámicas desde la config
        self.EXCHANGE_IA = self._config.rabbitmq_exchange_ia
        self.COLA_ENTRADA = self._config.rabbitmq_cola_entrada
        self.COLA_SALIDA = self._config.rabbitmq_cola_salida_ws
    # ── Ciclo de vida ─────────────────────────────────────────────────────────

    def iniciar(self) -> None:
        """Conecta al broker y comienza a consumir mensajes (bloqueante)."""
        try:
            self._conectar()
            self._configurar_colas()
            logger.info(f"[CONSUMIDOR] Escuchando en {self.COLA_ENTRADA}")
            self._canal.start_consuming()
        except pika.exceptions.AMQPConnectionError as exc:
            logger.critical(f"[CONSUMIDOR] Error de conexión física con RabbitMQ: {exc}")
            raise BrokerComunicacionError("No se pudo establecer conexión con el broker.", detalles=str(exc))
        except KeyboardInterrupt:
            self.detener()

    def detener(self) -> None:
        """Detiene el consumidor de forma limpia."""
        if self._canal and self._canal.is_open:
            self._canal.stop_consuming()
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

    def _configurar_colas(self) -> None:
        """Declara el exchange, las colas y el binding de forma idempotente."""
        # Exchange de entrada (durable: sobrevive reinicios del broker)
        self._canal.exchange_declare(
            exchange=self.EXCHANGE_IA,
            exchange_type="direct",
            durable=True,
        )

        # Cola de entrada: donde llegan los eventos de transacciones
        self._canal.queue_declare(queue=self.COLA_ENTRADA, durable=True)
        self._canal.queue_bind(
            queue=self.COLA_ENTRADA,
            exchange=self.EXCHANGE_IA,
            routing_key=self.COLA_ENTRADA,
        )

        # Cola de salida: donde publicamos los consejos para WhatsApp
        self._canal.queue_declare(queue=self.COLA_SALIDA, durable=True)

        # Registrar el callback
        self._canal.basic_consume(
            queue=self.COLA_ENTRADA,
            on_message_callback=self._callback,
            auto_ack=False,   # ACK manual para garantizar entrega
        )
        logger.info(
            "[CONSUMIDOR] Colas configuradas: entrada='%s', salida='%s'",
            self.COLA_ENTRADA,
            self.COLA_SALIDA,
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

    def _callback(self, canal, metodo, propiedades, cuerpo) -> None:
        """Lógica de procesamiento con manejo de excepciones de dominio."""
        try:
            # 1. Deserialización
            evento = self._deserializar(cuerpo)
            
            # 2. IA
            consejo = self._coach.generar_consejo(evento)
            if not consejo:
                # Si Gemini falló internamente, hacemos NACK y no reintentamos (evitar bucle)
                canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=False)
                return

            # 3. Publicación
            if self._publicar_consejo(canal, evento, consejo):
                canal.basic_ack(delivery_tag=metodo.delivery_tag)
            else:
                # Si falló la publicación, reintentamos (requeue=True)
                canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=True)

        except ContratoDatosError:
            # Si el dato está roto, no sirve de nada reintentar. ACK para eliminar de la cola.
            logger.error("[CALLBACK] Contrato inválido. Descartando mensaje.")
            canal.basic_ack(delivery_tag=metodo.delivery_tag)
        except Exception as exc:
            logger.error(f"[CALLBACK] Error inesperado: {exc}")
            canal.basic_nack(delivery_tag=metodo.delivery_tag, requeue=True)

    # ── Métodos de soporte ────────────────────────────────────────────────────
    def _deserializar(self, cuerpo: bytes) -> EventoAnalisisIA:
        """Usa el modelo y lanza nuestra excepción personalizada si falla."""
        try:
            logger.debug(
                "[DESERIALIZAR] Evento válido: transacción='%s', monto=%.2f, "
                "tiene_perfil=%s, tiene_metas=%s, tiene_limite=%s",
                EventoAnalisisIA.transaccion.descripcion,
                EventoAnalisisIA.transaccion.monto,
                EventoAnalisisIA.contexto.tiene_perfil,
                EventoAnalisisIA.contexto.tiene_metas,
                EventoAnalisisIA.contexto.tiene_limite_activo,
            )
            return EventoAnalisisIA.desde_json(cuerpo)
        except Exception as exc:
            raise ContratoDatosError("El mensaje de Java no coincide con el modelo IA", detalles=str(exc))

    def _publicar_consejo(
        self,
        canal: BlockingChannel,
        evento: EventoAnalisisIA,
        consejo: str,
    ) -> bool:
        """
        Publica el consejo enriquecido en la cola de salida.
        Usa las variables de instancia configuradas en el __init__.
        """
        try:
            # Construimos el payload con TU lógica original (es excelente)
            payload = {
                "consejo":     consejo,
                "descripcion": evento.transaccion.descripcion,
                "monto":       evento.transaccion.monto,
                "categoria":   evento.transaccion.categoria,
                "tipo":        evento.transaccion.tipo.value,
            }

            # Enriquecemos con el tono si existe perfil
            if evento.contexto.tiene_perfil and evento.contexto.perfil_financiero:
                payload["tono_ia"] = evento.contexto.perfil_financiero.tono_ia.value

            # Publicación usando variables de instancia (self.COLA_SALIDA)
            canal.basic_publish(
                exchange="", # EXCHANGE_DEFAULT
                routing_key=self.COLA_SALIDA, # Usamos la variable del __init__
                body=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
                properties=BasicProperties(
                    delivery_mode=2,           # Persistente
                    content_type="application/json",
                    content_encoding="utf-8",
                ),
            )
            
            logger.info(
                f"[PUBLICAR] Consejo publicado en '{self.COLA_SALIDA}' para '{evento.transaccion.descripcion}'."
            )
            return True

        except Exception as exc:
            # Rastrear el error específico en la publicación
            logger.error(
                f"[PUBLICAR] Error crítico al publicar en '{self.COLA_SALIDA}': {exc}",
                exc_info=True,
            )
            return False