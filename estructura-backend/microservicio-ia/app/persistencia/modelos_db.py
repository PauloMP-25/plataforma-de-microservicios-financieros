"""
persistencia/modelos_db.py  ·  v2.0 — FASE 4: Patrón Outbox (LUKA-COACH V4)
══════════════════════════════════════════════════════════════════════════════
Modelos SQLAlchemy para la base de datos PostgreSQL del Microservicio IA.
  - IaAnalisisCache   : Caché de consejos Gemini (evita re-llamadas costosas).
  - IaRetoAhorro      : Retos gamificados persistidos por usuario.
  - OutboxEvento      : Patrón Transactional Outbox (at-least-once delivery).
══════════════════════════════════════════════════════════════════════════════
"""
import enum
from datetime import datetime

from sqlalchemy import Column, String, Integer, Text, Boolean, DateTime, Float, func
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


# ── 1. Cache de análisis IA ───────────────────────────────────────────────────

class IaAnalisisCache(Base):
    """
    Persistencia de consejos generados por Gemini.
    Segunda capa de caché (Redis es la primera) para ahorrar tokens.
    """
    __tablename__ = "ia_analisis_cache"

    hash_datos      = Column(String(64), primary_key=True)
    cliente_id      = Column(String(50), nullable=False, index=True)
    modulo          = Column(String(50), nullable=False)
    version_modulo  = Column(Integer, default=1)
    prompt_usado    = Column(Text, nullable=False)
    consejo_gemini  = Column(Text, nullable=False)
    tokens_usados   = Column(Integer, default=0)
    costo_usd       = Column(Float, default=0.0)
    usando_fallback = Column(Boolean, default=False)
    fecha_creacion  = Column(DateTime, default=datetime.now)

    def __repr__(self):
        return f"<IaAnalisisCache(hash={self.hash_datos}, cliente={self.cliente_id}, modulo={self.modulo})>"


# ── 2. Retos de ahorro gamificados ────────────────────────────────────────────

class IaRetoAhorro(Base):
    """Persistencia de retos gamificados de ahorro por usuario."""
    __tablename__ = "ia_retos_ahorro"

    id            = Column(Integer, primary_key=True, index=True)
    usuario_id    = Column(String(50), index=True)
    categoria     = Column(String(50))
    monto_limite  = Column(Float)
    fecha_inicio  = Column(DateTime, default=func.now())
    fecha_fin     = Column(DateTime)
    frecuencia    = Column(String(20))  # SEMANAL, QUINCENAL, MENSUAL
    estado        = Column(String(20), default="ACTIVO")  # ACTIVO, PENDIENTE_VEREDICTO, FINALIZADO
    ahorro_logrado = Column(Float, default=0.0)
    veredicto_ia  = Column(Text, nullable=True)

    def __repr__(self):
        return f"<IaRetoAhorro(id={self.id}, usuario={self.usuario_id}, estado={self.estado})>"


# ── 3. Patrón Transactional Outbox ────────────────────────────────────────────

class EstadoOutbox(str, enum.Enum):
    """Ciclo de vida de un evento en la tabla outbox."""
    PENDIENTE = "PENDIENTE"   # Esperando ser publicado a RabbitMQ
    PROCESADO = "PROCESADO"   # Publicado exitosamente
    FALLIDO   = "FALLIDO"     # Superó max_reintentos — requiere atención manual


class OutboxEvento(Base):
    """
    Patrón Transactional Outbox — garantía at-least-once delivery.

    Flujo:
      1. ConsumidorIA genera RespuestaModulo → persiste aquí (PENDIENTE).
      2. OutboxScheduler lee filas PENDIENTE cada N segundos.
      3. Intenta publicar a RabbitMQ:
           - Éxito → estado = PROCESADO, procesado_en = now()
           - Fallo  → reintentos += 1
           - reintentos >= max_reintentos → estado = FALLIDO (alerta en log)
      4. Si RabbitMQ cae temporalmente, los eventos sobreviven en DB.
    """
    __tablename__ = "ia_outbox_eventos"

    id             = Column(Integer, primary_key=True, autoincrement=True)
    usuario_id     = Column(String(50), nullable=False, index=True)
    modulo         = Column(String(50), nullable=False)
    cola_destino   = Column(String(100), nullable=False)
    exchange       = Column(String(100), nullable=False)
    payload_json   = Column(Text, nullable=False)          # JSON de RespuestaModulo
    estado         = Column(String(20), default=EstadoOutbox.PENDIENTE, nullable=False, index=True)
    reintentos     = Column(Integer, default=0, nullable=False)
    max_reintentos = Column(Integer, default=3, nullable=False)
    creado_en      = Column(DateTime, default=datetime.now, nullable=False)
    procesado_en   = Column(DateTime, nullable=True)
    error_detalle  = Column(Text, nullable=True)

    def __repr__(self):
        return f"<OutboxEvento(id={self.id}, usuario={self.usuario_id}, estado={self.estado})>"
