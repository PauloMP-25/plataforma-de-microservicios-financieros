import { Injectable, inject, signal, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import { ResultadoApi } from '../models/auth/user.model';
import { AuthService } from './auth.service';
import {
  PeticionConFiltroFechaDTO,
  PeticionSimularMetaDTO,
  PeticionComparacionDTO,
  SolicitudClasificacionDTO,
  RespuestaModuloDTO,
  RespuestaClasificacionDTO,
  RespuestaIaDTO,
  SolicitudIaDTO
} from '../models/financiero/ia.model';

@Injectable({ providedIn: 'root' })
export class IaService {
  private base = `${environment.gatewayUrl}/api/v1/ia`;

  private readonly authService = inject(AuthService);

  readonly consultasRestantes = signal<number>(5);
  readonly consultasMaximas = signal<number>(5);
  readonly clasificacionesRestantes = signal<number>(2);
  readonly clasificacionesMaximas = signal<number>(2);

  constructor(private http: HttpClient) {
    effect(() => {
      const user = this.authService.usuario();
      if (user) {
        this.inicializarCuotasDesdePlan();
      } else {
        this.resetearCuotas();
      }
    });
  }

  private getPlanKey(idUsuario: string): string {
    return `luka_quota_plan_${idUsuario}`;
  }

  inicializarCuotasDesdePlan(): void {
    const user = this.authService.usuario();
    if (!user) return;

    let maxConsultas = 5;
    let maxClasificaciones = 2;

    if (this.authService.esPremium()) {
      maxConsultas = 50;
      maxClasificaciones = 20;
    } else if (this.authService.esPro()) {
      maxConsultas = 20;
      maxClasificaciones = 10;
    }

    this.consultasMaximas.set(maxConsultas);
    this.clasificacionesMaximas.set(maxClasificaciones);

    const planKey = this.getPlanKey(user.id);
    const cachedPlan = localStorage.getItem(planKey);
    const currentPlanName = this.authService.esPremium() ? 'PREMIUM' : (this.authService.esPro() ? 'PRO' : 'FREE');

    if (cachedPlan !== currentPlanName) {
      this.consultasRestantes.set(maxConsultas);
      this.clasificacionesRestantes.set(maxClasificaciones);
      localStorage.setItem(planKey, currentPlanName);
      this.guardarCuotasEnStorage(user.id);
    } else {
      const savedConsultas = localStorage.getItem(`luka_quota_consultas_${user.id}`);
      const savedClasificaciones = localStorage.getItem(`luka_quota_clasificaciones_${user.id}`);

      if (savedConsultas !== null) {
        this.consultasRestantes.set(Math.min(parseInt(savedConsultas, 10), maxConsultas));
      } else {
        this.consultasRestantes.set(maxConsultas);
      }

      if (savedClasificaciones !== null) {
        this.clasificacionesRestantes.set(Math.min(parseInt(savedClasificaciones, 10), maxClasificaciones));
      } else {
        this.clasificacionesRestantes.set(maxClasificaciones);
      }
    }
  }

  descontarConsulta(): void {
    const user = this.authService.usuario();
    if (!user) return;
    const current = this.consultasRestantes();
    if (current > 0) {
      this.consultasRestantes.set(current - 1);
      this.guardarCuotasEnStorage(user.id);
    }
  }

  descontarClasificacion(): void {
    const user = this.authService.usuario();
    if (!user) return;
    const current = this.clasificacionesRestantes();
    if (current > 0) {
      this.clasificacionesRestantes.set(current - 1);
      this.guardarCuotasEnStorage(user.id);
    }
  }

  private guardarCuotasEnStorage(userId: string): void {
    localStorage.setItem(`luka_quota_consultas_${userId}`, this.consultasRestantes().toString());
    localStorage.setItem(`luka_quota_clasificaciones_${userId}`, this.clasificacionesRestantes().toString());
  }

  private resetearCuotas(): void {
    this.consultasRestantes.set(5);
    this.consultasMaximas.set(5);
    this.clasificacionesRestantes.set(2);
    this.clasificacionesMaximas.set(2);
  }

  // Compatibilidad con código antiguo si lo hubiera
  consultar(payload: SolicitudIaDTO): Observable<RespuestaIaDTO> {
    return this.http.post<RespuestaIaDTO>(`${this.base}/consultar`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  // ── Módulo de Análisis ──
  
  getGastoHormiga(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/gasto-hormiga`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  getPredecirGastos(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/predecir-gastos`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  getHabitosFinancieros(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/habitos-financieros`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  getEstiloVida(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/estilo-vida`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  getReporteCompleto(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/reporte-completo`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  getComprobadorEvolucion(payload: PeticionComparacionDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/comprobador-evolucion`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  // ── Módulo de Coach ──

  getSimularMeta(payload: PeticionSimularMetaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/simular-meta`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  getRetoAhorro(payload: PeticionConFiltroFechaDTO): Observable<ResultadoApi<RespuestaModuloDTO>> {
    return this.http.post<ResultadoApi<RespuestaModuloDTO>>(`${this.base}/reto-ahorro`, payload).pipe(
      tap(() => this.descontarConsulta())
    );
  }

  // ── Módulo de Clasificación ──

  getClasificarTransaccion(payload: SolicitudClasificacionDTO): Observable<ResultadoApi<RespuestaClasificacionDTO>> {
    return this.http.post<ResultadoApi<RespuestaClasificacionDTO>>(`${this.base}/clasificar-transaccion`, payload).pipe(
      tap(() => this.descontarClasificacion())
    );
  }
}
