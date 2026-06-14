export interface SolicitudPresupuesto {
  nombre?: string;
  montoLimite: number;
  porcentajeAlerta: number;
  fechaInicio: string;
  fechaFin: string;
}

export interface PresupuestoDTO {
  id: string;
  usuarioId: string;
  nombre: string;
  montoLimite: number;
  porcentajeAlerta: number;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
  fechaCreacion: string;
}

