"""
mensajeria/outbox_scheduler.py  ·  v1.0 — FASE 4: Transactional Outbox (LUKA-COACH V4)
══════════════════════════════════════════════════════════════════════════════
Scheduler de publicación del Patrón Transactional Outbox.

Responsabilidad única:
  Leer periódicamente la tabla `ia_outbox_eventos` buscando filas con
  estado=PENDIENTE y publicarlas a RabbitMQ. Si la publicación falla,
  incrementa el contador de reintentos. Si supera max_reintentos,
  marca el evento como FALLIDO (requiere atención manual/alerta).

Uso desde main.py (en el lifespan de FastAPI):
    scheduler = OutboxScheduler()
    scheduler.iniciar()           # Lanza hilo daemon
    ...
    scheduler.detener()           # En el shutdown
══════════════════════════════════════════════════════════════════════════════
"""
import json
import logging
import threading
import time
from datetime import datetime
from typing import Optional

import os
import ssl as ssl_lib
import pika
import pika.exceptions
from pika import BasicProperties

from app.configuracion import obtener_configuracion
from app.persistencia.postgres.database import SessionLocal
from app.persistencia.postgres.modelos_db import OutboxEvento, EstadoOutbox

logger = logging.getLogger(__name__)

# Intervalo entre ciclos de publicación (segundos)
INTERVALO_SEGUNDOS = 15
# Máximo de eventos procesados por ciclo (evita timeouts en lotes grandes)
BATCH_SIZE = 20


class OutboxScheduler:
    """
    Scheduler que publica eventos pendientes del Outbox a RabbitMQ.
    Corre en un hilo daemon independiente del consumidor principal.
    """

    def __init__(self) -> None:
        self._config  = obtener_configuracion()
        self._activo  = threading.Event()
        self._hilo:   Optional[threading.Thread] = None
        self._conexion = None
        self._canal    = None

    def iniciar(self) -> None:
        """Lanza el scheduler en un hilo daemon."""
        self._activo.set()
        self._hilo = threading.Thread(
            target=self._loop,
            name="outbox-scheduler",
            daemon=True,
        )
        self._hilo.start()
        logger.info("[OUTBOX-SCHEDULER] Iniciado — intervalo %ds, batch %d", INTERVALO_SEGUNDOS, BATCH_SIZE)

    def detener(self) -> None:
        """Señaliza al hilo que debe detenerse."""
        self._activo.clear()
        self._cerrar_conexion()
        logger.info("[OUTBOX-SCHEDULER] Detenido.")

    # ── Loop principal ─────────────────────────────────────────────────────────

    def _loop(self) -> None:
        """Ciclo principal: espera INTERVALO_SEGUNDOS entre publicaciones."""
        while self._activo.is_set():
            try:
                self._procesar_pendientes()
            except Exception as exc:
                logger.error("[OUTBOX-SCHEDULER] Error en ciclo: %s", exc)
            # Espera interruptible (sale antes si se llama detener())
            self._activo.wait(timeout=INTERVALO_SEGUNDOS)

    def _procesar_pendientes(self) -> None:
        """Lee eventos PENDIENTE de la DB e intenta publicarlos."""
        with SessionLocal() as db:
            pendientes = (
                db.query(OutboxEvento)
                .filter(OutboxEvento.estado == EstadoOutbox.PENDIENTE)
                .order_by(OutboxEvento.creado_en.asc())
                .limit(BATCH_SIZE)
                .all()
            )

            if not pendientes:
                return

            logger.info("[OUTBOX-SCHEDULER] Procesando %d evento(s) pendiente(s)", len(pendientes))
            canal = self._obtener_canal()

            for evento in pendientes:
                if canal is None:
                    logger.warning("[OUTBOX-SCHEDULER] Canal no disponible, abortando ciclo.")
                    break

                try:
                    canal.basic_publish(
                        exchange    = evento.exchange,
                        routing_key = evento.cola_destino,
                        body        = evento.payload_json.encode("utf-8"),
                        properties  = BasicProperties(
                            delivery_mode = 2,
                            content_type  = "application/json",
                        ),
                    )
                    # Éxito → marcar como PROCESADO
                    evento.estado       = EstadoOutbox.PROCESADO
                    evento.procesado_en = datetime.now()
                    db.commit()
                    logger.info(
                        "[OUTBOX-SCHEDULER] ✅ Publicado evento id=%d | usuario=%s | cola=%s",
                        evento.id, evento.usuario_id, evento.cola_destino,
                    )

                except Exception as exc:
                    evento.reintentos    += 1
                    evento.error_detalle  = str(exc)[:500]

                    if evento.reintentos >= evento.max_reintentos:
                        evento.estado = EstadoOutbox.FALLIDO
                        logger.error(
                            "[OUTBOX-SCHEDULER] 🚨 Evento id=%d marcado FALLIDO tras %d intentos: %s",
                            evento.id, evento.reintentos, exc,
                        )
                    else:
                        logger.warning(
                            "[OUTBOX-SCHEDULER] ⚠️ Reintento %d/%d para evento id=%d: %s",
                            evento.reintentos, evento.max_reintentos, evento.id, exc,
                        )

                    db.commit()
                    # Resetear canal para reconectar en el próximo ciclo
                    self._cerrar_conexion()
                    canal = None

    # ── Conexión RabbitMQ ──────────────────────────────────────────────────────

    def _obtener_canal(self):
        """Devuelve el canal activo, reconectando si es necesario."""
        try:
            if self._canal and self._canal.is_open:
                return self._canal
        except Exception:
            pass

        try:
            cfg = self._config
            credenciales = pika.PlainCredentials(cfg.rabbitmq_usuario, cfg.rabbitmq_password)
            ssl_options = None
            if os.getenv("RABBITMQ_SSL_ENABLED", "true").lower() == "true":
                ssl_context = ssl_lib.create_default_context()
                ssl_context.check_hostname = False
                ssl_context.verify_mode = ssl_lib.CERT_NONE
                ssl_options = pika.SSLOptions(ssl_context)
                
            puerto = int(os.getenv("RABBITMQ_PUERTO", str(cfg.rabbitmq_puerto)))
            
            parametros   = pika.ConnectionParameters(
                host=cfg.rabbitmq_host,
                port=puerto,
                virtual_host=cfg.rabbitmq_vhost,
                credentials=credenciales,
                ssl_options=ssl_options,
                client_properties={"connection_name": "outbox-scheduler"},
                heartbeat=60,
                blocked_connection_timeout=10,
            )
            self._conexion = pika.BlockingConnection(parametros)
            self._canal    = self._conexion.channel()
            logger.info("[OUTBOX-SCHEDULER] Conectado a RabbitMQ.")
            return self._canal
        except Exception as exc:
            logger.warning("[OUTBOX-SCHEDULER] Sin conexión RabbitMQ: %s", exc)
            return None

    def _cerrar_conexion(self) -> None:
        try:
            if self._conexion and not self._conexion.is_closed:
                self._conexion.close()
        except Exception:
            pass
        self._conexion = None
        self._canal    = None
