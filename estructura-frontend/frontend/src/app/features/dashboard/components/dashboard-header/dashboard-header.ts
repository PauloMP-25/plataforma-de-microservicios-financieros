import { Component, Output, EventEmitter, OnInit } from '@angular/core';
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
export class DashboardHeaderComponent implements OnInit {
  @Output() filtrosCambio = new EventEmitter<DashboardFiltros>();

  fechaInicio: string = '2026-01-01';
  fechaFin: string = '';
  tipoMovimiento: string = '';
  metodoPago: string = '';

  ngOnInit(): void {
    this.fechaFin = this.formatDate(new Date());
    setTimeout(() => this.onFiltroChange());
  }

  onFiltroChange(): void {
    const filtros: DashboardFiltros = {
      fechaInicio: this.fechaInicio || undefined,
      fechaFin: this.fechaFin || undefined,
      tipoMovimiento: this.tipoMovimiento || undefined,
      metodoPago: this.metodoPago || undefined
    };
    this.filtrosCambio.emit(filtros);
  }

  seleccionarRangoRapido(tipo: 'seis_meses' | 'mes' | 'tres_meses' | 'anio'): void {
    const hoy = new Date();
    
    if (tipo === 'seis_meses') {
      const inicio = new Date();
      inicio.setMonth(hoy.getMonth() - 6);
      this.fechaInicio = this.formatDate(inicio);
      this.fechaFin = this.formatDate(hoy);
    } else if (tipo === 'mes') {
      const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
      this.fechaInicio = this.formatDate(inicio);
      this.fechaFin = this.formatDate(hoy);
    } else if (tipo === 'tres_meses') {
      const inicio = new Date();
      inicio.setMonth(hoy.getMonth() - 3);
      this.fechaInicio = this.formatDate(inicio);
      this.fechaFin = this.formatDate(hoy);
    } else if (tipo === 'anio') {
      const inicio = new Date(hoy.getFullYear(), 0, 1);
      this.fechaInicio = this.formatDate(inicio);
      this.fechaFin = this.formatDate(hoy);
    }
    
    this.onFiltroChange();
  }

  esRangoActivo(tipo: 'seis_meses' | 'mes' | 'tres_meses' | 'anio'): boolean {
    if (!this.fechaInicio || !this.fechaFin) return false;
    
    const hoy = new Date();
    const hoyStr = this.formatDate(hoy);
    if (this.fechaFin !== hoyStr) return false;
    
    if (tipo === 'seis_meses') {
      const inicio = new Date();
      inicio.setMonth(hoy.getMonth() - 6);
      return this.fechaInicio === this.formatDate(inicio);
    } else if (tipo === 'mes') {
      const inicio = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
      return this.fechaInicio === this.formatDate(inicio);
    } else if (tipo === 'tres_meses') {
      const inicio = new Date();
      inicio.setMonth(hoy.getMonth() - 3);
      return this.fechaInicio === this.formatDate(inicio);
    } else if (tipo === 'anio') {
      const inicio = new Date(hoy.getFullYear(), 0, 1);
      return this.fechaInicio === this.formatDate(inicio);
    }
    
    return false;
  }

  limpiarFiltrosFechas(): void {
    this.seleccionarRangoRapido('anio');
  }


  obtenerNombreMesActual(): string {
    const meses = [
      'ENERO', 'FEBRERO', 'MARZO', 'ABRIL', 'MAYO', 'JUNIO',
      'JULIO', 'AGOSTO', 'SEPTIEMBRE', 'OCTUBRE', 'NOVIEMBRE', 'DICIEMBRE'
    ];
    return meses[new Date().getMonth()];
  }

  private formatDate(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
}
