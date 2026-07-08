import { TransaccionDTO } from '../financiero/transaccion.model';

/**
 * Resumen consolidado avanzado para las tarjetas KPI del Dashboard V2.
 */
export interface DashboardResumenDTO {
  desde: string; // ISO LocalDateTime
  hasta: string; // ISO LocalDateTime
  tasaAhorro: number; // Porcentaje de ahorro
  gastoPromedioDiario: number; // Nuevo KPI
  cumplimientoPresupuesto: number; // % consumido
  proyeccionFinDeMes: number; // Balance proyectado
  totalIngresos?: number; // Total acumulado anual de ingresos
  totalGastos?: number; // Total acumulado anual de egresos
  balance?: number; // Balance total acumulado anual
  volumenTransacciones?: number; // Cantidad total de movimientos en el periodo
}

export interface CashflowPointDTO {
  mes: string;
  ingresos: number;
  gastos: number;
}

export interface CategoriaDistribucionDTO {
  categoria: string;
  total: number;
  porcentaje: number;
  color: string;
}



/** [NUEVO] Mapa de calor de gastos por día de semana */
export interface HeatmapPointDTO {
  dia: string; // Lunes, Martes, etc.
  intensidad: number; // 0-10 o monto
}

/** [NUEVO] Progreso de metas */
export interface MetaProgressDTO {
  nombre: string;
  objetivo: number;
  actual: number;
  porcentaje: number;
  color: string;
}

/** [NUEVO] Comparativa histórica mensual */
export interface ComparativaMensualDTO {
  mes: string;
  actual: number; // Gasto este año
  anterior: number; // Gasto el año pasado
}

/**
 * DTO contenedor principal (Enriquecido)
 */
export interface DashboardAnaliticaDTO {
  resumen: DashboardResumenDTO;
  flujoCaja: CashflowPointDTO[];
  distribucionGastos: CategoriaDistribucionDTO[];
  heatmap: HeatmapPointDTO[];
  metas: MetaProgressDTO[];
  comparativa: ComparativaMensualDTO[];
  transaccionesMetodo?: { metodo: string; cantidad: number; color: string }[];
}
