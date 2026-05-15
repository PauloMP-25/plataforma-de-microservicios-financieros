import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import {
  RespuestaDatosPersonales,
  RespuestaPerfilFinanciero,
  SolicitudDatosPersonales,
  SolicitudPerfilFinanciero
} from '../models/cliente/perfil-cliente.model';

@Injectable({ providedIn: 'root' })
export class ClientePerfilService {
  private basePerfil = `${environment.gatewayUrl}/api/v1/clientes/perfil`;
  private basePerfilFinanciero = `${environment.gatewayUrl}/api/v1/clientes/perfil-financiero`;

  constructor(private http: HttpClient) {}

  crearPerfilInicial(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.http.post<RespuestaDatosPersonales>(`${this.basePerfil}/inicial?usuarioId=${usuarioId}`, {});
  }

  consultarPerfil(usuarioId: string): Observable<RespuestaDatosPersonales> {
    return this.http.get<RespuestaDatosPersonales>(`${this.basePerfil}/${usuarioId}`);
  }

  actualizarPerfil(usuarioId: string, payload: SolicitudDatosPersonales): Observable<RespuestaDatosPersonales> {
    return this.http.put<RespuestaDatosPersonales>(`${this.basePerfil}/${usuarioId}`, payload);
  }

  consultarPerfilFinanciero(usuarioId: string): Observable<RespuestaPerfilFinanciero> {
    return this.http.get<RespuestaPerfilFinanciero>(`${this.basePerfilFinanciero}/${usuarioId}`);
  }

  guardarPerfilFinanciero(usuarioId: string, payload: SolicitudPerfilFinanciero): Observable<RespuestaPerfilFinanciero> {
    return this.http.put<RespuestaPerfilFinanciero>(`${this.basePerfilFinanciero}/${usuarioId}`, payload);
  }
}

