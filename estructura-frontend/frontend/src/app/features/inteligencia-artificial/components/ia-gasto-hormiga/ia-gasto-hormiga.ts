import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  signal,
  computed,
  AfterViewInit,
  OnDestroy,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

/**
 * Interfaz para cada "sospechoso" del tablero de crímenes financieros.
 */
export interface SospechosoHormiga {
  descripcion: string;
  frecuencia: string;
  total: number;
  categoria: string;
  promedio_por_compra?: number;
  dia_mayor_gasto?: string;
  rotacion: number; // grados de inclinación aleatorios (-3° a +3°)
  delay: number;    // delay de animación staggered (ms)
}

@Component({
  selector: 'app-ia-gasto-hormiga',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-gasto-hormiga.html',
  styleUrl: './ia-gasto-hormiga.scss',
})
export class IaGastoHormigaComponent implements OnChanges, AfterViewInit, OnDestroy {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  @ViewChild('odometroValor') odometroRef!: ElementRef<HTMLSpanElement>;

  // ── Signals Reactivos ──
  sospechosos = signal<SospechosoHormiga[]>([]);
  fugaTotal = signal<number>(0);
  proyeccionAnual = signal<number>(0);
  variacion = signal<number>(0);
  hayHormigas = signal<boolean>(false);
  principalSospechoso = signal<string>('N/A');
  ingresoReferencia = signal<number>(3200);
  cardHoveredIndex = signal<number | null>(null);
  odometroDisplay = signal<string>('0');
  consejoTexto = signal<string>('');
  consejoVisible = signal<boolean>(false);

  // ── Computed ──
  esAlertaCritica = computed(() => {
    const ingreso = this.ingresoReferencia();
    if (ingreso <= 0) return false;
    return (this.fugaTotal() / ingreso) * 100 > 15;
  });

  porcentajeFuga = computed(() => {
    const ingreso = this.ingresoReferencia();
    if (ingreso <= 0) return 0;
    return Math.round((this.fugaTotal() / ingreso) * 100);
  });

  /** Velocidad del reloj: más rápido a mayor proyección */
  velocidadReloj = computed(() => {
    const proy = this.proyeccionAnual();
    if (proy <= 500) return 12;
    if (proy <= 1500) return 6;
    if (proy <= 3000) return 3;
    return 1.5;
  });

  private animFrameId = 0;
  private typewriterTimeout: any = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resultado'] && this.resultado) {
      this.procesarResultado();
    }
  }

  ngAfterViewInit(): void {
    if (this.resultado && !this.cargando) {
      setTimeout(() => this.iniciarOdometro(), 800);
    }
  }

  ngOnDestroy(): void {
    if (this.animFrameId) cancelAnimationFrame(this.animFrameId);
    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
  }

  // ── Procesamiento de Datos ──
  private procesarResultado(): void {
    const insight = this.resultado?.insight;
    if (!insight) return;

    this.hayHormigas.set(insight.hay_hormigas ?? false);
    this.fugaTotal.set(insight.total_gastos_hormiga ?? 0);
    this.proyeccionAnual.set(insight.proyeccion_fuga_anual ?? 0);
    this.variacion.set(insight.variacion_vs_mes_anterior ?? 0);
    this.principalSospechoso.set(insight.principal_gasto_hormiga ?? 'N/A');
    this.ingresoReferencia.set(insight.ingreso_mensual_referencia ?? 3200);

    // Construir sospechosos con rotación aleatoria
    const detectados = insight.gastos_detectados || [];
    const sospechosos: SospechosoHormiga[] = detectados.map(
      (g: any, i: number) => ({
        descripcion: g.descripcion,
        frecuencia: g.frecuencia,
        total: g.total,
        categoria: g.categoria,
        promedio_por_compra: g.promedio_por_compra,
        dia_mayor_gasto: g.dia_mayor_gasto,
        rotacion: (Math.random() * 6 - 3),
        delay: i * 200,
      })
    );
    this.sospechosos.set(sospechosos);

    // Animaciones
    setTimeout(() => this.iniciarOdometro(), 800);
    this.iniciarTypewriter(this.resultado?.consejo ?? '');
  }

  // ── Animación Odómetro Roll-Up ──
  private iniciarOdometro(): void {
    const target = this.fugaTotal();
    const duration = 2000;
    const startTime = performance.now();

    const animar = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // Easing: ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      const valor = eased * target;
      this.odometroDisplay.set(valor.toFixed(2));

      if (progress < 1) {
        this.animFrameId = requestAnimationFrame(animar);
      }
    };

    this.animFrameId = requestAnimationFrame(animar);
  }

  // ── Animación Typewriter para el consejo ──
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

    // Esperar a que las tarjetas hayan caído antes de mostrar el consejo
    this.typewriterTimeout = setTimeout(escribir, 1500);
  }

  // ── Hover de tarjetas ──
  onCardHover(index: number): void {
    this.cardHoveredIndex.set(index);
  }

  onCardLeave(): void {
    this.cardHoveredIndex.set(null);
  }

  // ── Helpers del template ──
  getIconoCategoria(categoria: string): string {
    const iconos: Record<string, string> = {
      'Cafetería': 'fa-solid fa-mug-hot',
      'Café': 'fa-solid fa-mug-hot',
      'Delivery': 'fa-solid fa-motorcycle',
      'Suscripciones': 'fa-solid fa-credit-card',
      'Apps': 'fa-solid fa-mobile-screen',
      'Entretenimiento': 'fa-solid fa-gamepad',
      'Snacks': 'fa-solid fa-cookie-bite',
      'Transporte': 'fa-solid fa-taxi',
    };
    return iconos[categoria] || 'fa-solid fa-money-bill-wave';
  }

  getTendenciaTexto(): string {
    const v = this.variacion();
    if (v > 0) return `+${v.toFixed(1)}% vs mes anterior`;
    if (v < 0) return `${v.toFixed(1)}% vs mes anterior`;
    return 'Sin variación vs mes anterior';
  }

  getTendenciaClase(): string {
    const v = this.variacion();
    if (v > 5) return 'tendencia-peligro';
    if (v > 0) return 'tendencia-alerta';
    if (v < 0) return 'tendencia-bien';
    return 'tendencia-neutral';
  }
}
