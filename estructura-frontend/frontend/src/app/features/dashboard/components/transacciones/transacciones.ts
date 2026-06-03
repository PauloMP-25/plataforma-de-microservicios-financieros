import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Transaccion {
  id: number | string;
  fecha: string;
  descripcion: string;
  categoria: string;
  tipo: string;
  monto: number;
  estado: string;
  icono: string;
}

@Component({
  selector: 'app-transacciones',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transacciones.html',
  styleUrl: './transacciones.scss',
})
export class Transacciones {
  @Input() transacciones: Transaccion[] = [];
  filtroTipo: string = 'todos';

  get transaccionesFiltradas(): Transaccion[] {
    if (this.filtroTipo === 'todos') {
      return this.transacciones;
    }
    return this.transacciones.filter(t => t.tipo.toLowerCase() === this.filtroTipo.toLowerCase());
  }

  getColorCategoria(cat: string): string {
    const colores: { [key: string]: string } = {
      food: 'var(--color-food)',
      transport: 'var(--color-transport)',
      leisure: 'var(--color-leisure)',
      health: 'var(--color-health)',
      SaaS: 'var(--color-primary)',
      inversiones: 'var(--color-success)',
      transferencia: 'var(--color-warning)',
      salario: 'var(--color-success)'
    };
    return colores[cat] || 'var(--text-muted)';
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
