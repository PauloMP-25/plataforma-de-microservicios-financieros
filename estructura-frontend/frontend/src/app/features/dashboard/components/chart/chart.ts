import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface CategoriaDistribucion {
  categoria: string;
  total: number;
  porcentaje: number;
  color: string;
}

interface FlujoCajaPunto {
  mes: string;
  ingresos: number;
  gastos: number;
}

@Component({
  selector: 'app-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart.html',
  styleUrl: './chart.scss',
})
export class Chart {
  @Input() flujoCaja: FlujoCajaPunto[] = [];
  @Input() distribucion: CategoriaDistribucion[] = [];

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
