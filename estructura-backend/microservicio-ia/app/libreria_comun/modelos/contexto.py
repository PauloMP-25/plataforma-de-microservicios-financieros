"""
libreria_comun/modelos/contexto.py  ·  v2.0 — LUKA-COACH V4
══════════════════════════════════════════════════════════════════════════════
Espejo del ContextoEstrategicoIADTO.java (record, libreria-comun).

Principio de Menor Privilegio:
  Solo los campos estrictamente necesarios para que la IA genere
  un consejo personalizado. Sin IDs internos, sin datos sensibles.

Nota sobre alias:
  - La mayoría de campos usa alias_generator=to_camel (automático).
  - `tono_ia` usa alias EXPLÍCITO "tonoIA" porque Pydantic to_camel
    lo convertiría a "tonoIa" (minúscula final), mientras que el record
    Java lo envía como "tonoIA" (ambas mayúsculas). Sin este alias el
    campo no se deserializa al recibir JSON de ms-cliente.
══════════════════════════════════════════════════════════════════════════════
"""

from decimal import Decimal

from pydantic import BaseModel, Field, ConfigDict, field_validator
from pydantic.alias_generators import to_camel


class ContextoEstrategicoIADTO(BaseModel):
    """
    DTO optimizado diseñado específicamente para el microservicio-ia.
    Espejo funcional de ContextoEstrategicoIADTO.java (record).

    Acepta tanto camelCase (JSON del ms-cliente Java) como snake_case
    (uso interno Python), gracias a populate_by_name=True.
    """

    model_config = ConfigDict(
        # Genera alias camelCase automáticamente (ej: ingreso_mensual → ingresoMensual)
        alias_generator=to_camel,
        # Permite usar tanto el alias (camelCase) como el nombre original (snake_case)
        populate_by_name=True,
        # Permite construir desde ORM o dicts con atributos
        from_attributes=True,
        # Tolerante a campos extra que lleguen de Java (ignora sin error)
        extra="ignore",
    )

    # ── Identidad Personal ────────────────────────────────────────────────────
    nombres: str = Field(
        ...,
        min_length=1,
        max_length=150,
        description="Nombre del cliente para personalizar la conversación.",
        examples=["Paulo"],
    )

    ocupacion: str = Field(
        ...,
        min_length=1,
        max_length=100,
        description="Ocupación del cliente (ej: 'Estudiante de Sistemas', 'Freelancer').",
        examples=["Estudiante de Ingeniería de Sistemas"],
    )

    # ── Datos Financieros ─────────────────────────────────────────────────────
    ingreso_mensual: Decimal = Field(
        ...,
        ge=Decimal("0"),
        description="Ingreso mensual declarado (S/). "
                    "Usado para calcular el umbral de 'gasto hormiga' (1% del ingreso).",
        examples=[Decimal("1500.00")],
    )

    # ── Preferencias de Interacción ───────────────────────────────────────────
    # Alias EXPLÍCITO porque Pydantic to_camel convierte tono_ia → "tonoIa"
    # pero el record Java envía "tonoIA" (ambas letras finales en mayúscula).
    tono_ia: str = Field(
        ...,
        alias="tonoIA",
        description="Tono conversacional configurado por el usuario. "
                    "Valores: FORMAL, AMIGABLE, MOTIVADOR, DIRECTO, CRITICO, SERIO, INTELIGENTE, MEJOR AMIGO.",
        examples=["MOTIVADOR"],
    )

    # ── Contexto de Meta de Ahorro ────────────────────────────────────────────
    porcentaje_meta_principal: Decimal = Field(
        ...,
        ge=Decimal("0"),
        le=Decimal("100"),
        description="Progreso porcentual de la meta de ahorro más cercana a cumplirse.",
        examples=[Decimal("35.50")],
    )

    nombre_meta_principal: str = Field(
        default="Ninguna",
        min_length=0,
        max_length=200,
        description="Nombre de la meta de ahorro principal activa.",
        examples=["Laptop para estudios"],
    )

    # ── Configuración de Alertas ──────────────────────────────────────────────
    porcentaje_alerta_gasto: int = Field(
        ...,
        ge=1,
        le=100,
        description="Umbral porcentual del límite de gasto para emitir advertencias.",
        examples=[80],
    )

    # ── Validadores ───────────────────────────────────────────────────────────

    @field_validator("tono_ia")
    @classmethod
    def validar_tono(cls, valor: str) -> str:
        """Normaliza y valida el tono IA contra los valores permitidos en Java."""
        tonos_validos = {"FORMAL", "AMIGABLE", "MOTIVADOR", "DIRECTO"}
        normalizado = valor.upper().strip()
        if normalizado not in tonos_validos:
            raise ValueError(
                f"Tono inválido: '{valor}'. Valores permitidos: {tonos_validos}"
            )
        return normalizado

    # ── Propiedades de Conveniencia ───────────────────────────────────────────

    @property
    def umbral_gasto_hormiga(self) -> Decimal:
        """
        Calcula el umbral monetario para clasificar un gasto como 'hormiga'.
        Regla de negocio LUKA: < 1% del ingreso mensual.
        """
        if self.ingreso_mensual <= 0:
            return Decimal("0")
        return (self.ingreso_mensual * Decimal("0.01")).quantize(Decimal("0.01"))

    @property
    def primer_nombre(self) -> str:
        """Extrae el primer nombre para saludos informales en el prompt."""
        return self.nombres.split()[0] if self.nombres else "estudiante"

    @property
    def resumen_para_prompt(self) -> str:
        """
        Genera el bloque de contexto personal que se inyecta en el prompt de Gemini.
        Centralizado aquí para que todos los módulos lo usen igual.
        """
        lineas = [
            f"- Nombre: {self.primer_nombre}",
            f"- Ocupación: {self.ocupacion}",
            f"- Ingreso mensual: S/ {self.ingreso_mensual:,.2f}",
            f"- Tono preferido: {self.tono_ia}",
            f"- Meta activa: '{self.nombre_meta_principal}' "
            f"(progreso: {self.porcentaje_meta_principal}%)",
        ]
        return "\n".join(lineas)

    def __repr__(self) -> str:
        return (
            f"ContextoEstrategicoIADTO("
            f"nombres={self.nombres!r}, "
            f"tono={self.tono_ia!r}, "
            f"ingreso=S/{self.ingreso_mensual})"
        )