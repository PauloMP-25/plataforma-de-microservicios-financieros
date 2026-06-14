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
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);
  }

  // ── Mapeos y Trazados para el Gráfico de Flujo de Caja (Line Chart SVG) ──

  get maxValorFlujo(): number {
    if (!this.flujoCaja || this.flujoCaja.length === 0) return 1;
    const maxVal = Math.max(...this.flujoCaja.map(p => Math.max(p.ingresos, p.gastos)), 1);
    return maxVal;
  }

  get yAxisLabels(): string[] {
    const max = this.maxValorFlujo;
    return [
      this.formatCurrency(max),
      this.formatCurrency(max * 2 / 3),
      this.formatCurrency(max / 3),
      this.formatCurrency(0)
    ];
  }

  get ingresosPath(): string {
    return this.generarLíneaPath(this.flujoCaja.map(p => p.ingresos));
  }

  get ingresosFillPath(): string {
    const linePath = this.ingresosPath;
    if (!linePath) return '';
    return `${linePath} L 450 180 L 50 180 Z`;
  }

  get gastosPath(): string {
    return this.generarLíneaPath(this.flujoCaja.map(p => p.gastos));
  }

  get gastosFillPath(): string {
    const linePath = this.gastosPath;
    if (!linePath) return '';
    return `${linePath} L 450 180 L 50 180 Z`;
  }

  get puntosIngresos(): { cx: number; cy: number; valor: number }[] {
    return this.obtenerNodosPuntos(this.flujoCaja.map(p => p.ingresos));
  }

  get puntosGastos(): { cx: number; cy: number; valor: number }[] {
    return this.obtenerNodosPuntos(this.flujoCaja.map(p => p.gastos));
  }

  private generarLíneaPath(valores: number[]): string {
    if (!valores || valores.length === 0) return '';
    const max = this.maxValorFlujo;
    return valores.map((val, idx) => {
      const x = 50 + idx * 100;
      const y = 180 - (val / max) * 150;
      return `${idx === 0 ? 'M' : 'L'} ${x} ${y}`;
    }).join(' ');
  }

  private obtenerNodosPuntos(valores: number[]): { cx: number; cy: number; valor: number }[] {
    if (!valores || valores.length === 0) return [];
    const max = this.maxValorFlujo;
    return valores.map((val, idx) => ({
      cx: 50 + idx * 100,
      cy: 180 - (val / max) * 150,
      valor: val
    }));
  }

  // ── Mapeos para el Gráfico de Dona (Doughnut Chart SVG) ──

  get totalGastado(): number {
    return this.distribucion.reduce((acc, d) => acc + d.total, 0);
  }

  get segmentosDona(): { dashArray: string; dashOffset: number; color: string; categoria: string; porcentaje: number }[] {
    let acumulado = 100;
    return this.distribucion.map(d => {
      const pct = d.porcentaje;
      const dashArray = `${pct} ${100 - pct}`;
      const dashOffset = acumulado;
      acumulado -= pct;
      return {
        dashArray,
        dashOffset,
        color: d.color,
        categoria: d.categoria,
        porcentaje: pct
      };
    });
  }
}
