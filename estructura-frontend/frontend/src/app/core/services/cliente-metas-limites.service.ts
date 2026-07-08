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
    return this.http.get<ResultadoApi<any>>(this.baseMetas, { params }).pipe(
      map(res => {
        const datos = res.datos;
        if (!datos) {
          throw new Error('Sin datos en la respuesta de metas');
        }
        const content = datos.contenido || datos.content || [];
        return {
          content: content,
          totalElements: datos.totalElementos !== undefined ? datos.totalElementos : (datos.totalElements || content.length),
          totalPages: datos.totalPaginas !== undefined ? datos.totalPaginas : (datos.totalPages || 1),
          size: datos.tamañoPagina !== undefined ? datos.tamañoPagina : (datos.size || size),
          number: datos.numeroPagina !== undefined ? datos.numeroPagina : (datos.number || page)
        } as Pagina<RespuestaMetaAhorro>;
      }),
      catchError(() => {
        // Fallback a localStorage
        const localMetasStr = localStorage.getItem('luka_mock_metas');
        let list: RespuestaMetaAhorro[] = [];
        if (localMetasStr) {
          try {
            list = JSON.parse(localMetasStr);
          } catch (e) {
            console.error('Error parseando luka_mock_metas:', e);
          }
        }
        
        // Si no hay nada en localStorage, inicializar con mocks por defecto para pruebas fluidas
        if (list.length === 0) {
          list = [
            {
              id: 'mock-meta-1',
              nombre: '[Viaje] Viaje a Cancún',
              montoObjetivo: 2000,
              montoActual: 2000,
              porcentajeProgreso: 100,
              fechaObjetivo: '2026-11-29',
              fechaInicio: '2025-01-15',
              completada: true,
              fechaCreacion: '2025-01-15',
              fechaActualizacion: '2026-11-29'
            },
            {
              id: 'mock-meta-2',
              nombre: '[Tecnología] Laptop',
              montoObjetivo: 300,
              montoActual: 300,
              porcentajeProgreso: 100,
              fechaObjetivo: '2025-08-15',
              fechaInicio: '2024-10-10',
              completada: true,
              fechaCreacion: '2024-10-10',
              fechaActualizacion: '2025-08-15'
            },
            {
              id: 'mock-meta-3',
              nombre: '[Auto] Auto',
              montoObjetivo: 5000,
              montoActual: 1700,
              porcentajeProgreso: 34,
              fechaObjetivo: '2026-03-10',
              fechaInicio: '2025-02-01',
              completada: false,
              fechaCreacion: '2025-02-01',
              fechaActualizacion: '2025-02-01'
            },
            {
              id: 'mock-meta-4',
              nombre: '[Estudios] Estudios',
              montoObjetivo: 5100,
              montoActual: 1700,
              porcentajeProgreso: 33,
              fechaObjetivo: '2027-04-20',
              fechaInicio: '2025-01-20',
              completada: false,
              fechaCreacion: '2025-01-20',
              fechaActualizacion: '2025-01-20'
            },
            {
              id: 'mock-meta-5',
              nombre: '[Tecnología] Nuevo Celular',
              montoObjetivo: 1500,
              montoActual: 850,
              porcentajeProgreso: 57,
              fechaObjetivo: '2026-05-05',
              fechaInicio: '2025-03-01',
              completada: false,
              fechaCreacion: '2025-03-01',
              fechaActualizacion: '2025-03-01'
            },
            {
              id: 'mock-meta-6',
              nombre: '[Otros] Muebles',
              montoObjetivo: 2500,
              montoActual: 250,
              porcentajeProgreso: 10,
              fechaObjetivo: '2025-09-20',
              fechaInicio: '2024-12-01',
              completada: false,
              fechaCreacion: '2024-12-01',
              fechaActualizacion: '2024-12-01'
            },
            {
              id: 'mock-meta-7',
              nombre: '[Emergencia] Fondo de Emergencia',
              montoObjetivo: 1000,
              montoActual: 300,
              porcentajeProgreso: 30,
              fechaObjetivo: '2026-12-31',
              fechaInicio: '2025-04-10',
              completada: false,
              fechaCreacion: '2025-04-10',
              fechaActualizacion: '2025-04-10'
            }
          ];
          localStorage.setItem('luka_mock_metas', JSON.stringify(list));
        }

        const start = page * size;
        const end = start + size;
        const paginatedList = list.slice(start, end);

        return of({
          content: paginatedList,
          totalElements: list.length,
          totalPages: Math.ceil(list.length / size),
          size: size,
          number: page
        } as Pagina<RespuestaMetaAhorro>);
      })
    );
  }

  listarMetasActivas(page: number = 0, size: number = 10): Observable<Pagina<RespuestaMetaAhorro>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ResultadoApi<any>>(`${this.baseMetas}/activas`, { params }).pipe(
      map(res => {
        const datos = res.datos;
        if (!datos) {
          throw new Error('Sin datos en la respuesta de metas activas');
        }
        const content = datos.contenido || datos.content || [];
        return {
          content: content,
          totalElements: datos.totalElementos !== undefined ? datos.totalElementos : (datos.totalElements || content.length),
          totalPages: datos.totalPaginas !== undefined ? datos.totalPaginas : (datos.totalPages || 1),
          size: datos.tamañoPagina !== undefined ? datos.tamañoPagina : (datos.size || size),
          number: datos.numeroPagina !== undefined ? datos.numeroPagina : (datos.number || page)
        } as Pagina<RespuestaMetaAhorro>;
      }),
      catchError(() => {
        // Fallback a localStorage
        const localMetasStr = localStorage.getItem('luka_mock_metas');
        let list: RespuestaMetaAhorro[] = [];
        if (localMetasStr) {
          try {
            list = JSON.parse(localMetasStr).filter((m: any) => !m.completada);
          } catch (e) {
            console.error('Error parseando metas activas:', e);
          }
        }
        const start = page * size;
        const end = start + size;
        const paginatedList = list.slice(start, end);
        return of({
          content: paginatedList,
          totalElements: list.length,
          totalPages: Math.ceil(list.length / size),
          size: size,
          number: page
        } as Pagina<RespuestaMetaAhorro>);
      })
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

