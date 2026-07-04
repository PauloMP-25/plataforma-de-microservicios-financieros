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

  private getCategoriaColor(categoria: string): string {
    const cat = categoria.toLowerCase().trim();
    const colores: Record<string, string> = {
      'vivienda': '#3b82f6', // Azul
      'hogar': '#3b82f6',
      'alimentación': '#ff7043', // Naranja
      'alimentacion': '#ff7043',
      'comida': '#ff7043',
      'tecnología': '#8b5cf6', // Violeta
      'tecnologia': '#8b5cf6',
      'viajes': '#ef4444', // Rojo
      'viaje': '#ef4444',
      'educación': '#10b981', // Verde
      'educacion': '#10b981',
      'salud': '#06b6d4', // Celeste
      'ropa y calzado': '#f59e0b', // Ámbar
      'ropa': '#f59e0b',
      'calzado': '#f59e0b',
      'ocio': '#ec4899', // Rosado
      'leisure': '#ec4899',
      'entretenimiento': '#ec4899',
      'suscripciones': '#6366f1', // Indigo
      'suscripciones streaming': '#a855f7', // Púrpura
      'servicios': '#14b8a6', // Teal
      'transporte': '#0ea5e9', // Sky blue
      'pasaje moto': '#26c6da',
      'inversiones': '#10b981',
      'transferencia': '#f59e0b',
      'otros': '#859397',
      'otros gastos': '#859397'
    };

    return colores[cat] || this.generarColorPorDefecto(cat);
  }

  private generarColorPorDefecto(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const h = Math.abs(hash % 360);
    return `hsl(${h}, 65%, 55%)`;
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
          backgroundColor: data.map(d => this.getCategoriaColor(d.categoria)),
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '75%',
        layout: {
          padding: {
            left: 10,
            right: 10,
            top: 10,
            bottom: 10
          }
        },
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: textColor,
              font: { family: 'Inter, sans-serif', size: 13, weight: 'bold' },
              usePointStyle: true,
              padding: 16
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
                return `${context.label}: S/ ${value.toLocaleString()} (${percentage}%)`;
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
    this.chart.data.datasets[0].backgroundColor = data.map(d => this.getCategoriaColor(d.categoria));

    if (this.chart.options.plugins?.legend?.labels) {
      this.chart.options.plugins.legend.labels.color = textColor;
    }

    this.chart.update();
  }
}
