import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import { PresupuestoDTO, SolicitudPresupuesto } from '../models/financiero/presupuesto.model';
import { ResultadoApi } from '../models/auth/user.model';

@Injectable({ providedIn: 'root' })
export class PresupuestoService {
  private base = `${environment.gatewayUrl}/api/v1/clientes/limites`;

  constructor(private http: HttpClient) {}

  crear(payload: SolicitudPresupuesto): Observable<PresupuestoDTO> {
    return this.http.post<ResultadoApi<PresupuestoDTO>>(this.base, payload).pipe(
      map(res => res.datos)
    );
  }

  obtenerActivo(): Observable<PresupuestoDTO | null> {
    return this.http.get<ResultadoApi<PresupuestoDTO>>(`${this.base}/activo`).pipe(
      map(res => res.datos),
      catchError(err => {
        if (err.status === 404) {
          return of(null);
        }
        return throwError(() => err);
      })
    );
  }

  actualizar(payload: SolicitudPresupuesto): Observable<PresupuestoDTO> {
    return this.http.patch<ResultadoApi<PresupuestoDTO>>(this.base, payload).pipe(
      map(res => res.datos)
    );
  }

  listarHistorial(): Observable<PresupuestoDTO[]> {
    return this.http.get<ResultadoApi<PresupuestoDTO[]>>(this.base).pipe(
      map(res => res.datos),
      catchError(() => of([]))
    );
  }

  eliminarActivo(): Observable<void> {
    return this.http.delete<ResultadoApi<void>>(this.base).pipe(
      map(() => undefined)
    );
  }
}

