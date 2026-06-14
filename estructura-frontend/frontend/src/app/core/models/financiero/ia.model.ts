export type TipoSolicitudIa = 'TRANSACCION_RECIENTE' | 'CONSULTA_MODULO';

export type ModuloIa =
  | 'TRANSACCION_AUTOMATICA'
  | 'PREDICCION_GASTOS'
  | 'GASTO_HORMIGA'
  | 'AUTOCLASIFICACION'
  | 'COMPARACION_MENSUAL'
  | 'CAPACIDAD_AHORRO'
  | 'METAS_FINANCIERAS'
  | 'ANOMALIAS'
  | 'ESTACIONALIDAD'
  | 'PRESUPUESTO_DINAMICO'
  | 'REPORTE_COMPLETO'
  | 'HABITOS_FINANCIEROS'
  | 'RETO_AHORRO_DINAMICO'
  | 'ANALISIS_ESTILO_VIDA'
  | 'ESPEJO_TEMPORAL'
  | 'ZONA_ENTRENAMIENTO';

export interface SolicitudIaDTO {
  idUsuario: string;
  tipoSolicitud: TipoSolicitudIa;
  moduloSolicitado?: ModuloIa;
}

// ── DTOs de Entrada para ms-ia ──

export interface PeticionConFiltroFechaDTO {
  usuario_id?: string; // Autocompletado del JWT
  token?: string; // Autocompletado del JWT
  mes?: number; // 1-12
  anio?: number; // 2020-2100
  tamanio_pagina?: number; // 10-1000
  frecuencia?: 'SEMANAL' | 'QUINCENAL' | 'MENSUAL';
}

export interface PeticionSimularMetaDTO {
  usuario_id?: string;
  nombre_meta: string;
  monto_objetivo: number;
  monto_actual_ahorrado?: number;
  aporte_mensual_deseado?: number;
}

export interface PeticionComparacionDTO {
  rangoA_inicio: string;
  rangoA_fin: string;
  rangoB_inicio: string;
  rangoB_fin: string;
}

export interface SolicitudClasificacionDTO {
  id_temporal: string;
  tipo_movimiento: 'INGRESO' | 'GASTO';
  etiquetas?: string;
  notas?: string;
  descripcion?: string;
}

// ── DTOs de Salida de ms-ia ──

export interface PuntoGraficoIaDTO {
  etiqueta: string;
  valor: number;
  color?: string;
}

export interface MetadataGraficoIaDTO {
  tipoGrafico: string;
  titulo: string;
  datos: PuntoGraficoIaDTO[];
  datosAux?: PuntoGraficoIaDTO[];
  unidad?: string;
  metaLinea?: number;
}

export interface KpiWidgetDTO {
  valor: number;
  etiqueta: string;
  tendencia?: string; // "ALZA" | "BAJA" | "ESTABLE"
  variacion_porcentaje?: number;
  unidad?: string;
}

export interface RespuestaModuloDTO {
  id_respuesta: string;
  usuario_id: string;
  modulo: string;
  fecha_generacion: string;
  consejo: any | string | null;
  estado_coach: 'EXITOSO' | 'CUOTA_AGOTADA' | 'AUTH_ERROR' | 'TIMEOUT' | 'NO_DISPONIBLE';
  usando_fallback: boolean;
  insight: any; // Datos analíticos puros devueltos por Pandas
  grafico?: MetadataGraficoIaDTO;
  kpi?: KpiWidgetDTO;
}

export interface RespuestaClasificacionDTO {
  id_temporal: string;
  sugerencias: string[];
  usando_fallback: boolean;
}

// Compatibilidad heredada
export interface RespuestaIaDTO {
  id: string;
  idUsuario: string;
  consejoTexto: string;
  tipoModulo: string;
  fechaGeneracion: string;
  metadataGrafico?: MetadataGraficoIaDTO;
  kpiPrincipal?: number;
  kpiLabel?: string;
}

// ── Modelos Específicos para Módulo: Zona de Entrenamiento ──

export interface EjercicioEntrenamientoDTO {
  nombre: string;
  descripcion: string;
  duracion_dias: number;
  frecuencia: string;
  metrica_exito: string;
}

export interface ConsejoEntrenamientoDTO {
  pensamiento_interno_ia: string;
  estado_fisico: string;
  evaluacion_previa?: string;
  rutina: EjercicioEntrenamientoDTO[];
}

// ── Modelos Específicos para Módulo: Predicción de Gastos ──
export interface ConsejoPredecirGastosDTO {
  pensamiento_interno_ia: string;
  introduccion: string;
  analisis_tendencia: string;
  impacto_meta: string;
  recomendacion_matematica: string;
  mensaje_motivacional: string;
}

// ── Modelos Específicos para Módulo: Simular Meta ──
export interface ConsejoSimularMetaDTO {
  pensamiento_interno_ia: string;
  introduccion: string;
  diagnostico_viabilidad: string;
  plan_accion: string;
  tecnica_sugerida?: string;
  mensaje_motivacional: string;
}

// ── Modelos Específicos para Módulo 5: Espejo Temporal ──

export interface DatosPresenteDTO {
  scoreActual: number;
  saldoActual: number;
  metasActivas: number;
}

export interface ProyeccionHitoDTO {
  scoreProyectado: number;
  ahorroAcumulado: number;
  metasCumplidas: string[];
  metasFracasadas: string[];
}

export interface ProyeccionFuturaDTO {
  hitos3Meses: ProyeccionHitoDTO;
  hitos6Meses: ProyeccionHitoDTO;
  hitos12Meses: ProyeccionHitoDTO;
}

export interface NarrativasGeminiDTO {
  cartaContinuidad: string;
  cartaTransformacion: string;
}

export interface InsightEspejoTemporalDTO {
  datosPresente: DatosPresenteDTO;
  proyeccionContinuidad: ProyeccionFuturaDTO;
  proyeccionTransformacion: ProyeccionFuturaDTO;
  narrativasGemini: NarrativasGeminiDTO;
}

// ── Modelos Específicos para Módulo: Gasto Hormiga ──
export interface ConsejoGastoHormigaDTO {
  pensamiento_interno_ia: string;
  introduccion: string;
  analisis_ia: string;
  conexion_emocional: string;
  plan_accion_titulo: string;
  plan_accion_pasos: string[];
  comentario_positivo: string;
}
