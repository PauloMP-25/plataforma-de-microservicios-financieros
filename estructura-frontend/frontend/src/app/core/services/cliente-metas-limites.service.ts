import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
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
    return this.http.get<RespuestaMetaAhorro[]>(this.baseMetas).pipe(
      catchError(() => {
        // Mocks si falla el backend
        return of([
          {
            id: 'mock-meta-1',
            nombre: 'Fondo de Emergencia',
            montoObjetivo: 5000,
            montoActual: 2500,
            porcentajeProgreso: 50,
            fechaLimite: new Date(new Date().getFullYear(), 11, 31).toISOString().split('T')[0],
            completada: false,
            fechaCreacion: new Date().toISOString(),
            fechaActualizacion: new Date().toISOString()
          },
          {
            id: 'mock-meta-2',
            nombre: 'Nueva Laptop',
            montoObjetivo: 3500,
            montoActual: 3500,
            porcentajeProgreso: 100,
            fechaLimite: new Date(new Date().getFullYear(), new Date().getMonth(), 15).toISOString().split('T')[0],
            completada: true,
            fechaCreacion: new Date().toISOString(),
            fechaActualizacion: new Date().toISOString()
          }
        ] as RespuestaMetaAhorro[]);
      })
    );
  }

  listarMetasActivas(): Observable<RespuestaMetaAhorro[]> {
    return this.http.get<RespuestaMetaAhorro[]>(`${this.baseMetas}/activas`);
  }

  obtenerMeta(metaId: string): Observable<RespuestaMetaAhorro> {
    return this.http.get<RespuestaMetaAhorro>(`${this.baseMetas}/${metaId}`);
  }

  actualizarProgresoMeta(metaId: string, montoActual: number): Observable<RespuestaMetaAhorro> {
    return this.http.patch<RespuestaMetaAhorro>(`${this.baseMetas}/${metaId}/progreso`, { montoActual });
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

