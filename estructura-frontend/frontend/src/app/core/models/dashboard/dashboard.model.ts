import { TransaccionDTO } from '../financiero/transaccion.model';

/**
   Resumen consolidado para las tarjetas KPI del Dashboard.
 */
export interface DashboardResumenDTO {
  desde: string; // ISO LocalDateTime
  hasta: string; // ISO LocalDateTime
  totalIngresos: number;
  totalGastos: number;
  balance: number;
  cantidadIngresos: number;
  cantidadGastos: number;
  tasaAhorro: number; // Calculado en el front o provisto
}

/**
   Punto de datos para el gráfico de líneas comparativo de flujo de caja.
 */
export interface CashflowPointDTO {
  mes: string; // Ene, Feb, etc.
  ingresos: number;
  gastos: number;
}

/**
   Distribución de gastos agrupados para el gráfico circular/dona.
 */
export interface CategoriaDistribucionDTO {
  categoria: string;
  total: number;
  porcentaje: number;
  color: string;
}

/**
   DTO contenedor principal para la carga completa del Dashboard.
 */
export interface DashboardDataDTO {
  resumen: DashboardResumenDTO;
  flujoCaja: CashflowPointDTO[];
  distribucionGastos: CategoriaDistribucionDTO[];
  recientes: TransaccionDTO[];
}
