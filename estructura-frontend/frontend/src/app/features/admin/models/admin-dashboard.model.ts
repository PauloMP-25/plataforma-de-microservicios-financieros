export type AdminEstadoServicio = 'healthy' | 'warning' | 'down';
export type AdminTono = 'primary' | 'info' | 'success' | 'warning' | 'danger' | 'purple';

export interface AdminKpiCard {
  etiqueta: string;
  valor: string;
  detalle: string;
  tendencia: string;
  tendenciaTipo: 'up' | 'down' | 'neutral';
  icono: string;
  tono: AdminTono;
}

export interface AdminServicioEstado {
  nombre: string;
  puerto: number;
  estado: AdminEstadoServicio;
  latencia: string;
  descripcion: string;
}

export interface AdminPagoReciente {
  id: string;
  usuario: string;
  monto: string;
  plan: string;
  estado: 'EXITOSO' | 'PENDIENTE' | 'FALLIDO';
}


export interface AdminIpBloqueada {
  ip: string;
  motivo: string;
  tiempo: string;
}

export interface AdminOtpBloqueado {
  usuario: string;
  estado: 'Bloqueado' | 'En revisión';
  intentos: number;
}

export interface AdminBarraGrafico {
  mes: string;
  ingresos: number;
  egresos: number;
}

export interface AdminSeccionResumen {
  titulo: string;
  descripcion: string;
  estado: 'conectado' | 'mock' | 'pendiente';
  endpoint?: string;
}

export interface AdminDashboardData {
  kpis: AdminKpiCard[];
  servicios: AdminServicioEstado[];
  pagos: AdminPagoReciente[];

  ipsBloqueadas: AdminIpBloqueada[];
  otpsBloqueados: AdminOtpBloqueado[];
  graficoIngresos: AdminBarraGrafico[];
  secciones: AdminSeccionResumen[];
}

export interface ResumenPagosDTO {
  totalTransacciones: number;
  ingresosTotales: number;
  transaccionesPorEstado: Record<string, number>;
  suscripcionesPorPlan: Record<string, number>;
  graficoIngresos?: AdminBarraGrafico[];
}

export interface DetallePagoAdmin {
  id: string;
  planSolicitado: string;
  monto: number;
  moneda: string;
  descripcion?: string;
  descuento?: number;
  cantidad: number;
}

export interface PagoAdmin {
  id: string;
  usuarioId: string;
  estado: 'EXITOSO' | 'PENDIENTE' | 'FALLIDO';
  stripeSessionId?: string;
  stripeEventoId?: string;
  fechaInicioPlan?: string;
  fechaFinPlan?: string;
  fechaCreacion: string;
  fechaActualizacion?: string;
  detalles?: DetallePagoAdmin[];
}

export interface PaginacionAdmin<T> {
  contenido: T[];
  numeroPagina: number;
  tamañoPagina: number;
  totalElementos: number;
  totalPaginas: number;
  esUltima: boolean;
}
