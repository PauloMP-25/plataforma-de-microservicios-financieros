import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { DistribucionCategoria, IngresoTendenciaPunto } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingreso-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ingreso-chart.html',
})
export class IngresoChartComponent {
  @Input() title = 'Ingresos por categoría';
  @Input() totalLabel = 'S/ 3,850';
  @Input() distribucion: DistribucionCategoria[] = [];
  @Input() tendencia: IngresoTendenciaPunto[] = [];

  get donutStyle(): string {
    const stops = this.distribucion
      .map((d, i) => {
        const prev = this.distribucion.slice(0, i).reduce((a, b) => a + b.porcentaje, 0);
        const end = prev + d.porcentaje;
        return `${d.color} ${prev}% ${end}%`;
      })
      .join(', ');
    return `background: conic-gradient(${stops});`;
  }

  get totalMonto(): number {
    return this.distribucion.reduce((acc, d) => acc + d.monto, 0);
  }

  get categoriaTop(): string {
    return this.distribucion.length ? this.distribucion[0].categoria : '-';
  }
}

