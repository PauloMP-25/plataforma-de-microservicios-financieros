import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import {
  RespuestaLimiteGasto,
  RespuestaMetaAhorro,
  SolicitudLimiteGasto,
  SolicitudMetaAhorro,
  Pagina
} from '../models/cliente/meta-limite.model';
import { ResultadoApi } from '../models/auth/user.model';

@Injectable({ providedIn: 'root' })
export class ClienteMetasLimitesService {
  private baseMetas = `${environment.gatewayUrl}/api/v1/clientes/metas`;
  private baseLimites = `${environment.gatewayUrl}/api/v1/clientes/limites`;

  constructor(private http: HttpClient) {}

  crearMeta(payload: SolicitudMetaAhorro): Observable<RespuestaMetaAhorro> {
    return this.http.post<ResultadoApi<RespuestaMetaAhorro>>(this.baseMetas, payload).pipe(
      map(res => res.datos)
    );
  }

  actualizarMeta(metaId: string, payload: SolicitudMetaAhorro): Observable<RespuestaMetaAhorro> {
    return this.http.put<ResultadoApi<RespuestaMetaAhorro>>(`${this.baseMetas}/${metaId}`, payload).pipe(
      map(res => res.datos)
    );
  }

  listarMetas(page: number = 0, size: number = 10): Observable<Pagina<RespuestaMetaAhorro>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ResultadoApi<Pagina<RespuestaMetaAhorro>>>(this.baseMetas, { params }).pipe(
      map(res => res.datos),
      catchError(() => {
        // Fallback vacío si falla para no romper la app
        return of({
          contenido: [],
          totalElementos: 0,
          totalPaginas: 0,
          tamañoPagina: size,
          numeroPagina: page,
          esUltima: true
        } as unknown as Pagina<RespuestaMetaAhorro>);
      })
    );
  }

  listarMetasActivas(page: number = 0, size: number = 10): Observable<Pagina<RespuestaMetaAhorro>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ResultadoApi<Pagina<RespuestaMetaAhorro>>>(`${this.baseMetas}/activas`, { params }).pipe(
      map(res => res.datos)
    );
  }

  obtenerMeta(metaId: string): Observable<RespuestaMetaAhorro> {
    return this.http.get<ResultadoApi<RespuestaMetaAhorro>>(`${this.baseMetas}/${metaId}`).pipe(
      map(res => res.datos)
    );
  }

  actualizarProgresoMeta(metaId: string, montoActual: number): Observable<RespuestaMetaAhorro> {
    return this.http.patch<ResultadoApi<RespuestaMetaAhorro>>(`${this.baseMetas}/${metaId}/progreso`, { montoActual }).pipe(
      map(res => res.datos)
    );
  }

  eliminarMeta(metaId: string): Observable<void> {
    return this.http.delete<ResultadoApi<void>>(`${this.baseMetas}/${metaId}`).pipe(
      map(() => undefined)
    );
  }

  crearLimite(payload: SolicitudLimiteGasto): Observable<RespuestaLimiteGasto> {
    return this.http.post<ResultadoApi<RespuestaLimiteGasto>>(this.baseLimites, payload).pipe(
      map(res => res.datos)
    );
  }

  obtenerLimiteActivo(): Observable<RespuestaLimiteGasto> {
    return this.http.get<ResultadoApi<RespuestaLimiteGasto>>(`${this.baseLimites}/activo`).pipe(
      map(res => res.datos)
    );
  }

  actualizarLimite(payload: SolicitudLimiteGasto): Observable<RespuestaLimiteGasto> {
    return this.http.patch<ResultadoApi<RespuestaLimiteGasto>>(this.baseLimites, payload).pipe(
      map(res => res.datos)
    );
  }

  listarHistorialLimites(): Observable<RespuestaLimiteGasto[]> {
    return this.http.get<ResultadoApi<RespuestaLimiteGasto[]>>(this.baseLimites).pipe(
      map(res => res.datos)
    );
  }

  desactivarLimiteActivo(): Observable<void> {
    return this.http.delete<ResultadoApi<void>>(this.baseLimites).pipe(
      map(() => undefined)
    );
  }
}

