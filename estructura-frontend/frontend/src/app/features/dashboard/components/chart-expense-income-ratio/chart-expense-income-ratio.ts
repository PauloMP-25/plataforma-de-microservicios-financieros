import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-expense-income-ratio',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-expense-income-ratio.html',
  styleUrls: ['./chart-expense-income-ratio.scss']
})
export class ChartExpenseIncomeRatioComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    // Escuchar reactivamente los cambios en el flujo de caja para recalcular la relación
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

  private calcularRatios(data: any[]): number[] {
    return data.map(d => {
      if (d.ingresos <= 0) {
        return d.gastos > 0 ? 100 : 0;
      }
      return Math.round((d.gastos / d.ingresos) * 100);
    });
  }

  private inicializarGrafico(): void {
    const canvas = document.getElementById('expenseIncomeRatioCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.flujoCaja();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const ratios = this.calcularRatios(data);
    const backgroundColors = ratios.map(r => r > 80 ? '#ef4444' : '#10b981');

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: data.map(d => d.mes),
        datasets: [{
          label: 'Relación Gasto-Ingreso',
          data: ratios,
          backgroundColor: backgroundColors,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        layout: {
          padding: {
            left: 10,
            right: 15,
            top: 25,
            bottom: 5
          }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: isDark ? 'rgba(30, 41, 59, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: isDark ? '#f8fafc' : '#0f172a',
            bodyColor: isDark ? '#cbd5e1' : '#475569',
            borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
            borderWidth: 1,
            callbacks: {
              label: (context: any) => {
                const val = context.raw as number;
                return `Gasto/Ingreso: ${val}%`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: textColor, font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' } }
          },
          y: {
            suggestedMin: 0,
            suggestedMax: 100,
            grace: '15%',
            grid: { color: gridColor },
            ticks: {
              color: textColor,
              font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' },
              callback: (value) => `${value}%`
            }
          }
        }
      },
      plugins: [
        {
          id: 'limitLine',
          beforeDatasetsDraw: (chart) => {
            const { ctx, chartArea: { left, right }, scales: { y } } = chart;
            const yVal = y.getPixelForValue(80);
            ctx.save();
            ctx.beginPath();
            ctx.setLineDash([5, 5]);
            ctx.strokeStyle = '#ef4444';
            ctx.lineWidth = 1.5;
            ctx.moveTo(left, yVal);
            ctx.lineTo(right, yVal);
            ctx.stroke();
            
            // Texto arriba de la línea
            ctx.fillStyle = '#ef4444';
            ctx.font = 'bold 11px Inter, sans-serif';
            ctx.textAlign = 'right';
            ctx.fillText('Límite Saludable (80%)', right - 10, yVal - 6);
            ctx.restore();
          }
        },
        {
          id: 'dataLabels',
          afterDatasetsDraw: (chart) => {
            const { ctx } = chart;
            ctx.save();
            ctx.font = 'bold 11px Inter, sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'bottom';
            
            chart.data.datasets.forEach((dataset, i) => {
              const meta = chart.getDatasetMeta(i);
              if (!meta.hidden) {
                meta.data.forEach((element: any, index) => {
                  const val = dataset.data[index] as number;
                  if (val !== undefined && val !== null) {
                    const label = `${val}%`;
                    const { x, y } = element.tooltipPosition();
                    ctx.fillStyle = val > 80 ? '#ef4444' : '#10b981';
                    ctx.fillText(label, x, y - 6);
                  }
                });
              }
            });
            ctx.restore();
          }
        }
      ]
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const ratios = this.calcularRatios(data);
    const backgroundColors = ratios.map(r => r > 80 ? '#ef4444' : '#10b981');

    this.chart.data.labels = data.map(d => d.mes);
    this.chart.data.datasets[0].data = ratios;
    this.chart.data.datasets[0].backgroundColor = backgroundColors;

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
