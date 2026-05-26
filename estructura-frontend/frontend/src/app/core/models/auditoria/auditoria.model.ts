import { PaginaDTO } from '../shared/pagina.model';

export type EstadoAcceso = 'EXITO' | 'FALLO';

export interface AuditoriaAccesoRequestDTO {
  usuarioId?: string;
  ipOrigen: string;
  navegador?: string;
  estado: EstadoAcceso;
  detalleError?: string;
  fecha?: string;
}

export interface AuditoriaAccesoDTO {
  id: string;
  usuarioId: string;
  ipOrigen: string;
  navegador: string;
  estado: EstadoAcceso;
  detalleError: string;
  fecha: string;
}

export interface AuditoriaTransaccionalRequestDTO {
  usuarioId: string;
  servicioOrigen: string;
  entidadAfectada: string;
  entidadId: string;
  valorAnterior?: string;
  valorNuevo?: string;
  fecha?: string;
}

export interface AuditoriaTransaccionalDTO {
  id: string;
  usuarioId: string;
  servicioOrigen: string;
  entidadAfectada: string;
  entidadId: string;
  valorAnterior: string;
  valorNuevo: string;
  fecha: string;
}

export interface RegistroAuditoriaRequestDTO {
  fechaHora?: string;
  nombreUsuario: string;
  accion: string;
  modulo: string;
  ipOrigen?: string;
  detalles?: string;
}

export interface RegistroAuditoriaDTO {
  id: string;
  fechaHora: string;
  usuario: string;
  accion: string;
  modulo: string;
  ipOrigen: string;
  detalles: string;
}

export interface RespuestaVerificacionIpDTO {
  bloqueada: boolean;
}

export type PaginaAuditoriaAcceso = PaginaDTO<AuditoriaAccesoDTO>;
export type PaginaAuditoriaTransaccional = PaginaDTO<AuditoriaTransaccionalDTO>;
export type PaginaRegistroAuditoria = PaginaDTO<RegistroAuditoriaDTO>;

