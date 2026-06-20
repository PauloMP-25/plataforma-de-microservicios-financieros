import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
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

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    effect(() => {
      const data = this.stateService.comparativa();
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
    const canvas = document.getElementById('historicalComparisonCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.comparativa();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: data.map(d => d.mes),
        datasets: [
          {
            label: 'Este Año',
            data: data.map(d => d.actual),
            backgroundColor: '#5b6af0',
            borderRadius: 4
          },
          {
            label: 'Año Anterior',
            data: data.map(d => d.anterior),
            backgroundColor: isDark ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)',
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            labels: { color: textColor, font: { family: 'Inter, sans-serif' } }
          },
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
            ticks: { color: textColor, font: { family: 'Inter, sans-serif' } }
          },
          y: {
            grid: { color: gridColor },
            ticks: { color: textColor, font: { family: 'Inter, sans-serif' } }
          }
        }
      }
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    this.chart.data.labels = data.map(d => d.mes);
    this.chart.data.datasets[0].data = data.map(d => d.actual);
    this.chart.data.datasets[1].data = data.map(d => d.anterior);
    this.chart.data.datasets[1].backgroundColor = isDark ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)';

    if (this.chart.options.plugins?.legend?.labels) {
      this.chart.options.plugins.legend.labels.color = textColor;
    }
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
