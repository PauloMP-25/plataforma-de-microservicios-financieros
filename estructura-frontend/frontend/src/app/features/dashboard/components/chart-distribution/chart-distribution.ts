import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-distribution',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-distribution.html',
  styleUrls: ['./chart-distribution.scss']
})
export class ChartDistributionComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    effect(() => {
      const data = this.stateService.distribucionGastos();
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
    const canvas = document.getElementById('distributionCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.distribucionGastos();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: data.map(d => d.categoria),
        datasets: [{
          data: data.map(d => d.total),
          backgroundColor: data.map(d => d.color),
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '75%',
        plugins: {
          legend: {
            position: 'right',
            labels: {
              color: textColor,
              font: { family: 'Inter, sans-serif', size: 12 },
              usePointStyle: true,
              padding: 20
            }
          },
          tooltip: {
            backgroundColor: isDark ? 'rgba(30, 41, 59, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: isDark ? '#f8fafc' : '#0f172a',
            bodyColor: isDark ? '#cbd5e1' : '#475569',
            borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
            borderWidth: 1,
            padding: 12,
            callbacks: {
              label: (context: any) => {
                const dataset = context.dataset;
                const total = dataset.data.reduce((acc: number, current: number) => acc + current, 0);
                const value = dataset.data[context.dataIndex];
                const percentage = Math.round((value / total) * 100);
                return `${context.label}: ${value} (${percentage}%)`;
              }
            }
          }
        }
      } as any
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';

    this.chart.data.labels = data.map(d => d.categoria);
    this.chart.data.datasets[0].data = data.map(d => d.total);
    this.chart.data.datasets[0].backgroundColor = data.map(d => d.color);

    if (this.chart.options.plugins?.legend?.labels) {
      this.chart.options.plugins.legend.labels.color = textColor;
    }

    this.chart.update();
  }
}
