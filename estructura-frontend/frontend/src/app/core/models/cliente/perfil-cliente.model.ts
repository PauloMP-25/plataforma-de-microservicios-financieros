export interface SolicitudDatosPersonales {
  dni: string;
  nombres: string;
  apellidos: string;
  genero: string;
  edad: number;
  telefono: string;
  fotoPerfilUrl?: string;
  direccion?: string;
  ciudad?: string;
}

export interface RespuestaDatosPersonales {
  dni: string;
  nombres: string;
  apellidos: string;
  genero: string;
  edad: number;
  telefono: string;
  fotoPerfilUrl: string;
  direccion: string;
  ciudad: string;
  datosCompletos: boolean;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface SolicitudPerfilFinanciero {
  ocupacion: string;
  ingresoMensual: number;
  estiloVida: string;
  tonoIA: string;
}

export interface RespuestaPerfilFinanciero {
  ocupacion: string;
  ingresoMensual: number;
  estiloVida: string;
  tonoIA: string;
  fechaCreacion: string;
  fechaActualizacion: string;
}

