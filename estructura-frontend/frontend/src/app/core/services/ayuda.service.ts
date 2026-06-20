import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';

export interface RespuestaSoporte {
  exito: boolean;
  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class AyudaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.gatewayUrl}/api/v1/mensajeria/soporte`;

  /**
   * Envía una consulta de soporte al backend.
   * Utiliza FormData para permitir la carga opcional de adjuntos.
   * @param formData Debe contener la parte "solicitud" (JSON Blob) y opcionalmente "adjunto" (File).
   */
  enviarContacto(formData: FormData): Observable<RespuestaSoporte> {
    return this.http.post<RespuestaSoporte>(`${this.baseUrl}/contacto`, formData);
  }

  /**
   * Envía un reporte de problema al backend.
   * Utiliza FormData para permitir la carga opcional de adjuntos.
   * @param formData Debe contener la parte "solicitud" (JSON Blob) y opcionalmente "adjunto" (File).
   */
  enviarReporte(formData: FormData): Observable<RespuestaSoporte> {
    return this.http.post<RespuestaSoporte>(`${this.baseUrl}/reporte`, formData);
  }
}
