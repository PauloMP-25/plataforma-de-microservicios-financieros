from typing import Any, List, Optional, TypeVar, Generic
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

T = TypeVar("T")

class ResultadoApi(BaseModel, Generic[T]):
    """
    Clase genérica para estandarizar las respuestas de la API,
    espejo de ResultadoApi.java en Spring Boot.
    """
    exito: bool = Field(..., description="Indica si la operación fue exitosa")
    mensaje: str = Field(..., description="Mensaje descriptivo del resultado")
    datos: Optional[T] = Field(None, description="Carga útil de la respuesta")
    codigo_error: Optional[str] = Field(None, description="Código de error interno (si aplica)")
    detalles: List[str] = Field(default_factory=list, description="Lista de errores detallados o trazas")

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

    @classmethod
    def exito_res(cls, datos: Any = None, mensaje: str = "Operación exitosa"):
        """Retorna una respuesta exitosa."""
        return cls(exito=True, mensaje=mensaje, datos=datos)

    @classmethod
    def error_res(cls, mensaje: str, codigo_error: str = "ERROR_INTERNO", detalles: List[str] = None):
        """Retorna una respuesta de error."""
        return cls(
            exito=False,
            mensaje=mensaje,
            codigo_error=codigo_error,
            detalles=detalles or []
        )
