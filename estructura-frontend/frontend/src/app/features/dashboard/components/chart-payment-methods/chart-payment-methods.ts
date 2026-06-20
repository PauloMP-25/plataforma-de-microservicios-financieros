import { Component, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-payment-methods',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-payment-methods.html',
  styleUrls: ['./chart-payment-methods.scss']
})
export class ChartPaymentMethodsComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    // Escuchar reactivamente los datos de transacciones por método de pago
    effect(() => {
      const data = this.stateService.transaccionesMetodo();
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
    const canvas = document.getElementById('paymentMethodsCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.transaccionesMetodo();
    const isDark = this.themeService.temaOscuro();
    const textColor = isDark ? '#94a3b8' : '#64748b';

    const labels = data.map(d => `${d.metodo}`);
    const counts = data.map(d => d.cantidad);
    const bgColors = data.map(d => d.color);

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: counts,
          backgroundColor: bgColors,
          borderWidth: 0,
          cutout: '72%'
        } as any]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
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
            position: 'right',
            labels: {
              color: textColor,
              font: { family: 'Inter, sans-serif', size: 12, weight: 'bold' },
              padding: 12,
              generateLabels: (chart) => {
                const data = chart.data;
                if (data.labels?.length && data.datasets.length) {
                  return data.labels.map((label, i) => {
                    const meta = chart.getDatasetMeta(0);
                    const style = meta.controller.getStyle(i, false) as any;
                    const val = data.datasets[0].data[i] as number;
                    return {
                      text: `${label}: ${val} trans.`,
                      fillStyle: style['backgroundColor'] as string,
                      strokeStyle: style['borderColor'] as string,
                      lineWidth: style['borderWidth'] as number,
                      hidden: isNaN(data.datasets[0].data[i] as number) || !chart.getDataVisibility(i),
                      index: i
                    };
                  });
                }
                return [];
              }
            }
          },
          tooltip: {
            backgroundColor: isDark ? 'rgba(30, 41, 59, 0.9)' : 'rgba(255, 255, 255, 0.9)',
            titleColor: isDark ? '#f8fafc' : '#0f172a',
            bodyColor: isDark ? '#cbd5e1' : '#475569',
            borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
            borderWidth: 1,
            callbacks: {
              label: (context: any) => {
                const val = context.raw as number;
                return ` Cantidad: ${val} transacciones`;
              }
            }
          }
        }
      },
      plugins: [{
        id: 'centerText',
        beforeDraw: (chart) => {
          const { width, height, ctx } = chart;
          ctx.save();
          
          const dataset = chart.data.datasets[0];
          if (!dataset || !dataset.data || dataset.data.length === 0) {
            ctx.restore();
            return;
          }
          
          const total = dataset.data.reduce((sum: number, val: any) => sum + (val || 0), 0) as number;

          // Obtener el centro visual exacto del doughnut
          const meta = chart.getDatasetMeta(0);
          let centerX = width / 2;
          let centerY = height / 2;
          
          if (meta && meta.data && meta.data[0]) {
            const firstArc = meta.data[0] as any;
            if (typeof firstArc.x === 'number' && typeof firstArc.y === 'number') {
              centerX = firstArc.x;
              centerY = firstArc.y;
            }
          }

          // Dibujar la cantidad total en grande
          const valFontSize = (height / 120).toFixed(2);
          ctx.font = `bold ${valFontSize}em Inter, sans-serif`;
          ctx.textBaseline = 'middle';
          ctx.textAlign = 'center';
          ctx.fillStyle = isDark ? '#f8fafc' : '#0f172a';
          
          const textVal = `${total}`;
          ctx.fillText(textVal, centerX, centerY - 12);
          
          // Dibujar la palabra "Trans." abajo
          const lblFontSize = (height / 190).toFixed(2);
          ctx.font = `600 ${lblFontSize}em Inter, sans-serif`;
          ctx.fillStyle = isDark ? '#94a3b8' : '#64748b';
          
          const textLabel = `Trans.`;
          ctx.fillText(textLabel, centerX, centerY + 14);
          
          ctx.restore();
        }
      }]
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const textColor = isDark ? '#94a3b8' : '#64748b';

    if (!data || data.length === 0) {
      this.chart.data.datasets[0].data = [];
      this.chart.data.labels = [];
      this.chart.update();
      return;
    }

    const labels = data.map(d => `${d.metodo}`);
    const counts = data.map(d => d.cantidad);
    const bgColors = data.map(d => d.color);

    this.chart.data.labels = labels;
    this.chart.data.datasets[0].data = counts;
    this.chart.data.datasets[0].backgroundColor = bgColors;

    if (this.chart.options.plugins?.legend?.labels) {
      this.chart.options.plugins.legend.labels.color = textColor;
    }

    this.chart.update();
  }
}
