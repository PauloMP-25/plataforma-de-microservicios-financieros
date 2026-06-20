import { Component, Input } from '@angular/core';
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
  @Input() cumplimientoPresupuesto: number = 0;
  @Input() proyeccionFinDeMes: number = 0;
}
