import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  signal,
  computed,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

@Component({
  selector: 'app-ia-prediccion-gastos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-prediccion-gastos.html',
  styleUrl: './ia-prediccion-gastos.scss',
})
export class IaPrediccionGastosComponent implements OnChanges, OnDestroy {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // ── Signals reactivos ──
  promedioHistorico = signal<number>(0);
  proyeccionProximoMes = signal<number>(0);
  pendiente = signal<'SUBE' | 'BAJA' | 'ESTABLE'>('ESTABLE');
  porcentajeVariacion = signal<number>(0);
  deficitEstimado = signal<number>(0);
  historialMeses = signal<number[]>([]);
  ingresoEsperado = signal<number>(2000.00);
  
  consejoTexto = signal<string>('');
  consejoVisible = signal<boolean>(false);

  private typewriterTimeout: any = null;

  // ── Computes para Graficado SVG Autónomo ──
  chartPoints = computed(() => {
    const hist = this.historialMeses();
    const proj = this.proyeccionProximoMes();
    if (hist.length === 0) {
      return { puntos: [], puntoProj: { x: 0, y: 0, val: 0 } };
    }
    
    // Combinar todo para calcular escalas
    const todos = [...hist, proj];
    const minVal = Math.min(...todos) * 0.95; // 5% de margen inferior
    const maxVal = Math.max(...todos) * 1.05; // 5% de margen superior
    const rango = maxVal - minVal || 1;

    const width = 240;
    const height = 140;
    const paddingX = 20;
    const paddingY = 20;

    const graphWidth = width - 2 * paddingX;
    const graphHeight = height - 2 * paddingY;

    // Puntos históricos
    const puntos = hist.map((val, index) => {
      const x = paddingX + (index / hist.length) * graphWidth;
      const y = height - paddingY - ((val - minVal) / rango) * graphHeight;
      return { x, y, val };
    });

    // Punto proyectado (se coloca al final)
    const xProj = width - paddingX;
    const yProj = height - paddingY - ((proj - minVal) / rango) * graphHeight;
    const puntoProj = { x: xProj, y: yProj, val: proj };

    return { puntos, puntoProj };
  });

  // Trazar línea de historial (curva suave)
  svgPathHistorial = computed(() => {
    const data = this.chartPoints();
    if (!data || !data.puntos || data.puntos.length === 0) return '';
    
    const pts = data.puntos;
    let path = `M ${pts[0].x} ${pts[0].y}`;
    
    for (let i = 1; i < pts.length; i++) {
      const xc = (pts[i - 1].x + pts[i].x) / 2;
      const yc = (pts[i - 1].y + pts[i].y) / 2;
      path += ` Q ${pts[i - 1].x} ${pts[i - 1].y}, ${xc} ${yc}`;
    }
    
    // Conectar el último tramo al último punto histórico
    const last = pts[pts.length - 1];
    path += ` L ${last.x} ${last.y}`;
    return path;
  });

  // Trazar línea de proyección (conecta último histórico con proyectado)
  svgPathProyeccion = computed(() => {
    const data = this.chartPoints();
    if (!data || !data.puntos || data.puntos.length === 0) return '';
    
    const lastHist = data.puntos[data.puntos.length - 1];
    const proj = data.puntoProj;
    
    return `M ${lastHist.x} ${lastHist.y} L ${proj.x} ${proj.y}`;
  });

  // Area bajo la curva del historial para el gradiente
  svgAreaHistorial = computed(() => {
    const histPath = this.svgPathHistorial();
    const data = this.chartPoints();
    if (!histPath || !data || !data.puntos || data.puntos.length === 0) return '';
    
    const pts = data.puntos;
    const first = pts[0];
    const last = pts[pts.length - 1];
    
    // Cerrar la forma por abajo del gráfico
    return `${histPath} L ${last.x} 140 L ${first.x} 140 Z`;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resultado'] && this.resultado) {
      this.procesarResultado();
    }
  }

  ngOnDestroy(): void {
    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
  }

  private procesarResultado(): void {
    const insight = this.resultado?.insight;
    if (!insight) return;

    this.promedioHistorico.set(insight.promedio_historico ?? 0);
    this.proyeccionProximoMes.set(insight.proyeccion_proximo_mes ?? 0);
    this.pendiente.set(insight.pendiente ?? 'ESTABLE');
    this.porcentajeVariacion.set(insight.porcentaje_variacion_mensual ?? 0);
    this.deficitEstimado.set(insight.deficit_estimado ?? 0);
    this.historialMeses.set(insight.historial_meses ?? []);
    this.ingresoEsperado.set(insight.ingreso_esperado ?? 2000.00);

    this.iniciarTypewriter(this.resultado?.consejo ?? '');
  }

  private iniciarTypewriter(texto: string): void {
    if (!texto) return;
    this.consejoTexto.set('');
    this.consejoVisible.set(true);
    let i = 0;

    const escribir = () => {
      if (i < texto.length) {
        this.consejoTexto.set(texto.substring(0, i + 1));
        i++;
        this.typewriterTimeout = setTimeout(escribir, 12);
      }
    };

    // Retardo para dejar ver las órbitas y el oráculo cargando
    this.typewriterTimeout = setTimeout(escribir, 1000);
  }

  getEscudoClase(): string {
    return this.deficitEstimado() > 0 ? 'escudo-peligro' : 'escudo-exito';
  }

  getEscudoIcono(): string {
    return this.deficitEstimado() > 0 ? 'fa-solid fa-shield-xmark' : 'fa-solid fa-shield-halved';
  }

  getTendenciaIcono(): string {
    const p = this.pendiente();
    if (p === 'SUBE') return 'fa-solid fa-arrow-trend-up';
    if (p === 'BAJA') return 'fa-solid fa-arrow-trend-down';
    return 'fa-solid fa-arrows-left-right';
  }

  getTendenciaClase(): string {
    const p = this.pendiente();
    if (p === 'SUBE') return 'tendencia-sube';
    if (p === 'BAJA') return 'tendencia-baja';
    return 'tendencia-estable';
  }
}
