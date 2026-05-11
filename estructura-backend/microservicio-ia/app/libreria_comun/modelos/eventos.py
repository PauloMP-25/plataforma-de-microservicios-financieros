from datetime import datetime
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field, ConfigDict
from pydantic.alias_generators import to_camel

class BaseDTO(BaseModel):
    """Base para todos los DTOs con configuración camelCase."""
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

class EventoAuditoriaDTO(BaseDTO):
    """DTO para registro de auditoría asíncrona."""
    usuario_id: str
    accion: str
    modulo: str
    ip_origen: str
    detalles: Optional[str] = None
    fecha: datetime = Field(default_factory=datetime.now)

class EventoAccesoDTO(BaseDTO):
    """DTO para eventos de seguridad y acceso."""
    usuario_id: str
    tipo_evento: str  # LOGIN, LOGOUT, INTENTO_FALLIDO
    ip: str
    navegador: str
    exitoso: bool
    mensaje: Optional[str] = None
    fecha: datetime = Field(default_factory=datetime.now)

class EventoTransaccionalDTO(BaseDTO):
    """DTO para eventos de movimientos financieros."""
    transaccion_id: int
    usuario_id: str
    monto: float
    tipo: str  # INGRESO, GASTO
    categoria: str
    descripcion: str
    fecha_transaccion: datetime
    ip_origen: str
    fecha_evento: datetime = Field(default_factory=datetime.now)
