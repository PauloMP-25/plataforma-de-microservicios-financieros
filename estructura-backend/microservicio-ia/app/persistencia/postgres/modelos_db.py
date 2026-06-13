"""
persistencia/modelos_db.py  ·  v3.0 — FASE 2: Memoria de Coaching (LUKA)
══════════════════════════════════════════════════════════════════════════════
Modelos SQLAlchemy para la base de datos PostgreSQL del Microservicio IA.

  - IaRetoAhorro        : Retos gamificados persistidos por usuario.
  - OutboxEvento        : Patrón Transactional Outbox (at-least-once delivery).
  - IaHistorialCoaching : NUEVO v3 — Memoria histórica por usuario/módulo.
                          Permite que Gemini recuerde el consejo anterior y
                          detecte si el usuario mejoró o empeoró sus hábitos.
══════════════════════════════════════════════════════════════════════════════
"""
import enum
import json
from datetime import datetime

from sqlalchemy import Column, String, Integer, Text, Boolean, DateTime, Float, func
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


# ── 1. Retos de ahorro gamificados ────────────────────────────────────────────

class IaRetoAhorro(Base):
    """Persistencia de retos gamificados de ahorro por usuario."""
    __tablename__ = "ia_retos_ahorro"

    id             = Column(Integer, primary_key=True, index=True)
    usuario_id     = Column(String(50), index=True)
    categoria      = Column(String(50))
    monto_limite   = Column(Float)
    fecha_inicio   = Column(DateTime, default=func.now())
    fecha_fin      = Column(DateTime)
    frecuencia     = Column(String(20))
    estado         = Column(String(20), default="ACTIVO")
    ahorro_logrado = Column(Float, default=0.0)
    veredicto_ia   = Column(Text, nullable=True)

    def __repr__(self):
        return f"<IaRetoAhorro(id={self.id}, usuario={self.usuario_id}, estado={self.estado})>"


class IaRutinaMensual(Base):
    """Persistencia de las rutinas de entrenamiento mensuales (Luka Gym)."""
    __tablename__ = "ia_rutinas_mensuales"

    id = Column(Integer, primary_key=True, autoincrement=True)
    usuario_id = Column(String(50), nullable=False, index=True)
    fecha_generacion = Column(DateTime, default=datetime.now, nullable=False)
    estado_fisico = Column(String(50), nullable=False)
    ejercicios_json = Column(Text, nullable=False)
    estado = Column(String(30), default="ACTIVA", index=True)

    def __repr__(self):
        return f"<IaRutinaMensual(id={self.id}, usuario={self.usuario_id}, estado={self.estado})>"


# ── 2. Patrón Transactional Outbox ────────────────────────────────────────────

class EstadoOutbox(str, enum.Enum):
    PENDIENTE = "PENDIENTE"
    PROCESADO = "PROCESADO"
    FALLIDO   = "FALLIDO"


class OutboxEvento(Base):
    """
    Patrón Transactional Outbox — garantía at-least-once delivery.
    """
    __tablename__ = "ia_outbox_eventos"

    id             = Column(Integer, primary_key=True, autoincrement=True)
    usuario_id     = Column(String(50), nullable=False, index=True)
    modulo         = Column(String(50), nullable=False)
    cola_destino   = Column(String(100), nullable=False)
    exchange       = Column(String(100), nullable=False)
    payload_json   = Column(Text, nullable=False)
    estado         = Column(String(20), default=EstadoOutbox.PENDIENTE, nullable=False, index=True)
    reintentos     = Column(Integer, default=0, nullable=False)
    max_reintentos = Column(Integer, default=3, nullable=False)
    creado_en      = Column(DateTime, default=datetime.now, nullable=False)
    procesado_en   = Column(DateTime, nullable=True)
    error_detalle  = Column(Text, nullable=True)

    def __repr__(self):
        return f"<OutboxEvento(id={self.id}, usuario={self.usuario_id}, estado={self.estado})>"


# ── 3. Historial de Coaching IA — NUEVO v3 ────────────────────────────────────

class IaHistorialCoaching(Base):
    """
    Memoria histórica de interacciones del Coach IA por usuario y módulo.

    Propósito:
      Permite que Gemini recuerde el último consejo entregado y detecte
      si el usuario mejoró o empeoró sus hábitos financieros en la sesión
      siguiente. Esto habilita el "Coaching Interactivo" de LUKA.

    Flujo de uso:
      1. Antes de generar el prompt → leer el último registro de este
         (usuario_id, modulo) para obtener el historial previo.
      2. Inyectar historial en metricas["_historial_previo"].
      3. Después de recibir respuesta exitosa de Gemini → guardar nuevo registro.

    Diseño de columnas JSON:
      - insight_calculado  : dict con las métricas del motor Pandas
                             (ej: {"total_gastos_hormiga": 120.5, ...}).
                             Permite comparar KPIs entre sesiones.
      - consejo_solicitado : el consejo entregado al usuario.
                             str (módulos legacy) o dict (ConsejoEstructurado).
                             Se serializa a JSON antes de guardar.

    Índice compuesto (usuario_id, modulo):
      Optimiza la consulta de "último registro de X módulo para Y usuario"
      que se ejecuta en CADA llamada al orquestador.

    Compatibilidad con tests:
      Usa Text en lugar de JSONB para que SQLite (entorno de tests) lo acepte.
      En PostgreSQL, Text almacena JSON válido igualmente. Si en el futuro
      se requiere indexación JSONB nativa, migrar la columna con Alembic.
    """
    __tablename__ = "ia_historial_coaching"

    id                 = Column(Integer, primary_key=True, autoincrement=True)

    # Clave de búsqueda — índice compuesto declarado abajo
    usuario_id         = Column(String(50), nullable=False, index=True)
    modulo             = Column(String(50), nullable=False, index=True)

    # Temporalidad — usada para ORDER BY al recuperar el último registro
    fecha_generacion   = Column(DateTime, default=datetime.now, nullable=False)

    # Métricas del motor analítico (Pandas) serializadas como JSON
    # Ejemplo: '{"total_gastos_hormiga": 120.5, "principal_gasto_hormiga": "Cafetería"}'
    insight_calculado  = Column(Text, nullable=True)

    # Consejo entregado al usuario, serializado como JSON
    # str plano  → guardado como JSON string: '"texto del consejo..."'
    # dict       → guardado como JSON object: '{"introduccion": "...", ...}'
    consejo_solicitado = Column(Text, nullable=True)

    # Estado del coach en esa sesión (EXITOSO, NO_DISPONIBLE, etc.)
    estado_coach       = Column(String(30), nullable=True)

    def __repr__(self):
        return (
            f"<IaHistorialCoaching("
            f"id={self.id}, "
            f"usuario={self.usuario_id}, "
            f"modulo={self.modulo}, "
            f"fecha={self.fecha_generacion})>"
        )

    # ── Helpers de serialización ──────────────────────────────────────────────

    def set_insight(self, metricas: dict) -> None:
        """Serializa el dict de métricas a JSON para persistir en Text."""
        try:
            self.insight_calculado = json.dumps(metricas, ensure_ascii=False, default=str)
        except (TypeError, ValueError):
            self.insight_calculado = None

    def get_insight(self) -> dict:
        """Deserializa el JSON de insight a dict. Retorna {} si falla."""
        if not self.insight_calculado:
            return {}
        try:
            return json.loads(self.insight_calculado)
        except (json.JSONDecodeError, TypeError):
            return {}

    def set_consejo(self, consejo) -> None:
        """
        Serializa el consejo (str o dict/ConsejoEstructurado) a JSON.

        - Si es str  → json.dumps produce '"texto..."' (JSON string válido).
        - Si es dict → json.dumps produce '{...}' (JSON object).
        - Si es BaseModel (ConsejoEstructurado) → llamar .model_dump() primero.
        """
        try:
            from pydantic import BaseModel as PydanticBaseModel
            valor = consejo.model_dump() if isinstance(consejo, PydanticBaseModel) else consejo
            self.consejo_solicitado = json.dumps(valor, ensure_ascii=False, default=str)
        except (TypeError, ValueError):
            self.consejo_solicitado = None

    def get_consejo(self):
        """
        Deserializa el consejo guardado.
        Retorna dict si era ConsejoEstructurado, str si era texto plano, None si falla.
        """
        if not self.consejo_solicitado:
            return None
        try:
            return json.loads(self.consejo_solicitado)
        except (json.JSONDecodeError, TypeError):
            return None
