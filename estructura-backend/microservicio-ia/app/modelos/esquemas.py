"""
modelos/esquemas.py  ·  v4 — IA Centrada en Datos (LUKA)
══════════════════════════════════════════════════════════════════════════════
DTOs (Data Transfer Objects) de entrada y salida para los 10 módulos de
análisis del Microservicio IA de LUKA.
 
Principios de diseño:
  - ENTRADA: Cada módulo recibe `usuario_id`, `token` JWT y filtros opcionales.
    No recibe el historial crudo: eso lo obtiene el servicio internamente.
  - SALIDA: Separa claramente los datos calculados por el motor analítico
    del consejo generado por Gemini (el "coach").
  - Tipado fuerte: sin `Any` sueltos, todos los campos con tipo explícito.
  - Valores por defecto seguros en todos los campos opcionales.
 
Estructura:
  ┌─ Entrada ──────────────────────────────────────────────────────────────┐
  │  PeticionBase            → campos comunes a todos los módulos          │
  │  PeticionClasificar      → módulo 1 (incluye datos de la transacción)  │
  │  PeticionConFiltroFecha  → módulos 2-10 (filtros mes/año)              │
  │  PeticionSimularMeta     → módulo 6 (parámetros de la meta)            │
  │  PeticionSimularEscenario → módulo 9 (gasto/ingreso hipotético)        │
  └────────────────────────────────────────────────────────────────────────┘
  ┌─ Salida ───────────────────────────────────────────────────────────────┐
  │  InsightAnalitico        → resumen técnico del motor (va a Gemini)     │
  │  PuntoGrafico            → un dato para un chart del Dashboard          │
  │  MetadataGrafico         → conjunto de puntos + tipo de gráfico        │
  │  RespuestaModulo         → respuesta unificada de los 10 endpoints     │
  └────────────────────────────────────────────────────────────────────────┘
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional
from uuid import uuid4
 
from pydantic import BaseModel, Field, field_validator, model_validator


# ══════════════════════════════════════════════════════════════════════════════
# ENUMERACIONES COMPARTIDAS
# ══════════════════════════════════════════════════════════════════════════════

# ══════════════════════════════════════════════════════════════════════════════
# ENUMERACIONES COMPARTIDAS
# ══════════════════════════════════════════════════════════════════════════════
 
class TipoMovimiento(str, Enum):
    """Discrimina si una transacción es un ingreso o un gasto."""
    INGRESO = "INGRESO"
    GASTO = "GASTO"
 
 
class NivelRiesgo(str, Enum):
    """Clasificación de riesgo financiero para anomalías y consejos."""
    BAJO = "BAJO"
    MEDIO = "MEDIO"
    ALTO = "ALTO"
    CRITICO = "CRITICO"
 
 
class TipoGrafico(str, Enum):
    """Tipos de gráfico soportados por el Dashboard de LUKA."""
    LINEA = "line"
    BARRA = "bar"
    DONA = "doughnut"
    PASTEL = "pie"
    BARRA_APILADA = "bar_stacked"


class EstadoEvento(str, Enum):
    """Resultado de una operación para auditoría. Espejo de EstadoEvento.java."""
    EXITOSO = "EXITOSO"
    FALLIDO = "FALLIDO"
    ADVERTENCIA = "ADVERTENCIA"
 
 
class NombreModulo(str, Enum):
    """
    Identifica el módulo de análisis.
    Usado para auditoría, logging y routing en el Dashboard.
    """
    CLASIFICAR = "CLASIFICAR"
    PREDECIR_GASTOS = "PREDECIR_GASTOS"
    DETECTAR_ANOMALIAS = "DETECTAR_ANOMALIAS"
    HABITOS_SEMANA = "HABITOS_SEMANA"
    OPTIMIZAR_SUSCRIPCIONES = "OPTIMIZAR_SUSCRIPCIONES"
    CAPACIDAD_AHORRO = "CAPACIDAD_AHORRO"
    SIMULAR_META = "SIMULAR_META"
    ESTACIONALIDAD = "ESTACIONALIDAD"
    PRESUPUESTO_DINAMICO = "PRESUPUESTO_DINAMICO"
    SIMULAR_ESCENARIO = "SIMULAR_ESCENARIO"
    REPORTE_COMPLETO = "REPORTE_COMPLETO"


# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE ENTRADA — Peticiones a los endpoints
# ══════════════════════════════════════════════════════════════════════════════

class PeticionBase(BaseModel):
    """
    Campos comunes a TODAS las peticiones de análisis.
    Todos los endpoints heredan de esta clase.
    """
    usuario_id: str = Field(
        ...,
        min_length=1,
        description="UUID o identificador único del usuario universitario.",
        examples=["550e8400-e29b-41d4-a716-446655440000"],
    )
    token: str = Field(
        ...,
        min_length=10,
        description="JWT Bearer token emitido por microservicio-usuario.",
    )
    tamanio_pagina: int = Field(
        default=200,
        ge=10,
        le=1000,
        description="Número máximo de transacciones a recuperar.",
    )
 
    model_config = {"str_strip_whitespace": True}
 
    @field_validator("usuario_id")
    @classmethod
    def validar_usuario_id(cls, valor: str) -> str:
        """Evita IDs vacíos después de limpiar espacios."""
        if not valor.strip():
            raise ValueError("El usuario_id no puede estar vacío.")
        return valor.strip()


class FiltroDeFecha(BaseModel):
    """
    Mixin de filtros temporales opcionales.
    Reutilizado en los módulos que necesitan acotar el período de análisis.
    """
    mes: Optional[int] = Field(
        default=None,
        ge=1,
        le=12,
        description="Mes de análisis (1=enero … 12=diciembre). None = todos.",
    )
    anio: Optional[int] = Field(
        default=None,
        ge=2020,
        le=2100,
        description="Año de análisis. None = todos.",
    )
 
    @model_validator(mode="after")
    def validar_coherencia_fecha(self) -> "FiltroDeFecha":
        """Si se proporciona mes, se debe proporcionar también el año."""
        if self.mes is not None and self.anio is None:
            raise ValueError("Si se especifica 'mes', también se debe especificar 'anio'.")
        return self


# ── Módulo 1: Autoclasificación ───────────────────────────────────────────────

class TransaccionParaClasificar(BaseModel):
    """Datos mínimos de una transacción para el módulo de clasificación."""
    monto: float = Field(..., gt=0, description="Monto de la transacción (debe ser > 0).")
    descripcion: str = Field(
        ...,
        min_length=1,
        max_length=500,
        description="Descripción del comercio o concepto (ej: 'Netflix', 'Supermercado Wong').",
    )
    categoria_actual: Optional[str] = Field(
        default=None,
        description="Categoría asignada actualmente. Se evaluará si es la más precisa.",
    )
    tipo: TipoMovimiento = Field(..., description="GASTO o INGRESO.")

class PeticionClasificar(PeticionBase):
    """Entrada para el módulo 1: Autoclasificación de transacciones."""
    transaccion: TransaccionParaClasificar = Field(
        ...,
        description="Transacción cuya categoría se desea clasificar o validar.",
    )


# ── Módulos 2, 3, 4, 5, 7, 8, 10: Con filtro de fecha ───────────────────────
 
class PeticionConFiltroFecha(PeticionBase, FiltroDeFecha):
    """
    Petición estándar para módulos que analizan el historial con filtros temporales.
    Usada por: PredecirGastos, DetectarAnomalias, OptimizarSuscripciones,
               CapacidadAhorro, Estacionalidad, PresupuestoDinamico, ReporteCompleto.
    """
    pass


# ── Módulo 6: Simulación de Meta de Ahorro ───────────────────────────────────
 
class PeticionSimularMeta(PeticionBase):
    """Entrada para el módulo 6: Simular cuánto tiempo tomará alcanzar una meta."""
    nombre_meta: str = Field(
        ...,
        min_length=1,
        max_length=200,
        description="Nombre descriptivo de la meta (ej: 'Laptop para estudios', 'Viaje Cusco').",
        examples=["Laptop Dell para programación"],
    )
    monto_objetivo: float = Field(
        ...,
        gt=0,
        description="Cuánto dinero necesita ahorrar en total (S/).",
    )
    monto_actual_ahorrado: float = Field(
        default=0.0,
        ge=0,
        description="Cuánto ya tiene ahorrado hacia esta meta (S/).",
    )
    aporte_mensual_deseado: Optional[float] = Field(
        default=None,
        gt=0,
        description="Cuánto puede aportar por mes. Si es None, se calcula desde la capacidad de ahorro.",
    )
 
    @model_validator(mode="after")
    def validar_progreso_coherente(self) -> "PeticionSimularMeta":
        """El monto ahorrado no puede superar el objetivo."""
        if self.monto_actual_ahorrado >= self.monto_objetivo:
            raise ValueError(
                "El monto_actual_ahorrado no puede ser mayor o igual al monto_objetivo. "
                "Si ya alcanzó la meta, no se necesita simulación."
            )
        return self


# ── Módulo 9: Simulación de Escenario "¿Qué pasaría si...?" ─────────────────
 
class PeticionSimularEscenario(PeticionBase, FiltroDeFecha):
    """Entrada para el módulo 9: Analizar el impacto de un cambio hipotético."""
    descripcion_escenario: str = Field(
        ...,
        min_length=5,
        max_length=300,
        description="Descripción del cambio hipotético (ej: 'Agregar suscripción Spotify S/20').",
        examples=["Agregar suscripción de gym por S/ 120 al mes"],
    )
    monto_cambio: float = Field(
        ...,
        description="Monto del cambio. Positivo = nuevo gasto. Negativo = reducción de gasto o nuevo ingreso.",
    )
    tipo_cambio: TipoMovimiento = Field(
        ...,
        description="¿El cambio es un nuevo GASTO o un nuevo INGRESO?",
    )
    recurrente: bool = Field(
        default=True,
        description="Si es True, el cambio se aplica todos los meses (suscripción). False = pago único.",
    )

# ══════════════════════════════════════════════════════════════════════════════
# DTO DE CONTEXTO PERSONAL — Perfil del usuario universitario
# ══════════════════════════════════════════════════════════════════════════════
 
class MetaAhorro(BaseModel):
    """Meta de ahorro activa del usuario, obtenida del microservicio-cliente."""
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
    """Límite de gasto mensual configurado por el usuario."""
    monto_limite: float = Field(default=0.0, ge=0, alias="montoLimite")
    activo: bool = Field(default=False)
 
    model_config = {"populate_by_name": True, "extra": "ignore"}
 
 
class PerfilUsuario(BaseModel):
    """
    Contexto personal del universitario, obtenido del microservicio-cliente.
 
    Diseño tolerante a fallos:
      - Todos los campos son opcionales con defaults seguros.
      - Si el microservicio-cliente no responde o falta algún campo,
        el coach sigue funcionando con la información disponible.
      - extra='ignore' absorbe campos nuevos del Java sin romper el modelo.
 
    Cuando tengas los campos exactos del endpoint Java, solo actualiza
    los alias (camelCase → snake_case) sin tocar el resto del sistema.
    """
    # ── Identidad personal ────────────────────────────────────────────────────
    nombre: Optional[str] = Field(
        default=None,
        description="Nombre del universitario (ej: 'Paulo'). "
                    "Gemini lo usará para personalizar el saludo.",
    )
    carrera: Optional[str] = Field(
        default=None,
        description="Carrera universitaria (ej: 'Ingeniería de Sistemas').",
    )
    universidad: Optional[str] = Field(
        default=None,
        description="Nombre de la universidad.",
    )
    ciudad: Optional[str] = Field(
        default=None,
        description="Ciudad donde estudia y gasta (ej: 'Lima', 'Arequipa').",
    )
    ciclo: Optional[int] = Field(
        default=None,
        ge=1,
        le=12,
        description="Ciclo académico actual. Útil para ajustar el contexto "
                    "(ciclo 1 ≠ ciclo 10 en términos de gastos).",
    )
 
    # ── Datos financieros configurados ────────────────────────────────────────
    ingreso_mensual: float = Field(
        default=0.0,
        ge=0,
        alias="ingresoMensual",
        description="Ingreso mensual declarado por el usuario (S/).",
    )
    meta_ahorro_activa: Optional[MetaAhorro] = Field(
        default=None,
        alias="metaAhorroActiva",
        description="Meta de ahorro principal activa.",
    )
    limite_gasto: Optional[LimiteGasto] = Field(
        default=None,
        alias="limiteGasto",
        description="Límite de gasto mensual configurado.",
    )
 
    model_config = {"populate_by_name": True, "extra": "ignore"}
 
    @classmethod
    def sin_datos(cls) -> "PerfilUsuario":
        """
        Retorna un perfil vacío cuando el microservicio-cliente no responde.
        El coach trabajará en modo genérico sin personalización.
        """
        return cls()
 
    @property
    def nombre_display(self) -> str:
        """Nombre para el saludo del coach. 'estudiante' si no está disponible."""
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
        """
        Genera un resumen legible del perfil para incluir en el prompt de Gemini.
        Solo incluye los campos disponibles, omite los None.
        """
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
            partes.append(
                f"Límite de gasto mensual: S/ {self.limite_gasto.monto_limite:,.2f}"
            )
        return "\n".join(f"- {p}" for p in partes) if partes else "- Perfil no disponible."
 
 
# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE SALIDA — Respuestas de los endpoints
# ══════════════════════════════════════════════════════════════════════════════

# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE SALIDA — Respuestas de los endpoints
# ══════════════════════════════════════════════════════════════════════════════
 
class PuntoGrafico(BaseModel):
    """Un punto de dato para renderizar en un gráfico del Dashboard."""
    etiqueta: str = Field(..., description="Etiqueta del eje X (ej: 'Ene 2026', 'Alimentación').")
    valor: float = Field(..., description="Valor numérico del punto.")
    color: Optional[str] = Field(
        default=None,
        description="Color hex opcional para el frontend (ej: '#E74C3C').",
        examples=["#3498DB"],
    )
 
 
class MetadataGrafico(BaseModel):
    """
    Datos estructurados para que el Dashboard de LUKA renderice un chart.
    Cada módulo define su propio tipo de gráfico.
    """
    tipo_grafico: TipoGrafico = Field(..., description="Tipo de visualización.")
    titulo: str = Field(..., description="Título legible del gráfico.")
    datos_primarios: List[PuntoGrafico] = Field(
        default_factory=list,
        description="Serie principal de datos.",
    )
    datos_secundarios: List[PuntoGrafico] = Field(
        default_factory=list,
        description="Segunda serie (ej: ingresos vs gastos).",
    )
    unidad: str = Field(default="S/", description="Unidad monetaria o de medida.")
    linea_referencia: Optional[float] = Field(
        default=None,
        description="Valor de línea de referencia (ej: 0, límite de presupuesto).",
    )
 
 
class InsightAnalitico(BaseModel):
    """
    Resumen técnico producido por el Motor Analítico (Pandas/Scikit-Learn).
    Este objeto es el que se envía a Gemini como contexto.
    Gemini NO ve el DataFrame crudo; solo ve este resumen conciso.
 
    Es el "jugo" que el motor extrae de los datos antes de llamar al coach.
    """
    modulo: NombreModulo = Field(..., description="Módulo que generó el insight.")
    total_transacciones_analizadas: int = Field(default=0, ge=0)
 
    # ── Métricas financieras clave ────────────────────────────────────────────
    total_ingresos: float = Field(default=0.0, description="Suma de ingresos en el período.")
    total_gastos: float = Field(default=0.0, description="Suma de gastos en el período.")
    balance_neto: float = Field(default=0.0, description="Ingresos - Gastos.")
    promedio_gasto_mensual: float = Field(default=0.0)
    promedio_ingreso_mensual: float = Field(default=0.0)
 
    # ── Hallazgos específicos por módulo ─────────────────────────────────────
    hallazgos: Dict[str, float | str | int | bool | list] = Field(
        default_factory=dict,
        description=(
            "Datos calculados específicos del módulo. "
            "Ej: {'variacion_pct': 12.3, 'categoria_exceso': 'Entretenimiento', "
            "     'proyeccion_cierre': 2450.0, 'meses_para_meta': 8}"
        ),
    )
    nivel_alerta: NivelRiesgo = Field(
        default=NivelRiesgo.BAJO,
        description="Nivel de riesgo calculado para priorizar el consejo del coach.",
    )
    periodo_analizado: str = Field(
        default="",
        description="Período legible del análisis (ej: 'Enero 2026 - Abril 2026').",
    )


class KpiWidget(BaseModel):
    """Valor destacado para el header del widget en el Dashboard de LUKA."""
    valor: float = Field(..., description="Número a mostrar en grande.")
    etiqueta: str = Field(..., description="Descripción del KPI (ej: '% de ahorro').")
    unidad: str = Field(default="", description="Unidad del valor (ej: 'S/', '%', 'meses').")
    tendencia: Optional[str] = Field(
        default=None,
        description="'subida', 'bajada' o 'estable' para la flechita del widget.",
    )
 
 
class EstadoCoach(str, Enum):
    """
    Estado del coach IA en la respuesta.
    Permite al frontend saber si el consejo está disponible
    o si debe mostrar un mensaje de degradación elegante.
    """
    EXITOSO = "EXITOSO"           # Gemini respondió correctamente.
    CUOTA_AGOTADA = "CUOTA_AGOTADA"   # Rate limit 429 alcanzado.
    AUTH_ERROR = "AUTH_ERROR"         # API Key inválida o expirada.
    TIMEOUT = "TIMEOUT"               # Gemini tardó demasiado.
    NO_DISPONIBLE = "NO_DISPONIBLE"   # Cualquier otro fallo de Gemini.
 
 
class RespuestaModulo(BaseModel):
    """
    Respuesta unificada para los 10 endpoints de análisis.
 
    Garantía de degradación elegante:
      El campo `insight`, `grafico` y `kpi` SIEMPRE están presentes
      porque los calcula el Motor Analítico (Pandas/Scikit-Learn), que
      nunca depende de servicios externos.
 
      El campo `consejo` es Optional: puede ser None si Gemini falla.
      El campo `estado_coach` indica el motivo exacto para que el
      frontend pueda mostrar un mensaje adecuado al universitario.
    """
    # ── Identificación ────────────────────────────────────────────────────────
    id_respuesta: str = Field(
        default_factory=lambda: str(uuid4()),
        description="UUID único de esta respuesta (útil para trazabilidad).",
    )
    usuario_id: str
    modulo: NombreModulo
    fecha_generacion: datetime = Field(default_factory=datetime.now)
 
    # ── Consejo del Coach (Gemini) — opcional por degradación elegante ────────
    consejo: Optional[str] = Field(
        default=None,
        description=(
            "Consejo financiero en lenguaje natural generado por Gemini. "
            "None si el coach no está disponible; los datos del insight siguen presentes."
        ),
    )
    estado_coach: EstadoCoach = Field(
        default=EstadoCoach.EXITOSO,
        description=(
            "Estado del coach IA. EXITOSO si el consejo está disponible. "
            "Cualquier otro valor indica el motivo por el que consejo es None."
        ),
    )
 
    # ── Datos calculados por el Motor Analítico — siempre presentes ───────────
    insight: InsightAnalitico = Field(
        ...,
        description="Resumen técnico calculado por Pandas/Scikit-Learn. Siempre presente.",
    )
 
    # ── Visualización para el Dashboard ──────────────────────────────────────
    grafico: Optional[MetadataGrafico] = Field(
        default=None,
        description="Datos para renderizar el chart. None si el módulo no tiene gráfico.",
    )
    kpi: Optional[KpiWidget] = Field(
        default=None,
        description="Valor destacado para el header del widget.",
    )
 
    def a_dict_serializable(self) -> dict:
        """Serialización segura con fechas en formato ISO para JSON/RabbitMQ."""
        datos = self.model_dump()
        datos["fecha_generacion"] = self.fecha_generacion.isoformat()
        return datos


# ══════════════════════════════════════════════════════════════════════════════
# DTOs AUXILIARES (usados internamente entre capas)
# ══════════════════════════════════════════════════════════════════════════════
 
class ResumenMensual(BaseModel):
    """
    Totales consolidados de un mes calendario.
    Producido por el preparador de datos a partir del DataFrame.
    """
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
        """Etiqueta corta para gráficos: '2026-04'"""
        return f"{self.anio}-{self.mes:02d}"
 
    @property
    def porcentaje_ahorro(self) -> float:
        """% de ahorro del mes. 0 si no hay ingresos."""
        if self.total_ingresos <= 0:
            return 0.0
        return round(((self.total_ingresos - self.total_gastos) / self.total_ingresos) * 100, 1)


# ══════════════════════════════════════════════════════════════════════════════
# DTOs DE EVENTOS — Sincronización con ms-auditoria (Java)
# ══════════════════════════════════════════════════════════════════════════════

class EventoAuditoriaDTO(BaseModel):
    """DTO para auditoría de eventos generales del sistema."""
    usuario_id: Optional[str] = Field(default=None, alias="usuarioId")
    accion: str
    modulo: str
    ip_origen: Optional[str] = Field(default=None, alias="ipOrigen", max_length=45)
    detalles: Optional[str] = None
    fecha: datetime = Field(default_factory=datetime.now)

    model_config = {"populate_by_name": True}


class EventoAccesoDTO(BaseModel):
    """Representa un evento de seguridad relacionado con el acceso al sistema."""
    usuario_id: Optional[str] = Field(default=None, alias="usuarioId")
    ip_origen: str = Field(..., alias="ipOrigen", max_length=45)
    navegador: Optional[str] = Field(default="LUKA-IA-SERVICE", max_length=500)
    estado: EstadoEvento
    detalle_error: Optional[str] = Field(default=None, alias="detalleError", max_length=500)
    fecha: datetime = Field(default_factory=datetime.now)
    correlation_id: Optional[str] = Field(default=None, alias="correlationId")

    model_config = {"populate_by_name": True}


class EventoTransaccionalDTO(BaseModel):
    """Registro de cambios en entidades de negocio (Trazabilidad)."""
    usuario_id: str = Field(..., alias="usuarioId")
    entidad_id: str = Field(..., alias="entidadId")
    servicio_origen: str = Field(default="ms-ia", alias="servicioOrigen")
    entidad_afectada: str = Field(..., alias="entidadAfectada")
    descripcion: str
    valor_anterior: Optional[str] = Field(default=None, alias="valorAnterior")
    valor_nuevo: Optional[str] = Field(default=None, alias="valorNuevo")
    fecha: datetime = Field(default_factory=datetime.now)

    model_config = {"populate_by_name": True}