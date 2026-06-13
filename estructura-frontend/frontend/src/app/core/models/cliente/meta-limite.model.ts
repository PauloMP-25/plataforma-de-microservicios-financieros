export interface SolicitudMetaAhorro {
  nombre: string;
  montoObjetivo: number;
  montoActual?: number;
  fechaLimite: string;
  proposito?: string;
}

export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface RespuestaMetaAhorro {
  id: string;
  nombre: string;
  montoObjetivo: number;
  montoActual: number;
  porcentajeProgreso: number;
  fechaLimite: string;
  completada: boolean;
  proposito?: string;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface SolicitudLimiteGasto {
  montoLimite: number;
  porcentajeAlerta: number;
  fechaInicio: string;
  fechaFin: string;
}

export interface RespuestaLimiteGasto {
  id: string;
  usuarioId: string;
  montoLimite: number;
  porcentajeAlerta: number;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
  fechaCreacion: string;
}

