import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import { ResultadoApi } from '../models/auth/user.model';

export interface RespuestaCheckoutDTO {
  pagoId: string;
  urlCheckout: string;
  plan: string;
  monto: number;
  moneda: string;
}

export interface RespuestaSuscripcionDTO {
  plan: string;
  estado: string;
  monto: number;
  moneda: string;
  fechaVencimiento: string | null;
  activo: boolean;
}

@Injectable({ providedIn: 'root' })
export class SuscripcionService {
  private base = `${environment.gatewayUrl}/api/v1/pagos`;

  constructor(private http: HttpClient) {}

  crearSesionCheckout(plan: 'PRO' | 'PREMIUM'): Observable<RespuestaCheckoutDTO> {
    return this.http.post<ResultadoApi<RespuestaCheckoutDTO>>(`${this.base}/checkout`, { plan }).pipe(
      map(res => res.datos)
    );
  }

  obtenerMiSuscripcion(): Observable<RespuestaSuscripcionDTO> {
    return this.http.get<ResultadoApi<RespuestaSuscripcionDTO>>(`${this.base}/mi-suscripcion`).pipe(
      map(res => res.datos)
    );
  }
}
