import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DashboardFiltros } from '../../../../core/services/dashboard-state.service';

@Component({
  selector: 'app-dashboard-header',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard-header.html',
  styleUrls: ['./dashboard-header.scss']
})
export class DashboardHeaderComponent {
  @Output() filtrosCambio = new EventEmitter<DashboardFiltros>();

  terminoBusqueda: string = '';
  fechaInicio: string = '';
  fechaFin: string = '';
  tipoMovimiento: string = '';
  metodoPago: string = '';

  onFiltroChange(): void {
    const filtros: DashboardFiltros = {
      fechaInicio: this.fechaInicio || undefined,
      fechaFin: this.fechaFin || undefined,
      tipoMovimiento: this.tipoMovimiento || undefined,
      metodoPago: this.metodoPago || undefined
    };
    this.filtrosCambio.emit(filtros);
  }

  limpiarBusqueda(): void {
    this.terminoBusqueda = '';
    this.onFiltroChange();
  }
}
