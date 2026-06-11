"""
modelos/esquemas.py  ·  v5 — FASE 1: Structured Outputs + Memoria (LUKA)
══════════════════════════════════════════════════════════════════════════════
DTOs (Data Transfer Objects) de entrada y salida para los 10 módulos de
análisis del Microservicio IA de LUKA.

Cambios v5 (FASE 1 - Migración Incremental):
  - Nuevo: ConsejoEstructurado — esquema de salida JSON para Gemini Structured Outputs.
    Usado exclusivamente por GASTO_HORMIGA en esta fase.
  - RespuestaModulo.consejo: Optional[str] → Optional[Union[str, ConsejoEstructurado]]
    para retrocompatibilidad con los 9 módulos que siguen devolviendo str plano.
  - a_dict_serializable(): blindado para serializar correctamente ambos tipos.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional, Union
from uuid import uuid4
 
from pydantic import BaseModel, Field, field_validator, model_validator


# ══════════════════════════════════════════════════════════════════════════════
# ENUMERACIONES COMPARTIDAS
# ══════════════════════════════════════════════════════════════════════════════
 
class TipoMovimiento(str, Enum):
    INGRESO = "INGRESO"
    GASTO = "GASTO"
 
 
class NivelRiesgo(str, Enum):
    BAJO = "BAJO"
    MEDIO = "MEDIO"
    ALTO = "ALTO"
    CRITICO = "CRITICO"
 
 
class TipoGrafico(str, Enum):
    LINEA = "line"
    BARRA = "bar"
    DONA = "doughnut"
    PASTEL = "pie"
    BARRA_APILADA = "bar_stacked"


class EstadoEvento(str, Enum):
    EXITOSO = "EXITOSO"
    FALLIDO = "FALLIDO"
    ADVERTENCIA = "ADVERTENCIA"
 
 
class NombreModulo(str, Enum):
    GASTO_HORMIGA = "GASTO_HORMIGA"
    PREDECIR_GASTOS = "PREDECIR_GASTOS"
    HABITOS_FINANCIEROS = "HABITOS_FINANCIEROS"
    SIMULAR_META = "SIMULAR_META"
    REPORTE_COMPLETO = "REPORTE_COMPLETO"
    RETO_AHORRO_DINAMICO = "RETO_AHORRO_DINAMICO"
    ANALISIS_ESTILO_VIDA = "ANALISIS_ESTILO_VIDA"
    AUTO_CLASIFICACION = "AUTO_CLASIFICACION"


# ══════════════════════════════════════════════════════════════════════════════
# NUEVO v5: CONSEJO ESTRUCTURADO — Esquema para Gemini Structured Outputs
# ══════════════════════════════════════════════════════════════════════════════

class ConsejoEstructurado(BaseModel):
    """
    Esquema de salida estructurada para el módulo GASTO_HORMIGA.

    Gemini usará este modelo como response_schema para garantizar que la
    respuesta sea siempre un JSON válido con estos campos exactos.
    El frontend puede renderizar cada campo de forma independiente
    (cards, listas, badges) en lugar de parsear Markdown.

    Compatibilidad:
      - Los 9 módulos restantes siguen devolviendo str plano.
      - RespuestaModulo.consejo es Union[str, ConsejoEstructurado] para soportar ambos.
      - a_dict_serializable() normaliza ambos tipos a dict antes de serializar.
    """
    pensamiento_interno_ia: str = Field(
        ...,
        description="Razonamiento lógico breve sobre los cálculos y variaciones. Mantenlo en máximo 1-2 oraciones para ahorrar tokens.",
    )
    introduccion: str = Field(
        ...,
        description="Saludo personalizado y contexto del análisis. 1-2 frases.",
    )
    analisis_ia: str = Field(
        ...,
        description="Análisis detallado de los gastos hormiga detectados: montos, categorías y patrones.",
    )
    conexion_emocional: str = Field(
        ...,
        description="Frase que conecta la fuga de dinero con la meta de ahorro personal del usuario.",
    )
    plan_accion_titulo: str = Field(
        ...,
        description="Título corto del plan de acción propuesto (ej: 'Plan Hormiga Cero').",
    )
    plan_accion_pasos: List[str] = Field(
        ...,
        min_length=2,
        max_length=5,
        description="Lista de 2 a 5 pasos concretos y accionables para reducir los gastos hormiga.",
    )
    comentario_positivo: str = Field(
        ...,
        description="Cierre motivador que refuerza el progreso o el esfuerzo del usuario.",
    )


# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE ENTRADA — Peticiones a los endpoints
# ══════════════════════════════════════════════════════════════════════════════

class PeticionBase(BaseModel):
    usuario_id: str = Field(
        default="N/A", 
        description="UUID del usuario obtenido del JWT."
    )

class SolicitudClasificacionDTO(BaseModel):
    id_temporal: str
    tipo_movimiento: str
    etiquetas: Optional[str] = ""
    descripcion: Optional[str] = ""

class RespuestaClasificacionDTO(BaseModel):
    id_temporal: str
    sugerencias: List[str]
    usando_fallback: bool = False

class FiltroDeFecha(BaseModel):
    mes: Optional[int] = Field(default=None, ge=1, le=12)
    anio: Optional[int] = Field(default=None, ge=2020, le=2100)
    dia_inicio: Optional[int] = Field(default=None, ge=1, le=31)
    mes_inicio: Optional[int] = Field(default=None, ge=1, le=12)
    anio_inicio: Optional[int] = Field(default=None, ge=2020, le=2100)
    dia_fin: Optional[int] = Field(default=None, ge=1, le=31)
    mes_fin: Optional[int] = Field(default=None, ge=1, le=12)
    anio_fin: Optional[int] = Field(default=None, ge=2020, le=2100)
 
    @model_validator(mode="after")
    def validar_coherencia_fecha(self) -> "FiltroDeFecha":
        if self.mes is not None and self.anio is None:
            raise ValueError("Si se especifica 'mes', también se debe especificar 'anio'.")
        if self.mes_inicio is not None and self.anio_inicio is None:
            raise ValueError("Si se especifica 'mes_inicio', también se debe especificar 'anio_inicio'.")
        if self.mes_fin is not None and self.anio_fin is None:
            raise ValueError("Si se especifica 'mes_fin', también se debe especificar 'anio_fin'.")
        return self

class PeticionConFiltroFecha(PeticionBase, FiltroDeFecha):
    token: str = Field(..., min_length=10)
    tamanio_pagina: int = Field(default=200, ge=10, le=1000)
    frecuencia: Optional[str] = Field(default="SEMANAL")
 
    model_config = {"str_strip_whitespace": True}
 
    @field_validator("usuario_id")
    @classmethod
    def validar_usuario_id(cls, valor: str) -> str:
        if not valor.strip():
            raise ValueError("El usuario_id no puede estar vacío.")
        return valor.strip()


class TransaccionParaClasificar(BaseModel):
    monto: float = Field(..., gt=0)
    descripcion: str = Field(..., min_length=1, max_length=500)
    categoria_actual: Optional[str] = Field(default=None)
    tipo: TipoMovimiento = Field(...)

class PeticionClasificar(PeticionBase):
    transaccion: TransaccionParaClasificar = Field(...)


class PeticionSimularMeta(PeticionBase):
    token: str = Field(..., min_length=10)
    nombre_meta: str = Field(..., min_length=1, max_length=80)
    monto_objetivo: float = Field(..., gt=0)
    monto_actual_ahorrado: float = Field(default=0.0, ge=0)
    aporte_mensual_deseado: Optional[float] = Field(default=None, gt=0)
 
    @model_validator(mode="after")
    def validar_progreso_coherente(self) -> "PeticionSimularMeta":
        if self.monto_actual_ahorrado >= self.monto_objetivo:
            raise ValueError(
                "El monto_actual_ahorrado no puede ser mayor o igual al monto_objetivo."
            )
        return self

    @field_validator("nombre_meta")
    @classmethod
    def sanitizar_nombre_meta(cls, v: str) -> str:
        texto_inferior = v.lower()
        bloqueados = ["ignora", "olvida", "instrucciones", "actúa como", "prompt", "bypass"]
        if any(b in texto_inferior for b in bloqueados):
            raise ValueError("Entrada no permitida por políticas de seguridad.")
        return v


class PeticionSimularEscenario(PeticionBase, FiltroDeFecha):
    descripcion_escenario: str = Field(..., min_length=5, max_length=80)
    monto_cambio: float = Field(...)
    tipo_cambio: TipoMovimiento = Field(...)
    recurrente: bool = Field(default=True)

    @field_validator("descripcion_escenario")
    @classmethod
    def sanitizar_descripcion_escenario(cls, v: str) -> str:
        texto_inferior = v.lower()
        bloqueados = ["ignora", "olvida", "instrucciones", "actúa como", "prompt", "bypass"]
        if any(b in texto_inferior for b in bloqueados):
            raise ValueError("Entrada no permitida por políticas de seguridad.")
        return v


# ══════════════════════════════════════════════════════════════════════════════
# DTO DE CONTEXTO PERSONAL
# ══════════════════════════════════════════════════════════════════════════════
 
class MetaAhorro(BaseModel):
    nombre: str = Field(default="Sin nombre")
    monto_objetivo: float = Field(default=0.0, ge=0)
    monto_actual: float = Field(default=0.0, ge=0)
    model_config = {"populate_by_name": True, "extra": "ignore"}
 
    @property
    def progreso_porcentaje(self) -> float:
        if self.monto_objetivo <= 0:
            return 0.0
        return round((self.monto_actual / self.monto_objetivo) * 100, 1)
 
    @property
    def monto_restante(self) -> float:
        return round(max(self.monto_objetivo - self.monto_actual, 0.0), 2)
 
 
class LimiteGasto(BaseModel):
    monto_limite: float = Field(default=0.0, ge=0, alias="montoLimite")
    activo: bool = Field(default=False)
    model_config = {"populate_by_name": True, "extra": "ignore"}
 
 
class PerfilUsuario(BaseModel):
    nombre: Optional[str] = Field(default=None)
    rol: str = Field(default="FREE")
    edad: Optional[int] = Field(default=None)
    ocupacion: Optional[str] = Field(default="Estudiante")
    carrera: Optional[str] = Field(default=None)
    universidad: Optional[str] = Field(default=None)
    ciudad: Optional[str] = Field(default=None)
    ciclo: Optional[int] = Field(default=None, ge=1, le=12)
    ingreso_mensual: float = Field(default=0.0, ge=0, alias="ingresoMensual")
    meta_ahorro_activa: Optional[MetaAhorro] = Field(default=None, alias="metaAhorroActiva")
    limite_gasto: Optional[LimiteGasto] = Field(default=None, alias="limiteGasto")
    model_config = {"populate_by_name": True, "extra": "ignore"}
 
    @classmethod
    def sin_datos(cls) -> "PerfilUsuario":
        return cls()
 
    @property
    def nombre_display(self) -> str:
        return self.nombre or "estudiante"
 
    @property
    def tiene_meta_activa(self) -> bool:
        return self.meta_ahorro_activa is not None and self.meta_ahorro_activa.monto_objetivo > 0
 
    @property
    def tiene_limite_activo(self) -> bool:
        return (
            self.limite_gasto is not None
            and self.limite_gasto.activo
            and self.limite_gasto.monto_limite > 0
        )
 
    @property
    def resumen_texto(self) -> str:
        partes = []
        if self.nombre:
            partes.append(f"Nombre: {self.nombre}")
        if self.carrera:
            partes.append(f"Carrera: {self.carrera}")
        if self.universidad:
            partes.append(f"Universidad: {self.universidad}")
        if self.ciudad:
            partes.append(f"Ciudad: {self.ciudad}")
        if self.ciclo:
            partes.append(f"Ciclo: {self.ciclo}")
        if self.ingreso_mensual > 0:
            partes.append(f"Ingreso mensual declarado: S/ {self.ingreso_mensual:,.2f}")
        if self.tiene_meta_activa:
            meta = self.meta_ahorro_activa
            partes.append(
                f"Meta activa: '{meta.nombre}' — "
                f"S/ {meta.monto_actual:,.2f} de S/ {meta.monto_objetivo:,.2f} "
                f"({meta.progreso_porcentaje}%)"
            )
        if self.tiene_limite_activo:
            partes.append(f"Límite de gasto mensual: S/ {self.limite_gasto.monto_limite:,.2f}")
        return "\n".join(f"- {p}" for p in partes) if partes else "- Perfil no disponible."
 
 
# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE SALIDA — Respuestas de los endpoints
# ══════════════════════════════════════════════════════════════════════════════
 
class PuntoGrafico(BaseModel):
    etiqueta: str = Field(...)
    valor: float = Field(...)
    color: Optional[str] = Field(default=None)
 
 
class MetadataGrafico(BaseModel):
    tipo_grafico: TipoGrafico = Field(...)
    titulo: str = Field(...)
    datos_primarios: List[PuntoGrafico] = Field(default_factory=list)
    datos_secundarios: List[PuntoGrafico] = Field(default_factory=list)
    unidad: str = Field(default="S/")
    linea_referencia: Optional[float] = Field(default=None)
 
 
class InsightAnalitico(BaseModel):
    modulo: NombreModulo = Field(...)
    total_transacciones_analizadas: int = Field(default=0, ge=0)
    total_ingresos: float = Field(default=0.0)
    total_gastos: float = Field(default=0.0)
    balance_neto: float = Field(default=0.0)
    promedio_gasto_mensual: float = Field(default=0.0)
    promedio_ingreso_mensual: float = Field(default=0.0)
    hallazgos: Dict[str, float | str | int | bool | list] = Field(default_factory=dict)
    nivel_alerta: NivelRiesgo = Field(default=NivelRiesgo.BAJO)
    periodo_analizado: str = Field(default="")


class KpiWidget(BaseModel):
    valor: float = Field(...)
    etiqueta: str = Field(...)
    unidad: str = Field(default="")
    tendencia: Optional[str] = Field(default=None)
 
 
class EstadoCoach(str, Enum):
    EXITOSO = "EXITOSO"
    CUOTA_AGOTADA = "CUOTA_AGOTADA"
    AUTH_ERROR = "AUTH_ERROR"
    TIMEOUT = "TIMEOUT"
    NO_DISPONIBLE = "NO_DISPONIBLE"
 
 
class RespuestaModulo(BaseModel):
    """
    Respuesta unificada para los 10 endpoints de análisis.

    v5 — Cambio en campo `consejo`:
      - Antes: Optional[str]
      - Ahora:  Optional[Union[str, ConsejoEstructurado]]

    Retrocompatibilidad garantizada:
      - Los 9 módulos que devuelven str siguen funcionando sin cambios.
      - GASTO_HORMIGA devuelve ConsejoEstructurado cuando Gemini responde con éxito.
      - El fallback de MotorReglasLocal sigue devolviendo str para todos los módulos.
      - a_dict_serializable() normaliza ambos tipos para RabbitMQ/Outbox.
    """
    id_respuesta: str = Field(default_factory=lambda: str(uuid4()))
    usuario_id: str
    modulo: NombreModulo
    fecha_generacion: datetime = Field(default_factory=datetime.now)
 
    # ── CAMBIO v5: Union en lugar de solo str ─────────────────────────────────
    consejo: Optional[Union[str, ConsejoEstructurado]] = Field(
        default=None,
        description=(
            "Consejo financiero. "
            "str para los 9 módulos legacy. "
            "ConsejoEstructurado para GASTO_HORMIGA (Structured Outputs)."
        ),
    )
    estado_coach: EstadoCoach = Field(default=EstadoCoach.EXITOSO)
    usando_fallback: bool = Field(default=False)
 
    insight: InsightAnalitico = Field(...)
 
    grafico: Optional[MetadataGrafico] = Field(default=None)
    kpi: Optional[KpiWidget] = Field(default=None)
 
    def a_dict_serializable(self) -> dict:
        """
        Serialización segura para JSON/RabbitMQ.

        Normaliza el campo `consejo` según su tipo real:
          - str              → se mantiene como str (módulos legacy, fallbacks).
          - ConsejoEstructurado → se convierte a dict via model_dump().
          - None             → permanece None.

        Las fechas se convierten a ISO-8601.
        """
        datos = self.model_dump()

        # Normalizar consejo: Pydantic model_dump() ya convierte BaseModel a dict,
        # pero lo hacemos explícito para mayor claridad y control de errores.
        if isinstance(self.consejo, ConsejoEstructurado):
            datos["consejo"] = self.consejo.model_dump()
        # Si es str o None, model_dump() ya lo maneja correctamente.

        datos["fecha_generacion"] = self.fecha_generacion.isoformat()
        return datos


# ══════════════════════════════════════════════════════════════════════════════
# DTOs AUXILIARES
# ══════════════════════════════════════════════════════════════════════════════
 
class ResumenMensual(BaseModel):
    anio: int = Field(..., ge=2020, le=2100)
    mes: int = Field(..., ge=1, le=12)
    total_ingresos: float = Field(default=0.0, ge=0)
    total_gastos: float = Field(default=0.0, ge=0)
    gastos_por_categoria: Dict[str, float] = Field(default_factory=dict)
    cantidad_transacciones: int = Field(default=0, ge=0)
 
    @property
    def balance(self) -> float:
        return round(self.total_ingresos - self.total_gastos, 2)
 
    @property
    def periodo_label(self) -> str:
        return f"{self.anio}-{self.mes:02d}"
 
    @property
    def porcentaje_ahorro(self) -> float:
        if self.total_ingresos <= 0:
            return 0.0
        return round(((self.total_ingresos - self.total_gastos) / self.total_ingresos) * 100, 1)


# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE EVENTOS
# ══════════════════════════════════════════════════════════════════════════════

class EventoAuditoriaDTO(BaseModel):
    usuario_id: Optional[str] = Field(default=None, alias="usuarioId")
    accion: str
    modulo: str
    ip_origen: Optional[str] = Field(default=None, alias="ipOrigen", max_length=45)
    detalles: Optional[str] = None
    fecha: datetime = Field(default_factory=datetime.now)
    model_config = {"populate_by_name": True}


class EventoAccesoDTO(BaseModel):
    usuario_id: Optional[str] = Field(default=None, alias="usuarioId")
    ip_origen: str = Field(..., alias="ipOrigen", max_length=45)
    navegador: Optional[str] = Field(default="LUKA-IA-SERVICE", max_length=500)
    estado: EstadoEvento
    detalle_error: Optional[str] = Field(default=None, alias="detalleError", max_length=500)
    fecha: datetime = Field(default_factory=datetime.now)
    correlation_id: Optional[str] = Field(default=None, alias="correlationId")
    model_config = {"populate_by_name": True}


class EventoTransaccionalDTO(BaseModel):
    usuario_id: str = Field(..., alias="usuarioId")
    entidad_id: str = Field(..., alias="entidadId")
    servicio_origen: str = Field(default="ms-ia", alias="servicioOrigen")
    entidad_afectada: str = Field(..., alias="entidadAfectada")
    descripcion: str
    valor_anterior: Optional[str] = Field(default=None, alias="valorAnterior")
    valor_nuevo: Optional[str] = Field(default=None, alias="valorNuevo")
    fecha: datetime = Field(default_factory=datetime.now)
    model_config = {"populate_by_name": True}
