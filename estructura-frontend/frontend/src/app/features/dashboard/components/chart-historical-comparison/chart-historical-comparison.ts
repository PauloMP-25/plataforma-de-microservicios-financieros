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

  tituloComparativa = computed(() => {
    return this.esFiltroSemanal() ? 'Comparativa Semanal' : 'Comparativa Mensual';
  });

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

  private esFiltroSemanal(): boolean {
    const filtros = this.stateService.filtrosActuales();
    if (filtros.fechaInicio && filtros.fechaFin) {
      const start = new Date(filtros.fechaInicio);
      const end = new Date(filtros.fechaFin);
      // Si el día de inicio es 1 (ej. "este mes"), no es semanal, sino mensual
      if (start.getDate() === 1) {
        return false;
      }
      const diffTime = Math.abs(end.getTime() - start.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      return diffDays <= 7;
    }
    return false;
  }

  private esFiltroMensual(): boolean {
    const filtros = this.stateService.filtrosActuales();
    if (filtros.fechaInicio && filtros.fechaFin) {
      const start = new Date(filtros.fechaInicio);
      const end = new Date(filtros.fechaFin);
      if (start.getDate() === 1) {
        return true;
      }
      const diffTime = Math.abs(end.getTime() - start.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      return diffDays > 7 && diffDays <= 31;
    }
    return false;
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

    const esSemana = this.esFiltroSemanal();
    const esMes = this.esFiltroMensual();
    const labelActual = esSemana ? 'Esta Semana' : (esMes ? 'Este Mes' : 'Este Año');
    const labelAnterior = esSemana ? 'Semana Anterior' : (esMes ? 'Mes Anterior' : 'Año Anterior');

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: data.map(d => d.mes),
        datasets: [
          {
            label: labelActual,
            data: data.map(d => d.actual),
            backgroundColor: '#5b6af0',
            borderRadius: 4
          },
          {
            label: labelAnterior,
            data: data.map(d => d.anterior),
            backgroundColor: isDark ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)',
            borderRadius: 4
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        layout: {
          padding: {
            left: 15,
            right: 15,
            top: 20,
            bottom: 5
          }
        },
        plugins: {
          legend: {
            labels: { color: textColor, font: { family: 'Inter, sans-serif', size: 13, weight: 'bold' } }
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
                  const label = `S/ ${val.toLocaleString()}`;
                  const { x, y } = element.tooltipPosition();
                  ctx.fillStyle = i === 0 ? '#5b6af0' : (isDark ? '#94a3b8' : '#64748b');
                  ctx.fillText(label, x, y - 6);
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

    const esSemana = this.esFiltroSemanal();
    const esMes = this.esFiltroMensual();
    this.chart.data.datasets[0].label = esSemana ? 'Esta Semana' : (esMes ? 'Este Mes' : 'Este Año');
    this.chart.data.datasets[1].label = esSemana ? 'Semana Anterior' : (esMes ? 'Mes Anterior' : 'Año Anterior');

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
