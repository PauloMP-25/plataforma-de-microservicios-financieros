import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { AuthService } from './auth.service';
import {
  TransaccionDTO,
  TransaccionRequestDTO,
  TransaccionFiltros,
} from '../models/financiero/transaccion.model';
import { PaginaDTO } from '../models/shared/pagina.model';

@Injectable({ providedIn: 'root' })
export class Transacciones {

  private base = `${environment.gatewayUrl}/api/v1/transacciones`;

  constructor(private http: HttpClient, private auth: AuthService) {}

  
  /*Registrar una transacción (ingreso o gasto)*/
  registrar(request: TransaccionRequestDTO): Observable<TransaccionDTO> {
    return this.http.post<TransaccionDTO>(this.base, request);
  }

  /* Registrar varias transacciones a la vez */
  registrarLote(requests: TransaccionRequestDTO[]): Observable<TransaccionDTO[]> {
    return this.http.post<TransaccionDTO[]>(`${this.base}/lote`, requests);
  }

  /**
   * Historial paginado con filtros opcionales.
   * — Módulo Gastos:
   *     listarHistorial({ tipo: 'GASTO' })
   * — Módulo Ingresos:
   *     listarHistorial({ tipo: 'INGRESO' })
   * — Dashboard (todos del mes):
   *     listarHistorial({ mes: 5, anio: 2025 })
   */
  listarHistorial(filtros: Partial<TransaccionFiltros> = {}): Observable<PaginaDTO<TransaccionDTO>> {
    const usuarioId = filtros.usuarioId ?? this.auth.usuario()?.id ?? '';
    let params = new HttpParams().set('usuarioId', usuarioId);

    if (filtros.tipo)         params = params.set('tipo',        filtros.tipo);
    if (filtros.categoriaId)  params = params.set('categoriaId', filtros.categoriaId);
    if (filtros.mes  != null) params = params.set('mes',         filtros.mes);
    if (filtros.anio != null) params = params.set('anio',        filtros.anio);
    params = params
      .set('pagina',  filtros.pagina  ?? 0)
      .set('tamanio', filtros.tamanio ?? 20);

    return this.http.get<PaginaDTO<TransaccionDTO>>(`${this.base}/historial`, { params });
  }

  /* Detalle de una transacción por ID */
  obtenerPorId(id: string): Observable<TransaccionDTO> {
    return this.http.get<TransaccionDTO>(`${this.base}/${id}`);
  }
}