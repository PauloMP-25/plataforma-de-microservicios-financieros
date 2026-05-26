export type TipoVerificacion = 'EMAIL' | 'SMS';

export type PropositoCodigo = 'ACTIVACION_CUENTA' | 'RESET_PASSWORD';

export interface SolicitudGenerarCodigo {
  usuarioId: string;
  email: string;
  telefono: string;
  tipo: TipoVerificacion;
  proposito: PropositoCodigo;
}

export interface SolicitudValidarCodigo {
  usuarioId: string;
  codigo: string;
}

export interface RespuestaGeneracion {
  exito: boolean;
  mensaje: string;
  tipo: string;
}

export interface RespuestaValidacion {
  exito: boolean;
  mensaje: string;
}

