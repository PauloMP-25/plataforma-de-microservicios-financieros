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
