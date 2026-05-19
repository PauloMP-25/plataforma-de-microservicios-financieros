"""
libreria_comun/respuesta/resultado_api.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Espejo exacto de ResultadoApi.java (libreria-comun) en Python/Pydantic.
Garantiza que el Gateway y el Frontend reciban el mismo contrato JSON
independientemente de si la respuesta viene de Java o Python.
══════════════════════════════════════════════════════════════════════════════
"""
from datetime import datetime, timezone
from typing import Any, List, Optional, TypeVar, Generic
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

T = TypeVar("T")


class PaginacionDTO(BaseModel):
    """
    Metadatos de paginación — espejo de PaginacionDTO.java.
    Se incluye en respuestas que listan colecciones paginadas.
    """
    pagina: int = Field(..., ge=0, description="Número de página actual (base 0).")
    tamanio: int = Field(..., ge=1, le=100, description="Tamaño de página (máx 100).")
    total_elementos: int = Field(..., ge=0, description="Total de elementos en la colección.")
    total_paginas: int = Field(..., ge=0, description="Total de páginas disponibles.")

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )


class ResultadoApi(BaseModel, Generic[T]):
    """
    Envoltorio estándar para TODAS las respuestas HTTP del ecosistema LUKA.
    Espejo exacto de ResultadoApi<T>.java (libreria-comun).

    Contrato JSON (camelCase via alias_generator):
      {
        "exito": true,
        "mensaje": "Análisis completado",
        "datos": { ... },
        "codigoError": null,
        "detalles": [],
        "ruta": "/api/v1/ia/gasto-hormiga",
        "timestamp": "2026-05-18T19:00:00Z",
        "paginacion": null
      }
    """
    exito: bool = Field(..., description="Indica si la operación fue exitosa.")
    mensaje: str = Field(..., description="Mensaje descriptivo del resultado.")
    datos: Optional[T] = Field(None, description="Carga útil de la respuesta.")
    codigo_error: Optional[str] = Field(None, description="Código de error interno (si aplica).")
    detalles: List[str] = Field(default_factory=list, description="Lista de errores detallados.")
    ruta: Optional[str] = Field(None, description="Ruta HTTP que generó la respuesta.")
    timestamp: str = Field(
        default_factory=lambda: datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        description="Timestamp ISO-8601 UTC de la respuesta.",
    )
    paginacion: Optional[PaginacionDTO] = Field(None, description="Metadatos de paginación (si aplica).")

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True,
    )

    @classmethod
    def exito_res(
        cls,
        datos: Any = None,
        mensaje: str = "Operación exitosa",
        ruta: Optional[str] = None,
    ) -> "ResultadoApi":
        """Factory method: respuesta exitosa con datos opcionales."""
        return cls(exito=True, mensaje=mensaje, datos=datos, ruta=ruta)

    @classmethod
    def exito_paginado(
        cls,
        datos: Any,
        paginacion: "PaginacionDTO",
        mensaje: str = "Consulta exitosa",
        ruta: Optional[str] = None,
    ) -> "ResultadoApi":
        """Factory method: respuesta exitosa con paginación incluida."""
        return cls(exito=True, mensaje=mensaje, datos=datos, paginacion=paginacion, ruta=ruta)

    @classmethod
    def error_res(
        cls,
        mensaje: str,
        codigo_error: str = "ERROR_INTERNO",
        detalles: Optional[List[str]] = None,
        ruta: Optional[str] = None,
    ) -> "ResultadoApi":
        """Factory method: respuesta de error estructurada."""
        return cls(
            exito=False,
            mensaje=mensaje,
            codigo_error=codigo_error,
            detalles=detalles or [],
            ruta=ruta,
        )
