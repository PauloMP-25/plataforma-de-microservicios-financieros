import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { ResultadoApi } from '../models/auth/user.model';
import { 
  DashboardResumenDTO, 
  CashflowPointDTO, 
  CategoriaDistribucionDTO,
  FijoVariableDTO,
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
  private base = `${environment.gatewayUrl}/api/v1/dashboard`;

  // ── Angular Signals de Estado ──
  readonly resumen = signal<DashboardResumenDTO | null>(null);
  readonly flujoCaja = signal<CashflowPointDTO[]>([]);
  readonly distribucionGastos = signal<CategoriaDistribucionDTO[]>([]);
  readonly fijoVariable = signal<FijoVariableDTO[]>([]);
  readonly heatmap = signal<HeatmapPointDTO[]>([]);
  readonly metas = signal<MetaProgressDTO[]>([]);
  readonly comparativa = signal<ComparativaMensualDTO[]>([]);

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
   * Carga los datos analíticos enriquecidos pasando los filtros al backend.
   */
  cargarAnalitica(filtros?: DashboardFiltros): void {
    // Si ya cargó y no se enviaron filtros nuevos, evitamos múltiples llamadas innecesarias
    if (this.initialLoadDone && !filtros) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    // Actualizamos el signal de filtros si vienen nuevos
    if (filtros) {
      // Comparar si son iguales (simplificado) para evitar re-fetch si no cambiaron
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

    this.http.get<ResultadoApi<DashboardAnaliticaDTO>>(`${this.base}/analitica-avanzada`, { params }).subscribe({
      next: (resp) => {
        if (resp.exito && resp.datos) {
          const d = resp.datos;
          this.resumen.set(d.resumen);
          this.flujoCaja.set(d.flujoCaja || []);
          
          // Tomar solo los 5 gastos más importantes (ordenados)
          const dist = (d.distribucionGastos || [])
            .sort((a, b) => b.total - a.total)
            .slice(0, 5);
          this.distribucionGastos.set(dist);

          this.fijoVariable.set(d.fijoVariable || []);
          this.heatmap.set(d.heatmap || []);
          this.metas.set(d.metas || []);
          this.comparativa.set(d.comparativa || []);
        } else {
          this.error.set(resp.mensaje || 'Error al cargar analítica avanzada');
        }
        this.loading.set(false);
        this.initialLoadDone = true;
      },
      error: (err) => {
        console.warn('[DashboardStateService] Fallback a mock de analítica avanzada:', err);
        // Mock fallback temporal para frontend dev
        this.cargarMock();
        this.loading.set(false);
        this.initialLoadDone = true;
      }
    });
  }

  /**
   * Invalida los datos y fuerza una recarga.
   */
  invalidarCache(): void {
    this.initialLoadDone = false; // Permitir recarga al ser evento de sincronización
    this.cargarAnalitica(this.filtrosActuales());
  }

  /**
   * Limpia el estado.
   */
  limpiarEstado(): void {
    this.resumen.set(null);
    this.flujoCaja.set([]);
    this.distribucionGastos.set([]);
    this.fijoVariable.set([]);
    this.heatmap.set([]);
    this.metas.set([]);
    this.comparativa.set([]);
    this.filtrosActuales.set({});
    this.loading.set(false);
    this.error.set(null);
  }

  // --- MOCK FALLBACK ---
  private cargarMock(): void {
    this.resumen.set({
      desde: new Date().toISOString(),
      hasta: new Date().toISOString(),
      tasaAhorro: 15.5,
      gastoPromedioDiario: 85.50,
      cumplimientoPresupuesto: 78.2,
      proyeccionFinDeMes: 1250.00
    });
    this.flujoCaja.set([
      { mes: 'Ene', ingresos: 3000, gastos: 2500 },
      { mes: 'Feb', ingresos: 3200, gastos: 2600 },
      { mes: 'Mar', ingresos: 3100, gastos: 2800 },
      { mes: 'Abr', ingresos: 3500, gastos: 2400 }
    ]);
    this.distribucionGastos.set([
      { categoria: 'Alimentación', total: 800, porcentaje: 35, color: '#f59e0b' },
      { categoria: 'Vivienda', total: 600, porcentaje: 25, color: '#3b82f6' },
      { categoria: 'Transporte', total: 400, porcentaje: 15, color: '#10b981' },
      { categoria: 'Entretenimiento', total: 300, porcentaje: 15, color: '#8b5cf6' },
      { categoria: 'Otros', total: 200, porcentaje: 10, color: '#64748b' }
    ]);
    this.fijoVariable.set([
      { tipo: 'FIJO', monto: 1200, porcentaje: 60 },
      { tipo: 'VARIABLE', monto: 800, porcentaje: 40 }
    ]);
    this.heatmap.set([
      { dia: 'Lunes', intensidad: 4 },
      { dia: 'Martes', intensidad: 6 },
      { dia: 'Miércoles', intensidad: 3 },
      { dia: 'Jueves', intensidad: 5 },
      { dia: 'Viernes', intensidad: 9 },
      { dia: 'Sábado', intensidad: 10 },
      { dia: 'Domingo', intensidad: 7 }
    ]);
    this.metas.set([
      { nombre: 'Fondo de Emergencia', objetivo: 5000, actual: 3500, porcentaje: 70, color: '#10b981' },
      { nombre: 'Viaje Fin de Año', objetivo: 2000, actual: 500, porcentaje: 25, color: '#3b82f6' }
    ]);
    this.comparativa.set([
      { mes: 'Marzo', actual: 2800, anterior: 2500 },
      { mes: 'Abril', actual: 2400, anterior: 2600 }
    ]);
  }
}
