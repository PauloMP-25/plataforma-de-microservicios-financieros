import { Component, AfterViewInit, OnDestroy, effect, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration } from 'chart.js/auto';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';
import { ServicioTema } from '../../../../core/services/servicio-tema';

@Component({
  selector: 'app-chart-goals-progress',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chart-goals-progress.html',
  styleUrls: ['./chart-goals-progress.scss']
})
export class ChartGoalsProgressComponent implements AfterViewInit, OnDestroy {
  private chart: Chart | undefined;

  activeMeta = computed(() => {
    const list = this.stateService.metas();
    return list && list.length > 0 ? list[0] : null;
  });

  constructor(
    private stateService: DashboardStateService,
    private themeService: ServicioTema
  ) {
    effect(() => {
      const data = this.stateService.metas();
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
    const canvas = document.getElementById('goalsProgressCanvas') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.stateService.metas();
    const isDark = this.themeService.temaOscuro();
    const bgRemaining = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';

    const meta = data && data.length > 0 ? data[0] : null;
    const remaining = meta ? meta.objetivo - meta.actual : 0;
    const actualData = meta ? [meta.actual, remaining > 0 ? remaining : 0] : [];
    const bgColors = meta ? [meta.color, bgRemaining] : [bgRemaining, bgRemaining];

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: ['Completado', 'Restante'],
        datasets: [{
          data: actualData,
          backgroundColor: bgColors,
          borderWidth: 0,
          cutout: '80%',
          circumference: 270,
          rotation: 225,
        } as any]
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
        } as any
      },
      plugins: [{
        id: 'centerText',
        beforeDraw: (chart) => {
          const { width, height, ctx } = chart;
          ctx.restore();
          
          const currentMeta = this.activeMeta();
          if (!currentMeta) return;

          const fontSize = (height / 85).toFixed(2);
          ctx.font = `bold ${fontSize}em Inter, sans-serif`;
          ctx.textBaseline = 'middle';
          ctx.fillStyle = isDark ? '#f8fafc' : '#0f172a';
          
          const text = `${Math.round(currentMeta.porcentaje)}%`;
          const textX = Math.round((width - ctx.measureText(text).width) / 2);
          const textY = height / 2;
          
          ctx.fillText(text, textX, textY);
          ctx.save();
        }
      }]
    };

    this.chart = new Chart(ctx, config);
  }

  private actualizarGrafico(data: any[], isDark: boolean): void {
    if (!this.chart) return;
    
    const bgRemaining = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';
    
    if (!data || data.length === 0) {
      this.chart.data.datasets[0].data = [];
      this.chart.update();
      return;
    }
    
    const meta = data[0];
    const remaining = meta.objetivo - meta.actual;

    this.chart.data.datasets[0].data = [meta.actual, remaining > 0 ? remaining : 0];
    this.chart.data.datasets[0].backgroundColor = [meta.color, bgRemaining];

    this.chart.update();
  }
}
