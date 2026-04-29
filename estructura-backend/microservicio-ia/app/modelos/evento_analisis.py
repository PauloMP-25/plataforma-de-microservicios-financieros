"""
modelos/evento_analisis.py
══════════════════════════════════════════════════════════════════════════════
Modelos Pydantic para deserializar el EventoAnalisisIA que llega desde
RabbitMQ (exchange.ia → cola.ia.procesamiento) tras cada transacción
registrada por el microservicio-nucleo-financiero.

Diseñado con valores por defecto en todos los campos opcionales para
tolerar contextos incompletos o nulos sin lanzar ValidationError.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field, field_validator


# ── Enumeraciones ─────────────────────────────────────────────────────────────

class TipoMovimiento(str, Enum):
    INGRESO = "INGRESO"
    GASTO   = "GASTO"


class TonoIA(str, Enum):
    """
    Tono de comunicación del coach financiero.
    Controla el estilo del prompt enviado a Gemini.
    """
    MOTIVADOR  = "Motivador"
    FORMAL     = "Formal"
    AMIGABLE   = "Amigable"
    DIRECTO    = "Directo"
    EMPÁTICO   = "Empático"


# ── Sub-modelos ───────────────────────────────────────────────────────────────

class TransaccionEvento(BaseModel):
    """Datos mínimos de la transacción que disparó el análisis."""
    monto:       float  = Field(..., gt=0,  description="Monto de la transacción")
    descripcion: str    = Field(..., min_length=1, description="Descripción del comercio o concepto")
    categoria:   str    = Field(..., min_length=1, description="Categoría (ej: Entretenimiento)")
    tipo:        TipoMovimiento = Field(..., description="GASTO o INGRESO")


class PerfilFinanciero(BaseModel):
    """Contexto del perfil del usuario extraído del microservicio-cliente."""
    ocupacion:       str    = Field(default="No especificada")
    ingreso_mensual: float  = Field(default=0.0, alias="ingresoMensual", ge=0)
    tono_ia:         TonoIA = Field(default=TonoIA.AMIGABLE, alias="tonoIA")

    model_config = {"populate_by_name": True}

    @field_validator("tono_ia", mode="before")
    @classmethod
    def normalizar_tono(cls, v):
        """Acepta el tono en cualquier capitalización."""
        if isinstance(v, str):
            # Busca coincidencia case-insensitive
            for tono in TonoIA:
                if tono.value.lower() == v.lower():
                    return tono
        return TonoIA.AMIGABLE  # Fallback seguro


class MetaAhorro(BaseModel):
    """Una meta financiera activa del usuario."""
    nombre:         str   = Field(..., min_length=1)
    monto_objetivo: float = Field(..., alias="montoObjetivo", gt=0)
    monto_actual:   float = Field(default=0.0, alias="montoActual", ge=0)

    model_config = {"populate_by_name": True}

    @property
    def progreso_porcentaje(self) -> float:
        """Calcula el % de avance hacia la meta."""
        if self.monto_objetivo <= 0:
            return 0.0
        return round((self.monto_actual / self.monto_objetivo) * 100, 1)

    @property
    def monto_restante(self) -> float:
        return round(max(self.monto_objetivo - self.monto_actual, 0.0), 2)


class LimiteGlobal(BaseModel):
    """Límite de gasto mensual configurado por el usuario."""
    monto_limite:        float = Field(default=0.0, alias="montoLimite",        ge=0)
    porcentaje_alerta:   int   = Field(default=80,  alias="porcentajeAlerta",   ge=1, le=100)
    activo:              bool  = Field(default=False)

    model_config = {"populate_by_name": True}


class ContextoUsuario(BaseModel):
    """
    Enriquecimiento del evento: perfil, metas y límites del usuario.
    Todos los campos son opcionales para tolerar contextos parciales.
    """
    perfil_financiero: Optional[PerfilFinanciero] = Field(
        default=None, alias="perfilFinanciero"
    )
    metas:             List[MetaAhorro]            = Field(default_factory=list)
    limite_global:     Optional[LimiteGlobal]      = Field(
        default=None, alias="limiteGlobal"
    )

    model_config = {"populate_by_name": True}

    @property
    def tiene_perfil(self) -> bool:
        return self.perfil_financiero is not None

    @property
    def tiene_metas(self) -> bool:
        return len(self.metas) > 0

    @property
    def tiene_limite_activo(self) -> bool:
        return (
            self.limite_global is not None
            and self.limite_global.activo
            and self.limite_global.monto_limite > 0
        )


# ── Modelo raíz del mensaje RabbitMQ ─────────────────────────────────────────

class EventoAnalisisIA(BaseModel):
    """
    Objeto completo que llega desde cola.ia.procesamiento.
    Contiene la transacción que disparó el evento + el contexto del usuario.
    """
    transaccion: TransaccionEvento
    contexto:    ContextoUsuario = Field(default_factory=ContextoUsuario)

    @classmethod
    def desde_json(cls, raw: str | bytes | dict) -> "EventoAnalisisIA":
        """
        Factoría conveniente: acepta JSON string, bytes o dict ya parseado.
        Lanza ValueError si la estructura mínima (transacción) no es válida.
        """
        import json
        if isinstance(raw, (str, bytes)):
            raw = json.loads(raw)
        return cls.model_validate(raw)