import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-heatmap',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-heatmap.html',
  styleUrls: ['./chart-heatmap.scss']
})
export class ChartHeatmapComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    effect(() => {
      const data = this.stateService.heatmap();
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
    const canvas = document.getElementById('heatmapCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.heatmap();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    // Se usa un gráfico de barras como pseudo heatmap mapeando la intensidad a la opacidad/color
    const backgroundColors = data.map(d => {
      const opacity = Math.min(Math.max(d.intensidad / 10, 0.2), 1);
      return `rgba(236, 72, 153, ${opacity})`;
    });

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: data.map(d => d.dia),
        datasets: [{
          label: 'Gastos Registrados',
          data: data.map(d => d.intensidad),
          backgroundColor: backgroundColors,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
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
            grace: '25%',
            suggestedMax: 12,
            grid: { color: gridColor },
            ticks: { display: false } // No mostrar números para dar efecto heatmap
          }
        }
      },
      plugins: [{
        id: 'dataLabels',
        afterDatasetsDraw: (chart) => {
          const { ctx } = chart;
          ctx.save();
          ctx.font = 'bold 11px Inter, sans-serif';
          ctx.fillStyle = isDark ? '#cbd5e1' : '#475569';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'bottom';
          
          chart.data.datasets.forEach((dataset, i) => {
            const meta = chart.getDatasetMeta(i);
            if (!meta.hidden) {
              meta.data.forEach((element: any, index) => {
                const val = dataset.data[index] as number;
                if (val !== undefined && val !== null && val > 0) {
                  const label = `${val}`;
                  const { x, y } = element.tooltipPosition();
                  ctx.fillText(label, x, y - 4);
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

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const backgroundColors = data.map(d => {
      const opacity = Math.min(Math.max(d.intensidad / 10, 0.2), 1);
      return `rgba(236, 72, 153, ${opacity})`;
    });

    this.chart.data.labels = data.map(d => d.dia);
    this.chart.data.datasets[0].data = data.map(d => d.intensidad);
    this.chart.data.datasets[0].backgroundColor = backgroundColors;

    if (this.chart.options.scales?.['x']) {
      (this.chart.options.scales['x'] as any).ticks.color = textColor;
    }
    if (this.chart.options.scales?.['y']) {
      (this.chart.options.scales['y'] as any).grid.color = gridColor;
    }

    this.chart.update();
  }
}
