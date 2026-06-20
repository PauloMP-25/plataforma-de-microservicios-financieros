import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-fixed-vs-variable',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-fixed-vs-variable.html',
  styleUrls: ['./chart-fixed-vs-variable.scss']
})
export class ChartFixedVsVariableComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    effect(() => {
      const data = this.stateService.fijoVariable();
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
    const canvas = document.getElementById('fijoVariableCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.fijoVariable();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Gastos'], // Barra única apilada o dos barras separadas
        datasets: data.map(d => ({
          label: d.tipo,
          data: [d.monto],
          backgroundColor: d.tipo === 'FIJO' ? '#3b82f6' : '#f59e0b',
          borderRadius: 4
        }))
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y', // Barras horizontales
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
            stacked: true,
            display: false // Ocultar eje X
          },
          y: {
            stacked: true,
            display: false // Ocultar eje Y
          }
        }
      }
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';

    this.chart.data.datasets = data.map(d => ({
      label: d.tipo,
      data: [d.monto],
      backgroundColor: d.tipo === 'FIJO' ? '#3b82f6' : '#f59e0b',
      borderRadius: 4
    }));

    if (this.chart.options.plugins?.legend?.labels) {
      this.chart.options.plugins.legend.labels.color = textColor;
    }

    this.chart.update();
  }
}
