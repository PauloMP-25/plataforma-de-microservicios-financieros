import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { ResultadoApi } from '../models/auth/user.model';
import {
  PeticionConFiltroFechaDTO,
  PeticionSimularMetaDTO,
  SolicitudClasificacionDTO,
  RespuestaModuloDTO,
  RespuestaClasificacionDTO,
  RespuestaIaDTO,
  SolicitudIaDTO
} from '../models/financiero/ia.model';

@Injectable({ providedIn: 'root' })
export class IaService {
  private base = `${environment.gatewayUrl}/api/v1/ia`;

  constructor(private http: HttpClient) {}

  // Compatibilidad con código antiguo si lo hubiera
  consultar(payload: SolicitudIaDTO): Observable<RespuestaIaDTO> {
    return this.http.post<RespuestaIaDTO>(`${this.base}/consultar`, payload);
  }

  // ── Módulo de Análisis ──
  
  getGastoHormiga(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/gasto-hormiga`, payload);
  }

  getPredecirGastos(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/predecir-gastos`, payload);
  }

  getHabitosFinancieros(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/habitos-financieros`, payload);
  }

  getEstiloVida(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/estilo-vida`, payload);
  }

  getReporteCompleto(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/reporte-completo`, payload);
  }

  // ── Módulo de Coach ──

  getSimularMeta(payload: PeticionSimularMetaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/simular-meta`, payload);
  }

  getRetoAhorro(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/reto-ahorro`, payload);
  }

  // ── Módulo de Clasificación ──

  getClasificarTransaccion(payload: SolicitudClasificacionDTO): Observable<ResultadoApi<RespuestaClasificacionDTO>> {
    return this.http.post<ResultadoApi<RespuestaClasificacionDTO>>(`${this.base}/clasificar-transaccion`, payload);
  }
}
