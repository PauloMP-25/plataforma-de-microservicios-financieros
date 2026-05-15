import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import {
  RespuestaLimiteGasto,
  RespuestaMetaAhorro,
  SolicitudLimiteGasto,
  SolicitudMetaAhorro
} from '../models/cliente/meta-limite.model';

@Injectable({ providedIn: 'root' })
export class ClienteMetasLimitesService {
  private baseMetas = `${environment.gatewayUrl}/api/v1/clientes/metas`;
  private baseLimites = `${environment.gatewayUrl}/api/v1/clientes/limites`;

  constructor(private http: HttpClient) {}

  crearMeta(payload: SolicitudMetaAhorro): Observable<RespuestaMetaAhorro> {
    return this.http.post<RespuestaMetaAhorro>(this.baseMetas, payload);
  }

  listarMetas(): Observable<RespuestaMetaAhorro[]> {
    return this.http.get<RespuestaMetaAhorro[]>(this.baseMetas);
  }

  listarMetasActivas(): Observable<RespuestaMetaAhorro[]> {
    return this.http.get<RespuestaMetaAhorro[]>(`${this.baseMetas}/activas`);
  }

  obtenerMeta(metaId: string): Observable<RespuestaMetaAhorro> {
    return this.http.get<RespuestaMetaAhorro>(`${this.baseMetas}/${metaId}`);
  }

  actualizarProgresoMeta(metaId: string, montoActual: number): Observable<RespuestaMetaAhorro> {
    const params = new HttpParams().set('montoActual', montoActual);
    return this.http.patch<RespuestaMetaAhorro>(`${this.baseMetas}/${metaId}/progreso`, null, { params });
  }

  eliminarMeta(metaId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseMetas}/${metaId}`);
  }

  crearLimite(payload: SolicitudLimiteGasto): Observable<RespuestaLimiteGasto> {
    return this.http.post<RespuestaLimiteGasto>(this.baseLimites, payload);
  }

  obtenerLimiteActivo(): Observable<RespuestaLimiteGasto> {
    return this.http.get<RespuestaLimiteGasto>(`${this.baseLimites}/activo`);
  }

  actualizarLimite(payload: SolicitudLimiteGasto): Observable<RespuestaLimiteGasto> {
    return this.http.patch<RespuestaLimiteGasto>(this.baseLimites, payload);
  }

  listarHistorialLimites(): Observable<RespuestaLimiteGasto[]> {
    return this.http.get<RespuestaLimiteGasto[]>(this.baseLimites);
  }

  desactivarLimiteActivo(): Observable<void> {
    return this.http.delete<void>(this.baseLimites);
  }
}

