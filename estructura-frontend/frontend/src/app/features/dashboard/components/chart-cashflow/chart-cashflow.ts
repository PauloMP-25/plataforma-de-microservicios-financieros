import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-cashflow',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-cashflow.html',
  styleUrls: ['./chart-cashflow.scss']
})
export class ChartCashflowComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    // Escuchar reactivamente los cambios en el flujo de caja
    effect(() => {
      const data = this.stateService.flujoCaja();
      const isDark = this.themeService.temaOscuro();
      if (this.chart) {
        this.actualizarGrafico(data, isDark);
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

  private inicializarGrafico(): void {
    const canvas = document.getElementById('cashflowCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.flujoCaja();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const gradientIngresos = ctx.createLinearGradient(0, 0, 0, 400);
    gradientIngresos.addColorStop(0, 'rgba(10, 185, 129, 0.4)');
    gradientIngresos.addColorStop(1, 'rgba(10, 185, 129, 0.0)');

    const gradientGastos = ctx.createLinearGradient(0, 0, 0, 400);
    gradientGastos.addColorStop(0, 'rgba(239, 68, 68, 0.4)');
    gradientGastos.addColorStop(1, 'rgba(239, 68, 68, 0.0)');

    const config: ChartConfiguration = {
      type: 'line',
      data: {
        labels: data.map(d => d.mes),
        datasets: [
          {
            label: 'Ingresos',
            data: data.map(d => d.ingresos),
            borderColor: '#10b981',
            backgroundColor: gradientIngresos,
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            pointBackgroundColor: '#10b981',
            pointRadius: 4,
            pointHoverRadius: 6
          },
          {
            label: 'Gastos',
            data: data.map(d => d.gastos),
            borderColor: '#ef4444',
            backgroundColor: gradientGastos,
            borderWidth: 2,
            tension: 0.4,
            fill: true,
            pointBackgroundColor: '#ef4444',
            pointRadius: 4,
            pointHoverRadius: 6
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        layout: {
          padding: {
            left: 10,
            right: 40,
            top: 25,
            bottom: 10
          }
        },
        plugins: {
          legend: {
            labels: { color: textColor, font: { family: 'Inter, sans-serif', size: 14, weight: 'bold' } }
          },
          tooltip: {
            mode: 'index',
            intersect: false,
            backgroundColor: isDark ? 'rgba(30, 41, 59, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: isDark ? '#f8fafc' : '#0f172a',
            bodyColor: isDark ? '#cbd5e1' : '#475569',
            borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
            borderWidth: 1,
            padding: 12
          }
        },
        scales: {
          x: {
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' } }
          },
          y: {
            grace: '20%',
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' } }
          }
        }
      },
      plugins: [{
        id: 'dataLabels',
        afterDraw: (chart) => {
          const { ctx } = chart;
          const chartArea = chart.chartArea;
          ctx.save();
          ctx.font = 'bold 11px Inter, sans-serif';
          
          const datasets = chart.data.datasets;
          const showIngresos = !chart.getDatasetMeta(0).hidden;
          const showGastos = !chart.getDatasetMeta(1).hidden;

          // Helper: draw a single label with axis boundary checks
          const drawLabel = (label: string, x: number, y: number, color: string, above: boolean) => {
            ctx.fillStyle = color;
            // Measure text width for boundary check
            const textWidth = ctx.measureText(label).width;
            const halfText = textWidth / 2;

            // Clamp X so label doesn't overlap Y-axis ticks or right edge
            let labelX = x;
            if (labelX - halfText < chartArea.left + 5) {
              ctx.textAlign = 'left';
              labelX = chartArea.left + 5;
            } else if (labelX + halfText > chartArea.right - 5) {
              ctx.textAlign = 'right';
              labelX = chartArea.right - 5;
            } else {
              ctx.textAlign = 'center';
            }

            // Choose above or below, and clamp Y so it doesn't exceed chart area
            if (above) {
              ctx.textBaseline = 'bottom';
              const labelY = Math.max(y - 10, chartArea.top + 12);
              ctx.fillText(label, labelX, labelY);
            } else {
              ctx.textBaseline = 'top';
              const labelY = Math.min(y + 10, chartArea.bottom - 14);
              ctx.fillText(label, labelX, labelY);
            }
          };

          if (showIngresos && showGastos) {
            const metaIngresos = chart.getDatasetMeta(0);
            const metaGastos = chart.getDatasetMeta(1);

            metaIngresos.data.forEach((element: any, index) => {
              const val = datasets[0].data[index] as number;
              if (val !== undefined && val !== null) {
                const valGastos = datasets[1].data[index] as number;
                const isTop = valGastos === undefined || val >= valGastos;
                const label = `S/ ${val.toLocaleString()}`;
                const { x, y } = element.tooltipPosition();
                drawLabel(label, x, y, datasets[0].borderColor as string, isTop);
              }
            });

            metaGastos.data.forEach((element: any, index) => {
              const val = datasets[1].data[index] as number;
              if (val !== undefined && val !== null) {
                const valIngresos = datasets[0].data[index] as number;
                const isTop = valIngresos === undefined || val >= valIngresos;
                const label = `S/ ${val.toLocaleString()}`;
                const { x, y } = element.tooltipPosition();
                drawLabel(label, x, y, datasets[1].borderColor as string, isTop);
              }
            });
          } else {
            datasets.forEach((dataset, i) => {
              const meta = chart.getDatasetMeta(i);
              if (!meta.hidden) {
                meta.data.forEach((element: any, index) => {
                  const val = dataset.data[index] as number;
                  if (val !== undefined && val !== null) {
                    const label = `S/ ${val.toLocaleString()}`;
                    const { x, y } = element.tooltipPosition();
                    drawLabel(label, x, y, dataset.borderColor as string, true);
                  }
                });
              }
            });
          }
          ctx.restore();
        }
      }]
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    this.chart.data.labels = data.map(d => d.mes);
    this.chart.data.datasets[0].data = data.map(d => d.ingresos);
    this.chart.data.datasets[1].data = data.map(d => d.gastos);

    // Actualizar colores del tema dinámicamente
    if (this.chart.options.plugins?.legend?.labels) {
      this.chart.options.plugins.legend.labels.color = textColor;
    }
    if (this.chart.options.scales?.['x']) {
      (this.chart.options.scales['x'] as any).grid.color = gridColor;
      (this.chart.options.scales['x'] as any).ticks.color = textColor;
    }
    if (this.chart.options.scales?.['y']) {
      (this.chart.options.scales['y'] as any).grid.color = gridColor;
      (this.chart.options.scales['y'] as any).ticks.color = textColor;
    }

    this.chart.update();
  }
}
