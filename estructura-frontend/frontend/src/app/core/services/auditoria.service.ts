import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import {
  AuditoriaAccesoDTO,
  AuditoriaAccesoRequestDTO,
  AuditoriaTransaccionalDTO,
  AuditoriaTransaccionalRequestDTO,
  PaginaAuditoriaAcceso,
  PaginaAuditoriaTransaccional,
  PaginaRegistroAuditoria,
  RegistroAuditoriaDTO,
  RegistroAuditoriaRequestDTO,
  RespuestaVerificacionIpDTO
} from '../models/auditoria/auditoria.model';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private base = `${environment.gatewayUrl}/api/v1/auditoria`;

  constructor(private http: HttpClient) {}

  registrarAcceso(payload: AuditoriaAccesoRequestDTO): Observable<AuditoriaAccesoDTO> {
    return this.http.post<AuditoriaAccesoDTO>(`${this.base}/accesos`, payload);
  }

  listarAccesos(pagina = 0, tamanio = 20): Observable<PaginaAuditoriaAcceso> {
    const params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    return this.http.get<PaginaAuditoriaAcceso>(`${this.base}/accesos`, { params });
  }

  verificarIp(ip: string): Observable<RespuestaVerificacionIpDTO> {
    return this.http.get<RespuestaVerificacionIpDTO>(`${this.base}/verificar-ip/${ip}`);
  }

  registrarTransaccion(payload: AuditoriaTransaccionalRequestDTO): Observable<AuditoriaTransaccionalDTO> {
    return this.http.post<AuditoriaTransaccionalDTO>(`${this.base}/transacciones`, payload);
  }

  listarTransacciones(filtros: {
    servicioOrigen?: string;
    desde?: string;
    hasta?: string;
    pagina?: number;
    tamanio?: number;
  } = {}): Observable<PaginaAuditoriaTransaccional> {
    let params = new HttpParams()
      .set('pagina', filtros.pagina ?? 0)
      .set('tamanio', filtros.tamanio ?? 20);

    if (filtros.servicioOrigen) params = params.set('servicioOrigen', filtros.servicioOrigen);
    if (filtros.desde) params = params.set('desde', filtros.desde);
    if (filtros.hasta) params = params.set('hasta', filtros.hasta);

    return this.http.get<PaginaAuditoriaTransaccional>(`${this.base}/transacciones`, { params });
  }

  registrarEvento(payload: RegistroAuditoriaRequestDTO): Observable<RegistroAuditoriaDTO> {
    return this.http.post<RegistroAuditoriaDTO>(`${this.base}/registrar`, payload);
  }

  listarRegistros(filtros: {
    modulo?: string;
    nivel?: string;
    pagina?: number;
    tamanio?: number;
  } = {}): Observable<PaginaRegistroAuditoria> {
    let params = new HttpParams()
      .set('pagina', filtros.pagina ?? 0)
      .set('tamanio', filtros.tamanio ?? 20);

    if (filtros.modulo) params = params.set('modulo', filtros.modulo);
    if (filtros.nivel) params = params.set('nivel', filtros.nivel);

    return this.http.get<PaginaRegistroAuditoria>(`${this.base}/registros`, { params });
  }
}

