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

  // ── [F-59] Lógica para segmentar el gráfico exclusivamente al Top 5 ──
  get topDistribucion(): DistribucionCategoria[] {
    if (this.distribucion.length <= 5) {
      return this.distribucion;
    }

    const top4 = this.distribucion.slice(0, 4);
    const elResto = this.distribucion.slice(4);

    const montoOtros = elResto.reduce((acc, d) => acc + d.monto, 0);
    const totalGlobal = this.totalMonto;
    const porcentajeOtros = totalGlobal > 0 ? (montoOtros / totalGlobal) * 100 : 0;

    return [
      ...top4,
      {
        categoria: 'Otros',
        monto: montoOtros,
        porcentaje: parseFloat(porcentajeOtros.toFixed(0)), // Mantiene el formato sin decimales de tu diseño original
        color: '#94a6b8' // Color neutro para la agrupación
      }
    ];
  }

  get donutStyle(): string {
    // Cambiado para que el gráfico de dona use el arreglo del Top 5
    const datosGrafico = this.topDistribucion;
    if (!datosGrafico.length) return 'background: conic-gradient(#e2e8f0 0% 100%);';

    const stops = datosGrafico
      .map((d, i) => {
        const prev = datosGrafico.slice(0, i).reduce((a, b) => a + b.porcentaje, 0);
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