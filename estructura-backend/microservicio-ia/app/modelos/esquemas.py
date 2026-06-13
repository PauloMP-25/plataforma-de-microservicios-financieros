"""
modelos/esquemas.py  ·  v5 — FASE 1: Structured Outputs + Memoria (LUKA)
══════════════════════════════════════════════════════════════════════════════
DTOs (Data Transfer Objects) de entrada y salida para los 10 módulos de
análisis del Microservicio IA de LUKA.

Cambios v5 (FASE 1 - Migración Incremental):
  - Nuevo: ConsejoEstructurado — esquema de salida JSON para Gemini Structured Outputs.
    Usado exclusivamente por GASTO_HORMIGA en esta fase.
  - RespuestaModulo.consejo: Optional[str] → Optional[Union[str, ConsejoEstructuradoHormiga]]
    para retrocompatibilidad con los 9 módulos que siguen devolviendo str plano.
  - a_dict_serializable(): blindado para serializar correctamente ambos tipos.
══════════════════════════════════════════════════════════════════════════════
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Dict, List, Optional, Union, Any
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
    COMPROBADOR_EVOLUCION = "COMPROBADOR_EVOLUCION"
    ZONA_ENTRENAMIENTO = "ZONA_ENTRENAMIENTO"
    ESPEJO_TEMPORAL = "ESPEJO_TEMPORAL"


# ══════════════════════════════════════════════════════════════════════════════
# NUEVO v5: CONSEJO ESTRUCTURADO — Esquema para Gemini Structured Outputs
# ══════════════════════════════════════════════════════════════════════════════

class ConsejoEstructuradoHabitos(BaseModel):
    """
    Esquema de salida estructurada para el módulo HABITOS_FINANCIEROS.
    """
    pensamiento_interno_ia: str = Field(description="Análisis paso a paso del LLM.")
    introduccion: str = Field(description="Saludo directo y evaluación rápida.")
    analisis_patron: str = Field(description="Comentario sobre los patrones detectados.")
    habito_atomico_sugerido: str = Field(description="Hábito accionable pequeño propuesto.")
    mensaje_motivacional: str = Field(description="Cierre motivador.")

class ConsejoEstructuradoReto(BaseModel):
    """
    Esquema de salida estructurada para el módulo RETO_AHORRO_DINAMICO.
    Se adapta tanto a la fase de proposición (NUEVO) como evaluación (VEREDICTO).
    """
    pensamiento_interno_ia: str = Field(description="Análisis paso a paso de la meta de ahorro.")
    titulo_mision: str = Field(description="Nombre creativo de la misión.")
    diagnostico: str = Field(description="Explicación concisa del estado del reto o área de mejora.")
    estrategia: str = Field(description="Acciones específicas para alcanzar o mantener el ahorro.")
    mensaje_motivacional: str = Field(description="Mensaje enérgico para el usuario.")

class ConsejoEstructuradoHormiga(BaseModel):
    """
    Esquema de salida estructurada para el módulo GASTO_HORMIGA.

    Gemini usará este modelo como response_schema para garantizar que la
    respuesta sea siempre un JSON válido con estos campos exactos.
    El frontend puede renderizar cada campo de forma independiente
    (cards, listas, badges) en lugar de parsear Markdown.

    Compatibilidad:
      - Los 9 módulos restantes siguen devolviendo str plano.
      - RespuestaModulo.consejo es Union[str, ConsejoEstructuradoHormiga] para soportar ambos.
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


class RecetaCategoria(BaseModel):
    categoria: str = Field(..., description="Nombre exacto de la categoría reincidente a tratar.")
    diagnostico: str = Field(..., description="Descripción del patrón detectado en lenguaje clínico y si es reincidencia o nuevo exceso.")
    posologia: List[str] = Field(..., min_length=3, max_length=3, description="Exactamente 3 acciones medibles para esta semana.")
    pronostico: str = Field(..., description="Estimación en términos monetarios de cuánto dinero adicional tendría en 3 meses.")

class ConsejoEstructuradoEvolucion(BaseModel):
    pensamiento_interno_ia: str = Field(..., description="Razonamiento lógico sobre el diagnóstico del IMF y las recetas.")
    veredicto_narrativo: str = Field(..., description="Párrafo de máximo 4 oraciones que justifica la clasificación diagnóstica final.")
    recetas_medicas: List[RecetaCategoria] = Field(..., description="Lista de recetas. Una por cada categoría reincidente (o de mantenimiento si no hay reincidencias).")

class EjercicioEntrenamiento(BaseModel):
    nombre: str = Field(..., description="Nombre motivacional del ejercicio de entrenamiento.")
    descripcion: str = Field(..., description="Descripción detallada de la acción a realizar.")
    duracion_dias: int = Field(..., description="Duración total del ejercicio en días (ej: 30).")
    frecuencia: str = Field(..., description="Frecuencia (ej: 'Diario', '1 vez a la semana', '3 veces por semana').")
    metrica_exito: str = Field(..., description="Condición medible y verificable para considerar el ejercicio cumplido.")

class ConsejoEstructuradoEntrenamiento(BaseModel):
    pensamiento_interno_ia: str = Field(..., description="Razonamiento lógico sobre el estado físico del usuario basado en sus signos vitales.")
    estado_fisico: str = Field(..., description="Uno de los 5 estados: 'Atleta de Élite', 'En Forma', 'Sedentario', 'Lesionado' o 'UCI Financiera'.")
    evaluacion_previa: Optional[str] = Field(..., description="Breve evaluación del cumplimiento de la rutina del mes anterior. Vacío si es la primera vez.")
    rutina: List[EjercicioEntrenamiento] = Field(..., min_length=3, max_length=3, description="Exactamente 3 ejercicios para el mes.")


class ConsejoEstructuradoEspejo(BaseModel):
    """
    Esquema de salida estructurada para el módulo ESPEJO_TEMPORAL.

    Gemini recibe los KPIs resumidos (FASE 3) y escribe exclusivamente
    estas dos cartas narrativas al futuro. Todos los números ya vienen
    calculados por Pandas en FASE 2; Gemini solo narra.

    cartaContinuidad    — Carta al usuario si continúa con sus hábitos actuales.
    cartaTransformacion — Carta al usuario si optimiza sus gastos no esenciales.
    """
    cartaContinuidad: str = Field(
        ...,
        description=(
            "Carta breve (máx 5 oraciones, 120 palabras) en segunda persona y tiempo presente "
            "que describe el futuro financiero del usuario sin cambios en sus hábitos. "
            "Debe citar el score proyectado y el ahorro mensual actual."
        ),
    )
    cartaTransformacion: str = Field(
        ...,
        description=(
            "Carta breve (máx 5 oraciones, 120 palabras) en segunda persona y tiempo presente "
            "que describe el futuro financiero del usuario si reduce sus gastos no esenciales. "
            "Debe citar el ahorro mensual optimizado y la diferencia neta acumulada en 12 meses."
        ),
    )

class ConsejoEstructuradoPredecir(BaseModel):
    pensamiento_interno_ia: str = Field(..., description="Razonamiento lógico sobre el riesgo de insolvencia y la tendencia de gastos.")
    introduccion: str = Field(..., description="Saludo personalizado indicando el estado de alerta o estabilidad.")
    analisis_tendencia: str = Field(..., description="Análisis de la proyección para el próximo mes y la variación porcentual.")
    impacto_meta: str = Field(..., description="Explicación de cómo esta tendencia afecta su meta de ahorro principal. Si no tiene metas, inventa un objetivo financiero.")
    recomendacion_matematica: str = Field(..., description="Recomendación exacta basada en los números (ej. 'necesitas reducir tus gastos en un 10%').")
    mensaje_motivacional: str = Field(..., description="Frase de cierre empática y motivacional.")


class ConsejoEstructuradoSimularMeta(BaseModel):
    pensamiento_interno_ia: str = Field(..., description="Razonamiento lógico sobre la viabilidad basada en el déficit y la capacidad de ahorro.")
    introduccion: str = Field(..., description="Saludo personalizado e indicación clara de si la meta es viable o no.")
    diagnostico_viabilidad: str = Field(..., description="Explicación concisa basada en el tiempo estimado y la capacidad de ahorro.")
    plan_accion: str = Field(..., description="Pasos concretos a tomar (ej: ajustes necesarios si no es viable, o mantenimiento si lo es).")
    tecnica_sugerida: Optional[str] = Field(..., description="Nombre y breve descripción de una técnica de ahorro recomendada.")
    mensaje_motivacional: str = Field(..., description="Cierre empático.")



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


class PeticionComparacionDTO(PeticionBase):
    token: str = Field(..., min_length=10)
    tamanio_pagina: int = Field(default=200, ge=10, le=1000)
    rango_a_inicio: datetime = Field(...)
    rango_a_fin: datetime = Field(...)
    rango_b_inicio: datetime = Field(...)
    rango_b_fin: datetime = Field(...)

    @model_validator(mode="after")
    def validar_rangos_fechas(self) -> "PeticionComparacionDTO":
        if self.rango_a_inicio > self.rango_a_fin:
            raise ValueError("El rango A de inicio debe ser anterior o igual al fin.")
        if self.rango_b_inicio > self.rango_b_fin:
            raise ValueError("El rango B de inicio debe ser anterior o igual al fin.")
        if self.rango_a_fin >= self.rango_b_inicio:
            raise ValueError("El Periodo A debe ser estrictamente anterior al Periodo B sin solapamiento.")
        
        dias_a = (self.rango_a_fin - self.rango_a_inicio).days + 1
        dias_b = (self.rango_b_fin - self.rango_b_inicio).days + 1
        if dias_a < 15 or dias_b < 15:
            raise ValueError("Ambos periodos deben tener una duración mínima de 15 días para el análisis.")
        return self


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
 
 
class ConsejoEstructuradoReporte(BaseModel):
    """
    Esquema de salida estructurada para el módulo REPORTE_COMPLETO.
    """
    pensamiento_interno_ia: str = Field(description="Análisis paso a paso del reporte.")
    analisis_score: str = Field(description="Explicación del Score de Salud (Riesgo, Estable, Excelente).")
    impacto_meta: str = Field(description="Análisis del balance y su efecto en la meta actual.")
    veredicto_final: str = Field(description="Cierre ejecutivo de lo que va del año.")
    mensaje_motivacional: str = Field(description="Frase motivadora respecto a lo que va del año.")

class ConsejoEstructuradoEstilo(BaseModel):
    """
    Esquema de salida estructurada para el módulo ANALISIS_ESTILO_VIDA.
    """
    pensamiento_interno_ia: str = Field(description="Análisis sobre los clusters de gasto detectados.")
    arquetipo: str = Field(description="Nombre creativo de la personalidad (ej: 'El Foodie Explorador').")
    significado_arquetipo: str = Field(description="Breve descripción de qué significa esta personalidad y por qué se le asignó.")
    descripcion_perfil: str = Field(description="Breve diagnóstico de su estilo de vida basado en los datos.")
    consejo_tactico: str = Field(description="Hack para ahorrar sin renunciar a sus gustos de ese estilo de vida.")
    alineacion_meta: str = Field(description="Cómo su estilo de vida impacta en su meta principal.")
    mensaje_estilo_vida: str = Field(description="Frase motivadora alineada al arquetipo descubierto.")

class ConsejoEstructuradoAutoClasificacion(BaseModel):
    """
    Esquema ultra estricto para el módulo AUTO_CLASIFICACION.
    """
    categorias_sugeridas: List[str] = Field(
        description="Lista de exactamente 4 palabras únicas que mejor categorizan la descripción del gasto/ingreso.",
        min_items=4,
        max_items=4
    )

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
      - Ahora:  Optional[Union[str, ConsejoEstructuradoHormiga]]
    Contrato universal de salida para todos los módulos.
    """
    id_respuesta: str = Field(default_factory=lambda: str(uuid4()))
    usuario_id: str
    modulo: NombreModulo
    fecha_generacion: datetime = Field(default_factory=datetime.now)
 
    consejo: Optional[Union[str, ConsejoEstructuradoHormiga, ConsejoEstructuradoEvolucion, ConsejoEstructuradoEntrenamiento, ConsejoEstructuradoEspejo, ConsejoEstructuradoPredecir, ConsejoEstructuradoSimularMeta, ConsejoEstructuradoHabitos, ConsejoEstructuradoReto, ConsejoEstructuradoReporte, ConsejoEstructuradoEstilo, ConsejoEstructuradoAutoClasificacion, Any]] = Field(
        default=None,
        description=(
            "Consejo financiero estructurado."
        ),
    )
    estado_coach: EstadoCoach = Field(default=EstadoCoach.EXITOSO)
    usando_fallback: bool = Field(default=False)
 
    insight: InsightAnalitico = Field(...)
 
    grafico: Optional[MetadataGrafico] = Field(default=None)
    kpi: Optional[KpiWidget] = Field(default=None)
 
    def a_dict_serializable(self) -> dict:
        datos = self.model_dump()

        if isinstance(self.consejo, BaseModel):
            datos["consejo"] = self.consejo.model_dump()
        elif isinstance(self.consejo, dict):
            datos["consejo"] = self.consejo

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
