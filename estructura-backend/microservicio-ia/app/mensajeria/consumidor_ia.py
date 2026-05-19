"""
mensajeria/consumidor_ia.py  ·  v5 — FASE 4: Mensajería Resiliente (LUKA-COACH V4)
══════════════════════════════════════════════════════════════════════════════
Consumidor RabbitMQ con topología robusta de producción:

Topología v5:
  exchange.ia (entrada, direct, durable)
    └── cola.ia.procesamiento   ← mensajes de análisis
    └── cola.ia.clasificacion   ← clasificación on-the-fly
    └── cola.ia.procesamiento.dlq ← mensajes que superaron MAX_REINTENTOS

  exchange.ia.dlx (Dead Letter Exchange, direct, durable)
    └── cola.ia.procesamiento.dlq

  exchange.dashboard (salida, direct, durable)
    └── cola.dashboard.consejos
    └── cola.dashboard.modulos

Flujo de resiliencia:
  1. Mensaje llega → _callback con ACK manual (prefetch=1)
  2. Error de negocio transitorio:
       x-death-count < MAX_REINTENTOS → basic_nack(requeue=False) → DLX → reintento TTL
  3. Error permanente o MAX_REINTENTOS alcanzado → basic_nack(requeue=False) → DLQ
  4. Éxito → persiste en OutboxEvento(PENDIENTE) → basic_ack
  5. OutboxScheduler (en main.py) publica desde DB a RabbitMQ de forma independiente
══════════════════════════════════════════════════════════════════════════════
"""

import json
import logging
import threading
import asyncio
from datetime import datetime
from typing import Optional

import pika
import pika.exceptions
from pika import BasicProperties
from pika.adapters.blocking_connection import BlockingChannel

from app.configuracion import obtener_configuracion
from app.modelos.esquemas import (
    NombreModulo,
    RespuestaModulo,
    PeticionConFiltroFecha,
    SolicitudClasificacionDTO,
)
from app.servicios.core.servicio_analisis import ServicioAnalisis
from app.servicios.ia.coach_ia import CoachIA
from app.utilidades.excepciones import BrokerComunicacionError, ContratoDatosError
from app.persistencia.database import SessionLocal
from app.persistencia.modelos_db import OutboxEvento, EstadoOutbox

logger = logging.getLogger(__name__)

# Número máximo de reintentos antes de derivar al DLQ
MAX_REINTENTOS = 3
# TTL del reintento en milisegundos (30 segundos entre reintentos)
REINTENTO_TTL_MS = 30_000


class ConsumidorIA:
    """
    Consumidor bloqueante (pika BlockingConnection) con topología resiliente.

    Uso desde main.py en un hilo daemon:
        consumidor = ConsumidorIA()
        hilo = threading.Thread(target=consumidor.iniciar, daemon=True)
        hilo.start()
    """

    def __init__(self) -> None:
        self._config   = obtener_configuracion()
        self._coach    = CoachIA()           # Para clasificación on-the-fly
        self._servicio = ServicioAnalisis()  # Orquestador principal
        self._conexion: Optional[pika.BlockingConnection] = None
        self._canal:    Optional[BlockingChannel]         = None
        self._activo   = threading.Event()
        self._activo.set()

        # Aliases de config
        self.EXCHANGE_IA        = self._config.exchange_ia
        self.EXCHANGE_DLX       = f"{self._config.exchange_ia}.dlx"
        self.COLA_ENTRADA       = self._config.cola_ia_procesamiento
        self.COLA_DLQ           = f"{self._config.cola_ia_procesamiento}.dlq"
        self.COLA_CLASIFICACION = self._config.cola_ia_clasificacion
        self.COLA_SALIDA        = self._config.cola_dashboard_consejos

    # ── Ciclo de vida ──────────────────────────────────────────────────────────

    def iniciar(self) -> None:
        """Conecta al broker y comienza a consumir mensajes (bloqueante)."""
        try:
            self._conectar()
            self._declarar_topologia()
            logger.info("[CONSUMIDOR-v5] Escuchando en %s", self.COLA_ENTRADA)
            self._canal.start_consuming()
        except pika.exceptions.AMQPConnectionError as exc:
            logger.critical("[CONSUMIDOR] Error físico con RabbitMQ: %s", exc)
            raise BrokerComunicacionError("Conexión con el broker perdida.", detalles=str(exc))
        except KeyboardInterrupt:
            self.detener()

    def detener(self) -> None:
        """Detiene el consumidor de forma limpia desde otro hilo."""
        self._activo.clear()
        if self._canal and self._canal.is_open:
            self._conexion.add_callback_threadsafe(self._canal.stop_consuming)
        self._cerrar()

    # ── Conexión ───────────────────────────────────────────────────────────────

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
                client_properties={"connection_name": self._config.rabbitmq_connection_name},
                heartbeat=self._config.rabbitmq_heartbeat,
                blocked_connection_timeout=self._config.rabbitmq_timeout,
            )
            self._conexion = pika.BlockingConnection(parametros)
            self._canal    = self._conexion.channel()
            self._canal.basic_qos(prefetch_count=1)  # Procesa un mensaje a la vez
        except Exception as exc:
            raise BrokerComunicacionError("Fallo al conectar con RabbitMQ", detalles=str(exc))

    # ── Topología resiliente ───────────────────────────────────────────────────

    def _declarar_topologia(self) -> None:
        """
        Declara todos los exchanges y colas de forma idempotente.

        Topología v5 (producción):
          exchange.ia       → cola.ia.procesamiento (con DLX configurado)
                           → cola.ia.clasificacion
          exchange.ia.dlx   → cola.ia.procesamiento.dlq  (Dead Letter Queue)
          exchange.dashboard → cola.dashboard.consejos / cola.dashboard.modulos
        """
        cfg = self._config

        # ── DLX y DLQ (deben declararse ANTES que la cola principal) ──────────
        self._canal.exchange_declare(
            exchange=self.EXCHANGE_DLX, exchange_type="direct", durable=True
        )
        self._canal.queue_declare(
            queue=self.COLA_DLQ,
            durable=True,
            arguments={"x-queue-type": "classic"},
        )
        self._canal.queue_bind(
            queue=self.COLA_DLQ,
            exchange=self.EXCHANGE_DLX,
            routing_key=self.COLA_ENTRADA,  # La clave de routing coincide con la cola origen
        )
        logger.info("[TOPOLOGIA] DLX '%s' → DLQ '%s' declarados", self.EXCHANGE_DLX, self.COLA_DLQ)

        # ── Exchange de entrada ───────────────────────────────────────────────
        self._canal.exchange_declare(
            exchange=cfg.exchange_ia, exchange_type="direct", durable=True
        )

        # Cola principal con DLX y TTL de reintento configurados
        self._canal.queue_declare(
            queue=cfg.cola_ia_procesamiento,
            durable=True,
            arguments={
                "x-dead-letter-exchange": self.EXCHANGE_DLX,       # Redirige fallidos al DLX
                "x-dead-letter-routing-key": self.COLA_ENTRADA,     # Routing key en DLX
                "x-message-ttl": 600_000,                           # 10 min TTL por mensaje
            },
        )
        self._canal.queue_bind(
            queue=cfg.cola_ia_procesamiento,
            exchange=cfg.exchange_ia,
            routing_key=cfg.cola_ia_procesamiento,
        )

        # Cola de clasificación (sin DLX por ser operación on-the-fly)
        self._canal.queue_declare(queue=cfg.cola_ia_clasificacion, durable=True)
        self._canal.queue_bind(
            queue=cfg.cola_ia_clasificacion,
            exchange=cfg.exchange_ia,
            routing_key=cfg.cola_ia_clasificacion,
        )

        # ── Exchange de salida (Dashboard) ────────────────────────────────────
        self._canal.exchange_declare(
            exchange=cfg.exchange_dashboard, exchange_type="direct", durable=True
        )
        for cola in [cfg.cola_dashboard_consejos, cfg.cola_dashboard_modulos]:
            self._canal.queue_declare(queue=cola, durable=True)
            self._canal.queue_bind(queue=cola, exchange=cfg.exchange_dashboard, routing_key=cola)

        # ── Registrar callbacks con ACK manual explícito ──────────────────────
        self._canal.basic_consume(
            queue=cfg.cola_ia_procesamiento,
            on_message_callback=self._callback,
            auto_ack=False,  # ACK manual obligatorio
        )
        self._canal.basic_consume(
            queue=cfg.cola_ia_clasificacion,
            on_message_callback=self._callback_clasificacion,
            auto_ack=False,
        )

        logger.info(
            "[CONSUMIDOR-v5] Topología lista | entrada: %s | DLQ: %s | salida: %s",
            cfg.cola_ia_procesamiento, self.COLA_DLQ, cfg.exchange_dashboard,
        )

    # ── Callback principal (análisis) ─────────────────────────────────────────

    def _callback(self, canal, metodo, propiedades, cuerpo) -> None:
        """
        Procesa mensajes de análisis IA con ACK manual y política de reintentos.

        Política de reintentos:
          - El header 'x-death' de RabbitMQ contiene el historial de muertes.
          - Si reintentos < MAX_REINTENTOS → basic_nack(requeue=False) → DLX → reencola después de TTL.
          - Si reintentos >= MAX_REINTENTOS → basic_nack(requeue=False) → DLQ definitivo.
        """
        tag = metodo.delivery_tag

        # Contar reintentos previos desde el header x-death
        reintentos_previos = self._contar_reintentos(propiedades)

        try:
            # 1. Deserializar
            datos   = json.loads(cuerpo)
            peticion = PeticionConFiltroFecha(**datos)
            logger.info(
                "[CALLBACK] Procesando usuario=%s módulo=%s (intento %d/%d)",
                peticion.usuario_id,
                datos.get("modulo", "?"),
                reintentos_previos + 1,
                MAX_REINTENTOS,
            )

            # 2. Determinar módulo
            nombre_modulo_str = datos.get("modulo", NombreModulo.REPORTE_COMPLETO.value)
            try:
                modulo = NombreModulo(nombre_modulo_str)
            except ValueError:
                logger.warning("[CALLBACK] Módulo desconocido '%s', usando REPORTE_COMPLETO", nombre_modulo_str)
                modulo = NombreModulo.REPORTE_COMPLETO

            # 3. Procesar (motor analítico + Gemini)
            resultado: RespuestaModulo = asyncio.run(
                self._servicio.procesar_modulo(modulo, peticion, "rabbitmq")
            )

            # 4. Persistir en Outbox (at-least-once delivery)
            cola_destino = (
                self._config.cola_dashboard_modulos
                if peticion.mes
                else self._config.cola_dashboard_consejos
            )
            self._guardar_en_outbox(resultado, cola_destino)

            # 5. ACK — mensaje procesado exitosamente
            canal.basic_ack(delivery_tag=tag)
            logger.info("[CALLBACK] ✅ Procesado y guardado en Outbox | usuario=%s", peticion.usuario_id)

        except (json.JSONDecodeError, TypeError, ContratoDatosError) as exc:
            # Mensaje malformado — no se puede reintentar, descartarlo
            logger.error("[CALLBACK] ❌ Mensaje malformado descartado: %s", exc)
            canal.basic_ack(delivery_tag=tag)  # ACK para limpiar el mensaje corrupto

        except Exception as exc:
            logger.error("[CALLBACK] ⚠️ Error en procesamiento (intento %d): %s", reintentos_previos + 1, exc)

            if reintentos_previos >= MAX_REINTENTOS - 1:
                # Superó reintentos → DLQ definitivo
                logger.error(
                    "[CALLBACK] 🚨 Máx reintentos (%d) alcanzados → DLQ | usuario=%s",
                    MAX_REINTENTOS,
                    datos.get("usuario_id", "?") if "datos" in dir() else "?",
                )
                canal.basic_nack(delivery_tag=tag, requeue=False)
            else:
                # Reintento disponible → DLX (requeue=False envía al DLX configurado)
                canal.basic_nack(delivery_tag=tag, requeue=False)

    # ── Callback de clasificación on-the-fly ──────────────────────────────────

    def _callback_clasificacion(self, ch, method, properties, body) -> None:
        """Procesa solicitudes de clasificación en tiempo real con ACK manual."""
        try:
            datos     = json.loads(body)
            solicitud = SolicitudClasificacionDTO(**datos)
            rol_usuario = datos.get("rol", "PRO")

            self._coach._verificar_cuota_diaria(
                datos.get("usuario_id", "N/A"),
                NombreModulo.AUTO_CLASIFICACION,
                rol_usuario,
            )

            from app.servicios.ia.clasificador_ia import ClasificadorIAService
            clasificador = ClasificadorIAService()
            respuesta = asyncio.run(clasificador.clasificar(solicitud))

            ch.basic_publish(
                exchange=self.EXCHANGE_IA,
                routing_key="ia.clasificacion.resultado",
                body=json.dumps(respuesta.model_dump()),
                properties=BasicProperties(delivery_mode=2, content_type="application/json"),
            )
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info("[CLASIFICADOR] ✅ Sugerencias enviadas para ID: %s", solicitud.id_temporal)

        except Exception as exc:
            logger.error("[CLASIFICADOR] ❌ Error: %s", exc)
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)

    # ── Outbox ─────────────────────────────────────────────────────────────────

    def _guardar_en_outbox(self, resultado: RespuestaModulo, cola_destino: str) -> None:
        """
        Persiste el resultado en ia_outbox_eventos (estado PENDIENTE).
        El OutboxScheduler lo publicará a RabbitMQ de forma independiente.
        """
        try:
            with SessionLocal() as db:
                evento = OutboxEvento(
                    usuario_id    = resultado.usuario_id,
                    modulo        = resultado.modulo.value,
                    cola_destino  = cola_destino,
                    exchange      = self._config.exchange_dashboard,
                    payload_json  = json.dumps(resultado.a_dict_serializable(), ensure_ascii=False),
                    estado        = EstadoOutbox.PENDIENTE,
                    max_reintentos = MAX_REINTENTOS,
                    creado_en     = datetime.now(),
                )
                db.add(evento)
                db.commit()
                logger.debug("[OUTBOX] Evento persistido id=%d | usuario=%s", evento.id, resultado.usuario_id)
        except Exception as exc:
            # No falla el flujo principal; log es suficiente
            logger.error("[OUTBOX] ❌ No se pudo persistir evento en DB: %s", exc)

    # ── Publicación directa (para OutboxScheduler) ────────────────────────────

    def _publicar_resultado(self, canal, resultado: RespuestaModulo, cola_destino: str) -> bool:
        """Serializa y publica el objeto RespuestaModulo al Dashboard."""
        try:
            body = json.dumps(resultado.a_dict_serializable(), ensure_ascii=False).encode("utf-8")
            canal.basic_publish(
                exchange   = self._config.exchange_dashboard,
                routing_key = cola_destino,
                body       = body,
                properties = BasicProperties(
                    delivery_mode  = 2,
                    content_type   = "application/json",
                    headers = {
                        "modulo":       resultado.modulo.value,
                        "estado_coach": resultado.estado_coach.value,
                    },
                ),
            )
            return True
        except Exception as exc:
            logger.error("[PUBLICAR] ❌ Fallo al publicar: %s", exc)
            return False

    # ── Utilidades ─────────────────────────────────────────────────────────────

    @staticmethod
    def _contar_reintentos(propiedades) -> int:
        """
        Extrae el número de veces que el mensaje ha pasado por el DLX
        leyendo el header estándar 'x-death' de RabbitMQ.
        """
        try:
            headers = propiedades.headers or {}
            x_death = headers.get("x-death", [])
            if x_death:
                return int(x_death[0].get("count", 0))
        except Exception:
            pass
        return 0

    def _cerrar(self) -> None:
        """Cierra el canal y la conexión de forma segura."""
        try:
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
                logger.info("[CONSUMIDOR] Conexión RabbitMQ cerrada.")
        except Exception as exc:
            logger.warning("[CONSUMIDOR] Error al cerrar conexión: %s", exc)