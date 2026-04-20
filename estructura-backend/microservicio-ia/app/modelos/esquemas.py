"""
modelos/esquemas.py — Modelos de entrada/salida de la API (Pydantic v2).
Todos los atributos están en Español siguiendo el estándar del proyecto.
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
from enum import Enum
import uuid


# ══════════════════════════════════════════════════════════════
# ENUMERACIONES
# ══════════════════════════════════════════════════════════════

class TipoMovimiento(str, Enum):
    INGRESO = "INGRESO"
    GASTO = "GASTO"


class MetodoPago(str, Enum):
    EFECTIVO = "EFECTIVO"
    TARJETA = "TARJETA"
    TRANSFERENCIA = "TRANSFERENCIA"
    DIGITAL = "DIGITAL"


# ══════════════════════════════════════════════════════════════
# MODELOS DE DATOS FINANCIEROS (espejo del microservicio Java)
# ══════════════════════════════════════════════════════════════

class TransaccionDTO(BaseModel):
    id: Optional[str] = None
    usuario_id: Optional[str] = Field(None, alias="usuarioId")
    nombre_cliente: Optional[str] = Field(None, alias="nombreCliente")
    monto: float = 0.0
    tipo: TipoMovimiento
    categoria_id: Optional[str] = Field(None, alias="categoriaId")
    categoria_nombre: Optional[str] = Field(None, alias="categoriaNombre")
    categoria_icono: Optional[str] = Field(None, alias="categoriaIcono")
    fecha_transaccion: Optional[datetime] = Field(None, alias="fechaTransaccion")
    metodo_pago: Optional[MetodoPago] = Field(None, alias="metodoPago")
    etiquetas: Optional[str] = None
    notas: Optional[str] = None
    fecha_registro: Optional[datetime] = Field(None, alias="fechaRegistro")

    model_config = {"populate_by_name": True}


class PaginaTransacciones(BaseModel):
    content: List[TransaccionDTO] = []
    total_elements: int = Field(0, alias="totalElements")
    total_pages: int = Field(0, alias="totalPages")
    number: int = 0
    size: int = 20

    model_config = {"populate_by_name": True}


# ══════════════════════════════════════════════════════════════
# MODELOS DE SOLICITUD A LA IA
# ══════════════════════════════════════════════════════════════

class SolicitudAnalisis(BaseModel):
    usuario_id: str = Field(..., description="UUID del usuario a analizar")
    mes: Optional[int] = Field(None, description="Mes para el análisis (1-12)")
    anio: Optional[int] = Field(None, description="Año para el análisis (ej: 2025)")
    tamanio_pagina: int = Field(200, description="Máximo de transacciones a cargar", ge=10, le=500)

    model_config = {
        "json_schema_extra": {
            "example": {
                "usuario_id": "550e8400-e29b-41d4-a716-446655440000",
                "mes": 4,
                "anio": 2026,
                "tamanio_pagina": 200
            }
        }
    }


class SolicitudSimulacion(BaseModel):
    usuario_id: str = Field(..., description="UUID del usuario")
    nuevo_gasto_fijo: float = Field(0.0, description="Nuevo gasto fijo mensual a simular", ge=0)
    nuevo_ingreso: float = Field(0.0, description="Nuevo ingreso mensual a simular", ge=0)
    mes: Optional[int] = None
    anio: Optional[int] = None

    model_config = {
        "json_schema_extra": {
            "example": {
                "usuario_id": "550e8400-e29b-41d4-a716-446655440000",
                "nuevo_gasto_fijo": 150.0,
                "nuevo_ingreso": 500.0,
                "mes": 4,
                "anio": 2026
            }
        }
    }


class SolicitudMetaFinanciera(BaseModel):
    usuario_id: str = Field(..., description="UUID del usuario")
    monto_meta: float = Field(..., description="Monto total de la meta financiera", gt=0)
    nombre_meta: str = Field(..., description="Nombre descriptivo de la meta", min_length=3)
    mes: Optional[int] = None
    anio: Optional[int] = None

    model_config = {
        "json_schema_extra": {
            "example": {
                "usuario_id": "550e8400-e29b-41d4-a716-446655440000",
                "monto_meta": 5000.0,
                "nombre_meta": "Fondo de emergencia",
                "mes": 4,
                "anio": 2026
            }
        }
    }


# ══════════════════════════════════════════════════════════════
# MODELOS DE RESPUESTA DE LA IA
# ══════════════════════════════════════════════════════════════

class RespuestaClasificacion(BaseModel):
    total_transacciones: int
    transacciones_clasificadas: int
    categorias_detectadas: Dict[str, int]
    etiquetas_asignadas: List[Dict[str, Any]]
    precision_estimada: float
    mensaje: str


class RespuestaPrediccion(BaseModel):
    usuario_id: str
    mes_predicho: str
    gasto_predicho: float
    ingreso_predicho: float
    balance_predicho: float
    metodo_utilizado: str
    confianza: str
    historico_meses: List[Dict[str, Any]]
    mensaje: str


class RespuestaAnomalias(BaseModel):
    usuario_id: str
    total_transacciones_analizadas: int
    anomalias_detectadas: int
    umbral_utilizado: float
    detalles_anomalias: List[Dict[str, Any]]
    monto_en_riesgo: float
    mensaje: str


class RespuestaSuscripciones(BaseModel):
    usuario_id: str
    total_gastos_recurrentes: int
    monto_total_mensual: float
    suscripciones_detectadas: List[Dict[str, Any]]
    ahorro_potencial: float
    recomendacion: str


class RespuestaCapacidadAhorro(BaseModel):
    usuario_id: str
    total_ingresos: float
    total_gastos: float
    gastos_fijos: float
    margen_bruto: float
    capacidad_ahorro: float
    porcentaje_ahorro: float
    factor_seguridad_aplicado: float
    clasificacion: str
    recomendacion: str


class RespuestaMetaFinanciera(BaseModel):
    usuario_id: str
    nombre_meta: str
    monto_meta: float
    capacidad_ahorro_mensual: float
    meses_para_alcanzar: int
    fecha_estimada: str
    escenario_optimista_meses: int
    escenario_pesimista_meses: int
    mensaje: str


class RespuestaEstacionalidad(BaseModel):
    usuario_id: str
    mes_mayor_gasto: str
    mes_mayor_ingreso: str
    mes_menor_gasto: str
    picos_detectados: List[Dict[str, Any]]
    patron_detectado: str
    distribucion_mensual: List[Dict[str, Any]]


class RespuestaPresupuesto(BaseModel):
    usuario_id: str
    gasto_semana_anterior: float
    presupuesto_semana_actual: float
    limite_diario_recomendado: float
    categorias_con_limite: List[Dict[str, Any]]
    advertencias: List[str]
    mensaje: str


class RespuestaSimulacion(BaseModel):
    usuario_id: str
    escenario_actual: Dict[str, float]
    escenario_simulado: Dict[str, float]
    impacto: Dict[str, Any]
    viabilidad: str
    recomendacion: str


class RespuestaReporte(BaseModel):
    usuario_id: str
    fecha_generacion: str
    periodo_analizado: str
    resumen_ejecutivo: str
    kpis: Dict[str, Any]
    alertas: List[str]
    recomendaciones: List[str]
    puntaje_salud_financiera: float
    clasificacion_salud: str


class RespuestaAnalisisCompleto(BaseModel):
    usuario_id: str
    fecha_analisis: str
    total_transacciones: int
    prediccion: Optional[RespuestaPrediccion] = None
    anomalias: Optional[RespuestaAnomalias] = None
    capacidad_ahorro: Optional[RespuestaCapacidadAhorro] = None
    suscripciones: Optional[RespuestaSuscripciones] = None
    reporte: Optional[RespuestaReporte] = None
    mensaje: str


# ══════════════════════════════════════════════════════════════
# MODELO DE ERROR ESTÁNDAR
# ══════════════════════════════════════════════════════════════

class ErrorRespuesta(BaseModel):
    estado: int
    error: str
    mensaje: str
    ruta: str
    fecha_hora: str