import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, of, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { ClienteMetasLimitesService } from '../../../core/services/cliente-metas-limites.service';
import { Transacciones } from '../../../core/services/transacciones';
import { MetasUtilityService } from './metas-utility.service';
import { RespuestaMetaAhorro, SolicitudMetaAhorro } from '../../../core/models/cliente/meta-limite.model';
import { TransaccionRequestDTO } from '../../../core/models/financiero/transaccion.model';

@Injectable({
  providedIn: 'root'
})
export class MetasDataService {
  private metasService = inject(ClienteMetasLimitesService);
  private transaccionesService = inject(Transacciones);
  private metasUtility = inject(MetasUtilityService);

  listarMetas(page: number, size: number): Observable<any> {
    return this.metasService.listarMetas(page, size).pipe(
      catchError(err => {
        console.warn('API error listing metas, using mock fallback', err);
        const mockList = this.metasUtility.obtenerListaMockActual();
        return of({ content: mockList });
      })
    );
  }

  obtenerMeta(id: string): Observable<RespuestaMetaAhorro> {
    return this.metasService.obtenerMeta(id).pipe(
      catchError(err => {
        console.warn(`API error getting meta ${id}, using mock fallback`, err);
        const mock = this.metasUtility.obtenerListaMockActual().find(m => m.id === id);
        if (mock) {
          return of(mock);
        }
        return throwError(() => new Error('Meta no encontrada'));
      })
    );
  }

  crearMeta(payload: SolicitudMetaAhorro): Observable<RespuestaMetaAhorro> {
    return this.metasService.crearMeta(payload).pipe(
      catchError(err => {
        console.warn('API error creating meta, using mock fallback', err);
        const newMock = this.metasUtility.crearMockLocalmente(payload);
        return of(newMock);
      })
    );
  }

  actualizarMeta(id: string, payload: SolicitudMetaAhorro): Observable<any> {
    return this.metasService.actualizarMeta(id, payload).pipe(
      catchError(err => {
        console.warn(`API error updating meta ${id}, using mock fallback`, err);
        this.metasUtility.actualizarMockLocalmente(id, payload);
        return of({ id, ...payload });
      })
    );
  }

  eliminarMeta(id: string): Observable<any> {
    return this.metasService.eliminarMeta(id).pipe(
      tap(() => this.metasUtility.removerMockLocalmente(id)),
      catchError(err => {
        console.warn(`API error deleting meta ${id}, using mock fallback`, err);
        this.metasUtility.removerMockLocalmente(id);
        return of({ success: true });
      })
    );
  }

  completarMeta(meta: any): Observable<any> {
    return this.metasService.actualizarProgresoMeta(meta.id, meta.montoObjetivo).pipe(
      catchError(err => {
        console.warn(`API error completing meta ${meta.id}, using mock fallback`, err);
        this.metasUtility.marcarMockComoCompletadoLocalmente(meta.id, meta.montoObjetivo);
        return of({ success: true, isMock: true });
      })
    );
  }
}
