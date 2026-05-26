import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { RespuestaIaDTO, SolicitudIaDTO } from '../models/financiero/ia.model';

@Injectable({ providedIn: 'root' })
export class IaService {
  private base = `${environment.gatewayUrl}/api/v1/ia`;

  constructor(private http: HttpClient) {}

  consultar(payload: SolicitudIaDTO): Observable<RespuestaIaDTO> {
    return this.http.post<RespuestaIaDTO>(`${this.base}/consultar`, payload);
  }
}

