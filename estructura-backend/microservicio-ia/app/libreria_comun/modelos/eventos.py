from datetime import datetime, date
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
    fecha: date = Field(default_factory=date.today)

class EventoAccesoDTO(BaseDTO):
    """DTO para eventos de seguridad y acceso."""
    usuario_id: str
    tipo_evento: str  # LOGIN, LOGOUT, INTENTO_FALLIDO
    ip: str
    navegador: str
    exitoso: bool
    mensaje: Optional[str] = None
    fecha: date = Field(default_factory=date.today)

class EventoTransaccionalDTO(BaseDTO):
    """DTO para registro de cambios en entidades de negocio (Trazabilidad)."""
    usuario_id: str
    entidad_id: str
    servicio_origen: str
    entidad_afectada: str
    descripcion: str
    valor_anterior: Optional[str] = None
    valor_nuevo: Optional[str] = None
    fecha: date = Field(default_factory=date.today)
