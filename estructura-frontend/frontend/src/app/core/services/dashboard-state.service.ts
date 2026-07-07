import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { ResultadoApi } from '../models/auth/user.model';
import { 
  DashboardResumenDTO, 
  CashflowPointDTO, 
  CategoriaDistribucionDTO,
  HeatmapPointDTO,
  MetaProgressDTO,
  ComparativaMensualDTO,
  DashboardAnaliticaDTO
} from '../models/dashboard/dashboard.model';

export interface DashboardFiltros {
  fechaInicio?: string;
  fechaFin?: string;
  metodoPago?: string;
  tipoMovimiento?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardStateService {
  private base = `${environment.gatewayUrl}/api/v1/ia/dashboard`;

  // ── Angular Signals de Estado ──
  readonly resumen = signal<DashboardResumenDTO | null>(null);
  readonly resumenYTD = signal<DashboardResumenDTO | null>(null); // Resumen YTD anual fijo para el Header
  readonly flujoCaja = signal<CashflowPointDTO[]>([]);
  readonly distribucionGastos = signal<CategoriaDistribucionDTO[]>([]);
  readonly heatmap = signal<HeatmapPointDTO[]>([]);
  readonly metas = signal<MetaProgressDTO[]>([]);
  readonly comparativa = signal<ComparativaMensualDTO[]>([]);
  readonly transaccionesMetodo = signal<{ metodo: string, cantidad: number, color: string }[]>([]);

  // ── Filtros Actuales ──
  readonly filtrosActuales = signal<DashboardFiltros>({});

  // ── Estados Auxiliares ──
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  constructor(private http: HttpClient) {}

  marcarForzarRefresco(): void {
    // Deprecated o ignorado para V2, retenido por compatibilidad con Auth
  }

  private initialLoadDone = false;

  /**
   * Carga los datos analíticos usando forkJoin para obtener KPIs y Gráficos del microservicio-ia
   */
  cargarAnalitica(filtros?: DashboardFiltros): void {
    if (this.initialLoadDone && !filtros) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    if (filtros) {
      const actuales = this.filtrosActuales();
      if (
        actuales.fechaInicio === filtros.fechaInicio &&
        actuales.fechaFin === filtros.fechaFin &&
        actuales.metodoPago === filtros.metodoPago &&
        actuales.tipoMovimiento === filtros.tipoMovimiento &&
        this.initialLoadDone
      ) {
        this.loading.set(false);
        return;
      }
      this.filtrosActuales.set(filtros);
    }

    let params = new HttpParams();
    const currentFilters = this.filtrosActuales();
    
    if (currentFilters.fechaInicio) params = params.set('fechaInicio', currentFilters.fechaInicio);
    if (currentFilters.fechaFin) params = params.set('fechaFin', currentFilters.fechaFin);
    if (currentFilters.metodoPago) params = params.set('metodoPago', currentFilters.metodoPago);
    if (currentFilters.tipoMovimiento) params = params.set('tipoMovimiento', currentFilters.tipoMovimiento);

    const hasFilters = !!(currentFilters.fechaInicio || currentFilters.fechaFin || currentFilters.metodoPago || currentFilters.tipoMovimiento);

    const requests: any = {
      kpis: this.http.get<ResultadoApi<any>>(`${this.base}/kpis`, { params }),
      graficos: this.http.get<ResultadoApi<any>>(`${this.base}/graficos`, { params })
    };

    // Cargar resumenYTD si es la carga inicial o si se invalidó la caché
    const cargarYTD = !this.initialLoadDone || !this.resumenYTD();
    if (cargarYTD) {
      requests.kpisYTD = this.http.get<ResultadoApi<any>>(`${this.base}/kpis`);
    }

    forkJoin(requests).subscribe({
      next: (res: any) => {
        const kpis = res.kpis;
        const graficos = res.graficos;
        const kpisYTD = res.kpisYTD;

        if (kpis.exito && kpis.datos) {
          const resumenBackend = kpis.datos.resumen;
          this.resumen.set({
            desde: resumenBackend.desde,
            hasta: resumenBackend.hasta,
            tasaAhorro: resumenBackend.tasaAhorro,
            gastoPromedioDiario: resumenBackend.gastoPromedioDiario || 0, 
            cumplimientoPresupuesto: resumenBackend.cumplimientoPresupuesto ?? 0, 
            proyeccionFinDeMes: resumenBackend.proyeccionFinDeMes || 0,
            totalIngresos: resumenBackend.totalIngresos,
            totalGastos: resumenBackend.totalGastos,
            balance: resumenBackend.balance,
            volumenTransacciones: resumenBackend.volumenTransacciones || 0
          });
        } else {
          this.error.set(kpis.mensaje || 'Error al cargar KPIs');
        }

        const ytdData = kpisYTD ? kpisYTD : (hasFilters ? null : kpis);
        if (ytdData && ytdData.exito && ytdData.datos) {
          const resumenBackend = ytdData.datos.resumen;
          this.resumenYTD.set({
            desde: resumenBackend.desde,
            hasta: resumenBackend.hasta,
            tasaAhorro: resumenBackend.tasaAhorro,
            gastoPromedioDiario: resumenBackend.gastoPromedioDiario || 0, 
            cumplimientoPresupuesto: resumenBackend.cumplimientoPresupuesto ?? 0, 
            proyeccionFinDeMes: resumenBackend.proyeccionFinDeMes || 0,
            totalIngresos: resumenBackend.totalIngresos,
            totalGastos: resumenBackend.totalGastos,
            balance: resumenBackend.balance,
            volumenTransacciones: resumenBackend.volumenTransacciones || 0
          });
        }

        if (graficos.exito && graficos.datos) {
          this.flujoCaja.set(graficos.datos.flujoCaja || []);

          const dist = (graficos.datos.distribucionGastos || [])
            .sort((a: any, b: any) => b.total - a.total)
            .slice(0, 5);
          this.distribucionGastos.set(dist);

          // Heatmap: gastos por día de semana
          this.heatmap.set(graficos.datos.heatmap || []);

          // Comparativa histórica: año actual vs año anterior
          this.comparativa.set(graficos.datos.comparativa || []);

          // Métodos de pago
          this.transaccionesMetodo.set(graficos.datos.transaccionesMetodo || []);
        }

        // Metas: no vienen aún del backend — dejar vacío por ahora
        this.metas.set([]);

        this.loading.set(false);
        this.initialLoadDone = true;
      },
      error: (err) => {
        console.error('[DashboardStateService] Error de conexión con backend:', err);
        this.error.set('No se pudo conectar con el microservicio de IA');
        this.loading.set(false);
        this.initialLoadDone = true;
      }
    });
  }

  invalidarCache(): void {
    this.initialLoadDone = false; 
    this.cargarAnalitica(this.filtrosActuales());
  }

  limpiarEstado(): void {
    this.resumen.set(null);
    this.resumenYTD.set(null);
    this.flujoCaja.set([]);
    this.distribucionGastos.set([]);
    this.heatmap.set([]);
    this.metas.set([]);
    this.comparativa.set([]);
    this.transaccionesMetodo.set([]);
    this.filtrosActuales.set({});
    this.loading.set(false);
    this.error.set(null);
  }
}
