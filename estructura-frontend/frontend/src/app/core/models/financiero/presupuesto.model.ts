export interface SolicitudPresupuesto {
  montoLimite: number;
  porcentajeAlerta: number;
  fechaInicio: string;
  fechaFin: string;
}

export interface PresupuestoDTO {
  id: string;
  usuarioId: string;
  montoLimite: number;
  porcentajeAlerta: number;
  fechaInicio: string;
  fechaFin: string;
  activo: boolean;
  fechaCreacion: string;
}

