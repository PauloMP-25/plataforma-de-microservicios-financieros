import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
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
    return this.http.get<PresupuestoDTO>(`${this.base}/activo`).pipe(
      catchError(() => {
        // Mock si falla la conexión
        const hoy = new Date();
        return of({
          id: 'mock-presupuesto-1',
          usuarioId: 'usuario-1',
          montoLimite: 3500,
          porcentajeAlerta: 80,
          fechaInicio: new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString(),
          fechaFin: new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).toISOString(),
          activo: true,
          fechaCreacion: new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString()
        } as PresupuestoDTO);
      })
    );
  }

  actualizar(payload: SolicitudPresupuesto): Observable<PresupuestoDTO> {
    return this.http.patch<PresupuestoDTO>(this.base, payload);
  }

  listarHistorial(): Observable<PresupuestoDTO[]> {
    return this.http.get<PresupuestoDTO[]>(this.base).pipe(
      catchError(() => of([]))
    );
  }

  eliminarActivo(): Observable<void> {
    return this.http.delete<void>(this.base);
  }
}

