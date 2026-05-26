import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { PresupuestoDTO, SolicitudPresupuesto } from '../models/financiero/presupuesto.model';

@Injectable({ providedIn: 'root' })
export class PresupuestoService {
  private base = `${environment.gatewayUrl}/api/v1/clientes/limites`;

  constructor(private http: HttpClient) {}

  crear(payload: SolicitudPresupuesto): Observable<PresupuestoDTO> {
    return this.http.post<PresupuestoDTO>(this.base, payload);
  }

  obtenerActivo(): Observable<PresupuestoDTO> {
    return this.http.get<PresupuestoDTO>(`${this.base}/activo`);
  }

  actualizar(payload: SolicitudPresupuesto): Observable<PresupuestoDTO> {
    return this.http.patch<PresupuestoDTO>(this.base, payload);
  }

  listarHistorial(): Observable<PresupuestoDTO[]> {
    return this.http.get<PresupuestoDTO[]>(this.base);
  }

  eliminarActivo(): Observable<void> {
    return this.http.delete<void>(this.base);
  }
}

