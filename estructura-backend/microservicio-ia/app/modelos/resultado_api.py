from datetime import datetime
from typing import Any, Generic, List, Optional, TypeVar
from pydantic import BaseModel, Field
from app.modelos.codigos_error import CodigoError

T = TypeVar("T")

class Paginacion(BaseModel, Generic[T]):
    """Información de paginación compatible con Java."""
    total_elementos: int = Field(..., alias="totalElementos")
    total_paginas: int = Field(..., alias="totalPaginas")
    pagina_actual: int = Field(..., alias="paginaActual")
    tamanio_pagina: int = Field(..., alias="tamanioPagina")
    es_primera: bool = Field(..., alias="esPrimera")
    es_ultima: bool = Field(..., alias="esUltima")

    model_config = {"populate_by_name": True}

class ResultadoApi(BaseModel, Generic[T]):
    """
    Envoltura universal para todas las respuestas de la plataforma LUKA APP.
    Espejo de ResultadoApi.java en libreria-comun.
    """
    exito: bool
    estado: int
    error: Optional[str] = None
    mensaje: str
    datos: Optional[T] = None
    detalles: Optional[List[str]] = None
    pagina: Optional[Paginacion] = None
    ruta: Optional[str] = None
    marca_tiempo: datetime = Field(default_factory=datetime.now, alias="marcaTiempo")

    model_config = {
        "populate_by_name": True,
        "json_encoders": {
            datetime: lambda v: v.isoformat()
        }
    }

    @classmethod
    def ok(cls, datos: Optional[T] = None, mensaje: str = "Operación realizada con éxito", ruta: Optional[str] = None) -> "ResultadoApi[T]":
        return cls(
            exito=True,
            estado=200,
            mensaje=mensaje,
            datos=datos,
            ruta=ruta
        )

    @classmethod
    def creado(cls, datos: T, mensaje: str = "Recurso creado con éxito", ruta: Optional[str] = None) -> "ResultadoApi[T]":
        return cls(
            exito=True,
            estado=201,
            mensaje=mensaje,
            datos=datos,
            ruta=ruta
        )

    @classmethod
    def falla(cls, codigo: CodigoError, mensaje: str, ruta: Optional[str] = None, detalles: Optional[List[str]] = None) -> "ResultadoApi[Void]":
        return cls(
            exito=False,
            estado=codigo.status_code,
            error=codigo.value,
            mensaje=mensaje,
            ruta=ruta,
            detalles=detalles
        )
