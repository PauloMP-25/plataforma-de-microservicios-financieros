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

export interface AdminAlertaSistema {
  titulo: string;
  descripcion: string;
  severidad: 'critica' | 'media' | 'informativa';
  icono: string;
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
  alertas: AdminAlertaSistema[];
  ipsBloqueadas: AdminIpBloqueada[];
  otpsBloqueados: AdminOtpBloqueado[];
  graficoIngresos: AdminBarraGrafico[];
  secciones: AdminSeccionResumen[];
}
