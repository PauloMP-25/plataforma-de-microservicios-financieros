import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardHeaderComponent } from '../../components/dashboard-header/dashboard-header';
import { DashboardKpiGridComponent } from '../../components/dashboard-kpi-grid/dashboard-kpi-grid';
import { DashboardChartsGridComponent } from '../../components/dashboard-charts-grid/dashboard-charts-grid';
import { DashboardStateService, DashboardFiltros } from '../../../../core/services/dashboard-state.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [
    CommonModule, 
    DashboardHeaderComponent, 
    DashboardKpiGridComponent, 
    DashboardChartsGridComponent
  ],
  templateUrl: './dashboard-page.html',
  styleUrls: ['./dashboard-page.scss']
})
export class DashboardPage implements OnInit, OnDestroy {
  private busSubscription!: Subscription;

  constructor(
    public stateService: DashboardStateService,
    private eventBus: AppEventBus
  ) {}

  ngOnInit(): void {
    // Carga inicial (sin filtros)
    this.stateService.cargarAnalitica();

    this.busSubscription = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      console.log('[DashboardPage] Invalidador de caché activado. Recargando analítica...');
      this.stateService.invalidarCache();
    });
  }

  ngOnDestroy(): void {
    if (this.busSubscription) {
      this.busSubscription.unsubscribe();
    }
  }

  onFiltrosCambio(filtros: DashboardFiltros): void {
    // Al cambiar los filtros en el header, recargamos la analítica desde el backend
    this.stateService.cargarAnalitica(filtros);
  }

  // Lectura directa de señales para la vista (KPIs)
  get tasaAhorro(): number { return this.stateService.resumen()?.tasaAhorro ?? 0; }
  get gastoPromedioDiario(): number { return this.stateService.resumen()?.gastoPromedioDiario ?? 0; }
  get cumplimientoPresupuesto(): number { return this.stateService.resumen()?.cumplimientoPresupuesto ?? 0; }
  get proyeccionFinDeMes(): number { return this.stateService.resumen()?.proyeccionFinDeMes ?? 0; }
}
