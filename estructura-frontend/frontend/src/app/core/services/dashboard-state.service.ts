import { Injectable, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
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
  private base = `${environment.gatewayUrl}/api/v1/dashboard`;

  // ── Angular Signals de Estado ──
  readonly resumen = signal<DashboardResumenDTO | null>(null);
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

          this.heatmap.set(d.heatmap || []);
          this.metas.set(d.metas || []);
          this.comparativa.set(d.comparativa || []);
          this.transaccionesMetodo.set(d.transaccionesMetodo || []);
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
    this.heatmap.set([]);
    this.metas.set([]);
    this.comparativa.set([]);
    this.transaccionesMetodo.set([]);
    this.filtrosActuales.set({});
    this.loading.set(false);
    this.error.set(null);
  }

  // --- MOCK FALLBACK ---
  private cargarMock(): void {
    const filtros = this.filtrosActuales();
    
    // Base numbers that we can scale or filter
    let ingresosBase = 3200;
    let gastosBase = 2500;
    let ahorroBase = 15.5;
    let promedioDiario = 85.50;
    let presupuestoCumplimiento = 78.2;
    let proyeccionFin = 1250.00;
    
    // Adjust based on Movement Type
    let showIngresos = true;
    let showGastos = true;
    
    if (filtros.tipoMovimiento === 'INGRESO') {
      showGastos = false;
      gastosBase = 0;
      ahorroBase = 100;
      promedioDiario = 0;
      presupuestoCumplimiento = 0;
      proyeccionFin = 0;
    } else if (filtros.tipoMovimiento === 'EGRESO') {
      showIngresos = false;
      ingresosBase = 0;
      ahorroBase = -100;
    }

    // Adjust based on Payment Method (just change the data slightly to show it works)
    let multiplier = 1.0;
    if (filtros.metodoPago === 'EFECTIVO') multiplier = 0.3;
    else if (filtros.metodoPago === 'TARJETA') multiplier = 0.55;
    else if (filtros.metodoPago === 'TRANSFERENCIA') multiplier = 0.75;
    else if (filtros.metodoPago === 'DIGITAL') multiplier = 0.45;

    ingresosBase *= multiplier;
    gastosBase *= multiplier;
    promedioDiario *= multiplier;
    proyeccionFin *= multiplier;

    // Adjust based on Date Range
    let cashflowData = [
      { mes: 'Ene', ingresos: 3000, gastos: 2500 },
      { mes: 'Feb', ingresos: 3200, gastos: 2600 },
      { mes: 'Mar', ingresos: 3100, gastos: 2800 },
      { mes: 'Abr', ingresos: 3500, gastos: 2400 },
      { mes: 'May', ingresos: 3400, gastos: 2700 },
      { mes: 'Jun', ingresos: 3600, gastos: 2900 },
      { mes: 'Jul', ingresos: 3800, gastos: 3100 },
      { mes: 'Ago', ingresos: 3700, gastos: 3000 },
      { mes: 'Sep', ingresos: 3900, gastos: 3200 },
      { mes: 'Oct', ingresos: 4000, gastos: 3300 },
      { mes: 'Nov', ingresos: 4200, gastos: 3400 },
      { mes: 'Dic', ingresos: 4500, gastos: 3600 }
    ];

    let comparativaData = [
      { mes: 'Ene', actual: 3000, anterior: 2800 },
      { mes: 'Feb', actual: 3200, anterior: 2900 },
      { mes: 'Mar', actual: 3100, anterior: 3000 },
      { mes: 'Abr', actual: 3500, anterior: 3200 },
      { mes: 'May', actual: 3400, anterior: 3100 },
      { mes: 'Jun', actual: 3600, anterior: 3300 },
      { mes: 'Jul', actual: 3800, anterior: 3500 },
      { mes: 'Ago', actual: 3700, anterior: 3400 },
      { mes: 'Sep', actual: 3900, anterior: 3600 },
      { mes: 'Oct', actual: 4000, anterior: 3700 },
      { mes: 'Nov', actual: 4200, anterior: 3800 },
      { mes: 'Dic', actual: 4500, anterior: 4000 }
    ];

    if (filtros.fechaInicio || filtros.fechaFin) {
      // If we filtered by a range, let's change labels or cut data points to reflect filtering
      const start = filtros.fechaInicio ? new Date(filtros.fechaInicio) : null;
      const end = filtros.fechaFin ? new Date(filtros.fechaFin) : null;
      
      if (start && end) {
        const diffTime = Math.abs(end.getTime() - start.getTime());
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        if (diffDays <= 7) {
          // Weekly: Show only 1 month (or a subset of cashflow)
          cashflowData = [
            { mes: 'Sem. Actual', ingresos: ingresosBase, gastos: gastosBase }
          ];
          comparativaData = [
            { mes: 'Lun', actual: Math.round(120 * multiplier), anterior: Math.round(100 * multiplier) },
            { mes: 'Mar', actual: Math.round(150 * multiplier), anterior: Math.round(130 * multiplier) },
            { mes: 'Mié', actual: Math.round(80 * multiplier),  anterior: Math.round(95 * multiplier) },
            { mes: 'Jue', actual: Math.round(200 * multiplier), anterior: Math.round(150 * multiplier) },
            { mes: 'Vie', actual: Math.round(350 * multiplier), anterior: Math.round(300 * multiplier) },
            { mes: 'Sáb', actual: Math.round(400 * multiplier), anterior: Math.round(380 * multiplier) },
            { mes: 'Dom', actual: Math.round(180 * multiplier), anterior: Math.round(200 * multiplier) }
          ];
          promedioDiario = gastosBase / 7;
        } else if (diffDays <= 31) {
          // Monthly: Show last 2 months
          cashflowData = [
            { mes: 'Mar', ingresos: ingresosBase * 0.9, gastos: gastosBase * 1.1 },
            { mes: 'Abr', ingresos: ingresosBase, gastos: gastosBase }
          ];
          comparativaData = [
            { mes: 'Marzo', actual: gastosBase * 0.9, anterior: gastosBase * 0.95 },
            { mes: 'Abril', actual: gastosBase, anterior: gastosBase * 0.9 }
          ];
        }
      }
    }

    // Apply movement filters to cashflow values
    cashflowData = cashflowData.map(d => ({
      mes: d.mes,
      ingresos: showIngresos ? Math.round(d.ingresos * multiplier) : 0,
      gastos: showGastos ? Math.round(d.gastos * multiplier) : 0
    }));

    comparativaData = comparativaData.map(d => ({
      mes: d.mes,
      actual: showGastos ? Math.round(d.actual * multiplier) : 0,
      anterior: showGastos ? Math.round(d.anterior * multiplier) : 0
    }));

    // Update state signals
    this.resumen.set({
      desde: filtros.fechaInicio || new Date().toISOString(),
      hasta: filtros.fechaFin || new Date().toISOString(),
      tasaAhorro: showIngresos && showGastos ? ahorroBase : (showIngresos ? 100 : -100),
      gastoPromedioDiario: Math.round(promedioDiario * 100) / 100,
      cumplimientoPresupuesto: showGastos ? Math.round(presupuestoCumplimiento * multiplier * 10) / 10 : 0,
      proyeccionFinDeMes: Math.round(proyeccionFin * 100) / 100
    });

    this.flujoCaja.set(cashflowData);
    
    // Scale distributions
    this.distribucionGastos.set(showGastos ? [
      { categoria: 'Alimentación', total: Math.round(800 * multiplier), porcentaje: 35, color: '#f59e0b' },
      { categoria: 'Vivienda', total: Math.round(600 * multiplier), porcentaje: 25, color: '#3b82f6' },
      { categoria: 'Transporte', total: Math.round(400 * multiplier), porcentaje: 15, color: '#10b981' },
      { categoria: 'Entretenimiento', total: Math.round(300 * multiplier), porcentaje: 15, color: '#8b5cf6' },
      { categoria: 'Otros', total: Math.round(200 * multiplier), porcentaje: 10, color: '#64748b' }
    ].slice(0, 5) : []);


    this.heatmap.set(showGastos ? [
      { dia: 'Lunes', intensidad: Math.round(4 * multiplier) || 1 },
      { dia: 'Martes', intensidad: Math.round(6 * multiplier) || 1 },
      { dia: 'Miércoles', intensidad: Math.round(3 * multiplier) || 1 },
      { dia: 'Jueves', intensidad: Math.round(5 * multiplier) || 1 },
      { dia: 'Viernes', intensidad: Math.round(9 * multiplier) || 2 },
      { dia: 'Sábado', intensidad: Math.round(10 * multiplier) || 2 },
      { dia: 'Domingo', intensidad: Math.round(7 * multiplier) || 1 }
    ] : []);

    this.metas.set([
      { nombre: 'Fondo de Emergencia', objetivo: 5000, actual: Math.round(3500 * (showIngresos ? multiplier : 0.5)), porcentaje: 70, color: '#10b981' },
      { nombre: 'Viaje Fin de Año', objetivo: 2000, actual: Math.round(500 * (showIngresos ? multiplier : 0.5)), porcentaje: 25, color: '#3b82f6' }
    ]);

    this.transaccionesMetodo.set([
      { metodo: 'Tarjeta', cantidad: Math.round(25 * multiplier) || 2, color: '#3b82f6' },
      { metodo: 'Efectivo', cantidad: Math.round(15 * multiplier) || 1, color: '#10b981' },
      { metodo: 'Transferencia', cantidad: Math.round(18 * multiplier) || 1, color: '#a855f7' },
      { metodo: 'Digital', cantidad: Math.round(12 * multiplier) || 1, color: '#f59e0b' }
    ]);

    this.comparativa.set(comparativaData);
  }
}
