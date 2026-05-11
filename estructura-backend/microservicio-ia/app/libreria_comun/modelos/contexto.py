from decimal import Decimal
from pydantic import BaseModel, Field, ConfigDict
from pydantic.alias_generators import to_camel

class ContextoEstrategicoIADTO(BaseModel):
    """
    DTO optimizado diseñado específicamente para el microservicio-ia.
    Espejo funcional de ContextoEstrategicoIADTO.java.
    """
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        from_attributes=True
    )

    nombres: str = Field(..., description="Nombre del cliente para personalizar la conversación.")
    ocupacion: str = Field(..., description="Ocupación del cliente.")
    ingreso_mensual: Decimal = Field(..., description="Nivel de ingresos para ajustar las recomendaciones.")
    tono_ia: str = Field(..., description="Tono conversacional (ej: formal, amigable, motivador).")
    porcentaje_meta_principal: Decimal = Field(..., description="Progreso de la meta de ahorro.")
    nombre_meta_principal: str = Field(..., description="Nombre de la meta de ahorro.")
    porcentaje_alerta_gasto: int = Field(..., description="Umbral porcentual del límite de gasto.")
