export interface SolicitudMetaAhorro {
  nombre: string;
  montoObjetivo: number;
  montoActual?: number;
  fechaLimite: string;
}

export interface RespuestaMetaAhorro {
  id: string;
  nombre: string;
  montoObjetivo: number;
  montoActual: number;
  porcentajeProgreso: number;
  fechaLimite: string;
  completada: boolean;
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

