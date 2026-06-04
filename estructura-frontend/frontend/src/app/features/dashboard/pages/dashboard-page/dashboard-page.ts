import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BalancesCard } from '../../components/balances-card/balances-card';
import { Chart } from '../../components/chart/chart';
import { Transacciones } from '../../components/transacciones/transacciones';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { Subscription } from 'rxjs';

interface Transaccion {
  id: string;
  fechaTransaccion: string;
  descripcion: string;
  categoria: string;
  tipo: 'INGRESO' | 'GASTO';
  monto: number;
  estado: string;
  categoriaIcono: string;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule, BalancesCard, Chart, Transacciones],
  templateUrl: './dashboard-page.html',
  styleUrls: ['./dashboard-page.scss']
})
export class DashboardPage implements OnInit, OnDestroy {
  // Configuración de Filtros
  filtroTiempo: string = '30';
  terminoBusqueda: string = '';

  private busSubscription!: Subscription;

  constructor(
    public stateService: DashboardStateService,
    private eventBus: AppEventBus
  ) {}

  ngOnInit(): void {
    // Cargar datos iniciales del BFF de forma limpia (con control de caché integrado)
    this.stateService.cargarResumen();
    this.stateService.cargarGraficos();

    // Escuchar el bus de eventos global para cambios financieros
    this.busSubscription = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      console.log('[DashboardPage] Invalidador de caché activado por evento. Recargando...');
      this.stateService.invalidarCache();
    });
  }

  ngOnDestroy(): void {
    if (this.busSubscription) {
      this.busSubscription.unsubscribe();
    }
  }

  // Filtrado de transacciones por término de búsqueda y periodo
  get transaccionesFiltradas(): any[] {
    const query = this.terminoBusqueda.trim().toLowerCase();
    let txs = this.stateService.recientes();

    // 1. Filtro por término
    if (query) {
      txs = txs.filter(t => {
        const coincideDesc = t.descripcion?.toLowerCase().includes(query);
        const coincideCat = t.categoria?.toLowerCase().includes(query);
        return coincideDesc || coincideCat;
      });
    }

    // 2. Filtro por tiempo (días desde hoy)
    if (this.filtroTiempo !== 'todos') {
      const limiteDias = parseInt(this.filtroTiempo, 10);
      const fechaLimite = new Date();
      fechaLimite.setDate(fechaLimite.getDate() - limiteDias);

      txs = txs.filter(t => {
        const fechaTrans = new Date(t.fechaTransaccion);
        return fechaTrans >= fechaLimite;
      });
    }

    return txs;
  }

  // Cálculos reactivos de KPIs expuestos a la vista (leídos del Signal)
  get totalBalance(): number {
    return this.stateService.resumen()?.balance ?? 0;
  }

  get totalIngresos(): number {
    return this.stateService.resumen()?.totalIngresos ?? 0;
  }

  get totalGastos(): number {
    return this.stateService.resumen()?.totalGastos ?? 0;
  }

  get tasaAhorro(): number {
    return this.stateService.resumen()?.tasaAhorro ?? 0;
  }

  // Datos para los subcomponentes del gráfico
  get flujoCajaPuntos(): any[] {
    return this.stateService.flujoCaja();
  }

  get distribucionCategorias(): any[] {
    return this.stateService.distribucionGastos();
  }
}
