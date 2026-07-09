import { Component, AfterViewInit, OnDestroy, effect, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-historical-comparison',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-historical-comparison.html',
  styleUrls: ['./chart-historical-comparison.scss']
})
export class ChartHistoricalComparisonComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  /** Título dinámico según el tipo de movimiento seleccionado */
  tituloComparativa = computed(() => {
    const tipo = this.stateService.filtrosActuales().tipoMovimiento?.toUpperCase();
    return tipo === 'INGRESO' ? 'Comparativa Mensual de Ingresos' : 'Comparativa Mensual de Gastos';
  });

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    effect(() => {
      const data = this.stateService.comparativa();
      const isDark = this.themeService.temaOscuro();
      const tipo = this.stateService.filtrosActuales().tipoMovimiento?.toUpperCase();
      if (this.chart) {
        this.actualizarGrafico(data, isDark, tipo);
      }
    });
  }

  ngAfterViewInit(): void {
    this.inicializarGrafico();
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  private resolverColorBarra(tipo?: string): string {
    return tipo === 'INGRESO' ? '#10b981' : '#5b6af0';
  }

  private resolverLabelBarra(tipo?: string): string {
    return tipo === 'INGRESO' ? 'Ingresos' : 'Gastos';
  }

  private resolverDatos(data: any[], tipo?: string): number[] {
    return tipo === 'INGRESO'
      ? data.map(d => d.ingresos ?? d.actual ?? 0)
      : data.map(d => d.gastos ?? d.actual ?? 0);
  }

  private inicializarGrafico(): void {
    const canvas = document.getElementById('historicalComparisonCanvas') as HTMLCanvasElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.comparativa();
    const isDark = this.themeService.temaOscuro();
    const tipo = this.stateService.filtrosActuales().tipoMovimiento?.toUpperCase();
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: data.map(d => d.mes),
        datasets: [
          {
            label: this.resolverLabelBarra(tipo),
            data: this.resolverDatos(data, tipo),
            backgroundColor: this.resolverColorBarra(tipo),
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        layout: { padding: { left: 15, right: 15, top: 20, bottom: 5 } },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: isDark ? 'rgba(30, 41, 59, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: isDark ? '#f8fafc' : '#0f172a',
            bodyColor: isDark ? '#cbd5e1' : '#475569',
            borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
            borderWidth: 1
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: textColor, font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' } }
          },
          y: {
            grace: '15%',
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' } }
          }
        }
      },
      plugins: [{
        id: 'dataLabels',
        afterDraw: (chart) => {
          const { ctx } = chart;
          const color = this.resolverColorBarra(this.stateService.filtrosActuales().tipoMovimiento?.toUpperCase());
          ctx.save();
          ctx.font = 'bold 11px Inter, sans-serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'bottom';
          ctx.fillStyle = color;
          chart.data.datasets.forEach((dataset, i) => {
            const meta = chart.getDatasetMeta(i);
            if (!meta.hidden) {
              meta.data.forEach((element: any, index) => {
                const val = dataset.data[index] as number;
                if (val !== undefined && val !== null) {
                  const { x, y } = element.tooltipPosition();
                  ctx.fillText(`S/ ${val.toLocaleString()}`, x, y - 6);
                }
              });
            }
          });
          ctx.restore();
        }
      }]
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean, tipo?: string): void {
    if (!this.chart) return;

    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    this.chart.data.labels = data.map(d => d.mes);
    this.chart.data.datasets[0].label = this.resolverLabelBarra(tipo);
    this.chart.data.datasets[0].data = this.resolverDatos(data, tipo);
    (this.chart.data.datasets[0] as any).backgroundColor = this.resolverColorBarra(tipo);

    if (this.chart.options.scales?.['x']) {
      (this.chart.options.scales['x'] as any).ticks.color = textColor;
    }
    if (this.chart.options.scales?.['y']) {
      (this.chart.options.scales['y'] as any).grid.color = gridColor;
      (this.chart.options.scales['y'] as any).ticks.color = textColor;
    }

    this.chart.update();
  }
}
