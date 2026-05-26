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
  | 'REPORTE_COMPLETO';

export interface SolicitudIaDTO {
  idUsuario: string;
  tipoSolicitud: TipoSolicitudIa;
  moduloSolicitado?: ModuloIa;
}

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

