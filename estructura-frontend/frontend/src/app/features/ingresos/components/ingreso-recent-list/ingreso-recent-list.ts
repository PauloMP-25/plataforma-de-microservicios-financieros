import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { IngresoReciente } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingreso-recent-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ingreso-recent-list.html',
})
export class IngresoRecentListComponent {
  @Input() items: IngresoReciente[] = [];

  // ── [F-60] Lógica de Paginación ──
  paginaActual = 1;
  limitePorPagina = 6;

  // Corta el arreglo original para devolver solo los 6 correspondientes
  get itemsPaginados(): IngresoReciente[] {
    const inicio = (this.paginaActual - 1) * this.limitePorPagina;
    const fin = inicio + this.limitePorPagina;
    return this.items.slice(inicio, fin);
  }

  get totalPaginas(): number {
    return Math.ceil(this.items.length / this.limitePorPagina) || 1;
  }

  cambiarPagina(direccion: number): void {
    const nuevaPagina = this.paginaActual + direccion;
    if (nuevaPagina >= 1 && nuevaPagina <= this.totalPaginas) {
      this.paginaActual = nuevaPagina;
    }
  }

  // Mantiene tus funciones de iconos intactas
  iconClass(categoria: string): string {
    const c = categoria.toLowerCase();
    if (c.includes('salario')) return 'fa-solid fa-briefcase text-emerald-600';
    if (c.includes('freelance')) return 'fa-regular fa-user text-violet-600';
    if (c.includes('invers')) return 'fa-solid fa-arrow-trend-up text-amber-500';
    if (c.includes('venta')) return 'fa-solid fa-cart-shopping text-blue-600';
    if (c.includes('bonif')) return 'fa-solid fa-gift text-cyan-600';
    return 'fa-solid fa-coins text-slate-600';
  }

  iconBgClass(categoria: string): string {
    const c = categoria.toLowerCase();
    if (c.includes('salario')) return 'bg-emerald-100 dark:bg-emerald-500/20';
    if (c.includes('freelance')) return 'bg-violet-100 dark:bg-violet-500/20';
    if (c.includes('invers')) return 'bg-amber-100 dark:bg-amber-500/20';
    if (c.includes('venta')) return 'bg-blue-100 dark:bg-blue-500/20';
    if (c.includes('bonif')) return 'bg-cyan-100 dark:bg-cyan-500/20';
    return 'bg-slate-100 dark:bg-slate-600';
  }
}