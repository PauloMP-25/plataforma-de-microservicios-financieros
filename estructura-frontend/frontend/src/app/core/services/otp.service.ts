import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import {
  RespuestaGeneracion,
  RespuestaValidacion,
  SolicitudGenerarCodigo,
  SolicitudValidarCodigo
} from '../models/mensajeria/otp.model';

@Injectable({ providedIn: 'root' })
export class OtpService {
  private base = `${environment.gatewayUrl}/api/v1/mensajeria/otp`;

  constructor(private http: HttpClient) {}

  generarCodigo(payload: SolicitudGenerarCodigo): Observable<RespuestaGeneracion> {
    return this.http.post<RespuestaGeneracion>(`${this.base}/generar`, payload);
  }

  validarActivacion(payload: SolicitudValidarCodigo): Observable<RespuestaValidacion> {
    return this.http.post<RespuestaValidacion>(`${this.base}/validar-activacion`, payload);
  }

  validarRecuperacion(usuarioId: string, codigo: string): Observable<string> {
    const params = new HttpParams().set('usuarioId', usuarioId).set('codigo', codigo);
    return this.http.get(`${this.base}/validar-recuperacion`, { params, responseType: 'text' });
  }

  validarLimite(payload: SolicitudGenerarCodigo): Observable<void> {
    return this.http.post<void>(`${this.base}/validar-limite`, payload);
  }
}

