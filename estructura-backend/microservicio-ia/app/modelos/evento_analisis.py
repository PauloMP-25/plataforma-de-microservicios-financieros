"""
modelos/evento_analisis.py
══════════════════════════════════════════════════════════════════════════════
Modelos Pydantic para deserializar el EventoAnalisisIA que llega desde
RabbitMQ (exchange.ia → cola.ia.procesamiento) tras cada transacción
registrada por el microservicio-nucleo-financiero.

Diseñado con valores por defecto en todos los campos opcionales para
tolerar contextos incompletos o nulos sin lanzar ValidationError.

ENTRADA (desde RabbitMQ):
  EventoAnalisisIA
    ├── tipo_solicitud: TipoSolicitud   ← NUEVO: discrimina automático vs manual
    ├── transaccion: TransaccionEvento  (puede ser None en consultas manuales)
    ├── historial_mensual: List[ResumenMes]  ← NUEVO: últimos 6 meses
    └── contexto: ContextoUsuario
 
SALIDA (hacia Dashboard / persistencia):
  ResultadoAnalisisIA
    ├── id_usuario
    ├── consejo_texto
    ├── tipo_modulo: TipoModulo
    ├── fecha_generacion
    └── metadata_grafico: Optional[MetadataGrafico]  ← datos para charts
 
Principios:
  - Todos los campos opcionales tienen defaults seguros.
  - ResumenMes expone propiedades calculadas (balance, variación).
  - TipoSolicitud permite al consumidor elegir la cola de respuesta correcta.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

# 3. Imports estándar de Python
import json
from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional
from uuid import uuid4

# 4. Imports de librerías externas (Pydantic)
from pydantic import BaseModel, Field, field_validator, model_validator

from app.excepciones import ContratoDatosError


# ══════════════════════════════════════════════════════════════════════════════
# ENUMERACIONES
# ══════════════════════════════════════════════════════════════════════════════
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

class TipoSolicitud(str, Enum):
    """
    Discrimina el origen del evento:
      TRANSACCION_RECIENTE → disparado automáticamente tras registrar un gasto/ingreso.
      CONSULTA_MODULO      → disparado manualmente desde un botón del Dashboard web.
    El ConsumidorIA usará este campo para elegir la cola de respuesta correcta.
    """
    TRANSACCION_RECIENTE = "TRANSACCION_RECIENTE"
    CONSULTA_MODULO      = "CONSULTA_MODULO"


class TipoModulo(str, Enum):
    """
    Identifica qué módulo de análisis generó el resultado.
    Usado en ResultadoAnalisisIA.tipo_modulo para que el Dashboard
    sepa qué widget/gráfico renderizar.
    """
    TRANSACCION_AUTOMATICA  = "TRANSACCION_AUTOMATICA"   # consejo inmediato
    PREDICCION_GASTOS       = "PREDICCION_GASTOS"
    GASTO_HORMIGA           = "GASTO_HORMIGA"
    AUTOCLASIFICACION       = "AUTOCLASIFICACION"
    COMPARACION_MENSUAL     = "COMPARACION_MENSUAL"
    CAPACIDAD_AHORRO        = "CAPACIDAD_AHORRO"
    METAS_FINANCIERAS       = "METAS_FINANCIERAS"
    ANOMALIAS               = "ANOMALIAS"
    ESTACIONALIDAD          = "ESTACIONALIDAD"
    PRESUPUESTO_DINAMICO    = "PRESUPUESTO_DINAMICO"
    REPORTE_COMPLETO        = "REPORTE_COMPLETO"

# ══════════════════════════════════════════════════════════════════════════════
# MODELOS DE ENTRADA — Submodelos
# ══════════════════════════════════════════════════════════════════════════════
class TransaccionEvento(BaseModel):
    """Datos mínimos de la transacción que disparó el análisis."""
    monto:       float  = Field(..., gt=0,  description="Monto de la transacción")
    descripcion: str    = Field(..., min_length=1, description="Descripción del comercio o concepto")
    categoria:   str    = Field(..., min_length=1, description="Categoría (ej: Entretenimiento)")
    tipo:        TipoMovimiento = Field(..., description="GASTO o INGRESO")

class ResumenMes(BaseModel):
    """
    Totales consolidados de un mes calendario.
    Enviados por el nucleo-financiero como contexto histórico.
    """
    anio:           int   = Field(..., ge=2020, le=2100)
    mes:            int   = Field(..., ge=1, le=12)
    total_ingresos: float = Field(default=0.0, alias="totalIngresos", ge=0)
    total_gastos:   float = Field(default=0.0, alias="totalGastos",   ge=0)
 
    # Desglose de gastos por categoría (clave: nombre categoría, valor: monto)
    gastos_por_categoria: Dict[str, float] = Field(
        default_factory=dict, alias="gastosPorCategoria"
    )
 
    model_config = {"populate_by_name": True}
 
    @property
    def balance(self) -> float:
        return round(self.total_ingresos - self.total_gastos, 2)
 
    @property
    def periodo_label(self) -> str:
        """Etiqueta legible: '2026-03'"""
        return f"{self.anio}-{self.mes:02d}"
 
    def variacion_gastos_vs(self, otro: "ResumenMes") -> float:
        """
        % de variación de gastos comparado con otro mes.
        Positivo = gastó más. Negativo = gastó menos.
        """
        if otro.total_gastos == 0:
            return 0.0
        return round(((self.total_gastos - otro.total_gastos) / otro.total_gastos) * 100, 1)
 
    def categoria_con_mayor_exceso_vs(self, otro: "ResumenMes") -> Optional[str]:
        """Retorna la categoría con mayor incremento de gasto respecto al mes anterior."""
        excesos: Dict[str, float] = {}
        for cat, monto in self.gastos_por_categoria.items():
            monto_anterior = otro.gastos_por_categoria.get(cat, 0.0)
            if monto > monto_anterior:
                excesos[cat] = monto - monto_anterior
        return max(excesos, key=lambda k: excesos[k]) if excesos else None

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
    Contrato de entrada desde cola.ia.procesamiento (v3).
 
    Campos nuevos respecto a v2:
      - id_usuario:         identifica al usuario (necesario para persistir el resultado)
      - tipo_solicitud:     TRANSACCION_RECIENTE | CONSULTA_MODULO
      - modulo_solicitado:  solo en CONSULTA_MODULO, indica qué TipoModulo ejecutar
      - historial_mensual:  últimos N meses con totales y desglose por categoría
      - transaccion:        obligatorio en TRANSACCION_RECIENTE, opcional en CONSULTA_MODULO
    """
    id_usuario:        str                     = Field(..., description="UUID del usuario")
    tipo_solicitud:    TipoSolicitud            = Field(default=TipoSolicitud.TRANSACCION_RECIENTE)
    modulo_solicitado: Optional[TipoModulo]     = Field(default=None)
    transaccion:       Optional[TransaccionEvento] = Field(default=None)
    historial_mensual: List[ResumenMes]         = Field(default_factory=list)
    contexto:          ContextoUsuario          = Field(default_factory=ContextoUsuario)

        
    @field_validator("transaccion", mode="after")
    @classmethod
    def validar_transaccion(cls, v, info):
        """
         Valida que si el tipo de solicitud es TRANSACCION_RECIENTE, la transacción no sea None.
         Para CONSULTA_MODULO, permite que transacción sea None (análisis basado solo en historial).
         Esta validación cruzada se hace en model_validator; aquí solo normalizamos el campo.
        En TRANSACCION_RECIENTE la transacción es obligatoria.
        En CONSULTA_MODULO puede omitirse (análisis sobre historial puro).
        """
        # La validación cruzada se hace en model_validator; aquí solo pasamos
        return v
    
    @property
    def tiene_historial(self) -> bool:
        return len(self.historial_mensual) > 0
    
    @property
    def mes_actual(self) -> Optional[ResumenMes]:
        """El mes más reciente del historial."""
        if not self.tiene_historial:
            return None
        return max(self.historial_mensual, key=lambda m: (m.anio, m.mes))
        
    @property
    def mes_anterior(self) -> Optional[ResumenMes]:
        """El segundo mes más reciente del historial."""
        if len(self.historial_mensual) < 2:
            return None
        ordenados = sorted(self.historial_mensual, key=lambda m: (m.anio, m.mes), reverse=True)
        return ordenados[1]
    
    @classmethod
    def desde_json(cls, raw: str | bytes | dict) -> "EventoAnalisisIA":
        try:
            if isinstance(raw, (str, bytes)):
                raw = json.loads(raw)
            return cls.model_validate(raw)
        except Exception as exc:
            # RASTREABILIDAD: Lanzamos la excepción que definimos ayer
            raise ContratoDatosError("Fallo al validar el contrato de entrada IA v3", detalles=str(exc))
        
# ══════════════════════════════════════════════════════════════════════════════
# MODELOS DE SALIDA — Persistencia y Dashboard
# ══════════════════════════════════════════════════════════════════════════════
 
class PuntoGrafico(BaseModel):
    """Un punto de dato para renderizar en un gráfico del Dashboard."""
    etiqueta: str    # Ej: "Ene 2026", "Alimentación"
    valor:    float
    color:    Optional[str] = None   # hex color opcional para el frontend
 
 
class MetadataGrafico(BaseModel):
    """
    Datos estructurados que el Dashboard usa para renderizar charts.
    Cada módulo decide qué tipo de gráfico y qué datos proveer.
    """
    tipo_grafico:  str                  # "line", "bar", "pie", "doughnut"
    titulo:        str
    datos:         List[PuntoGrafico]   = Field(default_factory=list)
    datos_aux:     List[PuntoGrafico]   = Field(default_factory=list)   # segunda serie
    unidad:        str                  = "S/"
    meta_linea:    Optional[float]      = None    # línea de referencia en el gráfico
 
class ResultadoAnalisisIA(BaseModel):
    """
    Contrato de salida persistido en la BD y enviado al Dashboard (v3).
 
    Publicado en:
      - cola.dashboard.consejos  (si tipo_solicitud == TRANSACCION_RECIENTE)
      - cola.dashboard.modulos   (si tipo_solicitud == CONSULTA_MODULO)
    """
    id:               str         = Field(default_factory=lambda: str(uuid4()))
    id_usuario:       str
    consejo_texto:    str
    tipo_modulo:      TipoModulo
    fecha_generacion: datetime    = Field(default_factory=datetime.now)
    metadata_grafico: Optional[MetadataGrafico] = None
 
    # Resumen numérico para el header del widget del Dashboard
    kpi_principal:    Optional[float] = None   # valor destacado (ej: % variación)
    kpi_label:        Optional[str]   = None   # etiqueta del KPI (ej: "Ahorro vs mes anterior")
 
    def a_dict_serializable(self) -> dict:
        """Serialización segura para publicar en RabbitMQ (fechas como ISO string)."""
        d = self.model_dump()
        d["fecha_generacion"] = self.fecha_generacion.isoformat()
        return d
 