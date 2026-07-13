import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard-kpi-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-kpi-grid.html',
  styleUrls: ['./dashboard-kpi-grid.scss']
})
export class DashboardKpiGridComponent {
  @Input() tasaAhorro: number = 0;
  @Input() gastoPromedioDiario: number = 0;
  @Input() volumenTransacciones: number = 0;
  @Input() proyeccionFinDeMes: number = 0;
  /** 'INGRESO' | 'EGRESO' | undefined = TODOS */
  @Input() tipoMovimiento?: string;

  get esIngreso(): boolean {
    return this.tipoMovimiento?.toUpperCase() === 'INGRESO';
  }

  get labelPromDiario(): string {
    return this.esIngreso ? 'Ingreso Prom. Diario' : 'Gasto Prom. Diario';
  }

  get descPromDiario(): string {
    return this.esIngreso
      ? 'Monto estimado que recibes cada día en promedio dentro del período seleccionado.'
      : 'Monto estimado que gastas cada día en promedio dentro del período seleccionado.';
  }

  get labelProyeccion(): string {
    return this.esIngreso ? 'Proyección Ingresos Mes' : 'Proyección Fin de Mes';
  }

  get descProyeccion(): string {
    return this.esIngreso
      ? 'Estimación de tus ingresos totales para el cierre de mes en base a tu ritmo actual.'
      : 'Estimación de tus egresos totales para el cierre de mes en base a tu velocidad de gasto actual.';
  }
}
