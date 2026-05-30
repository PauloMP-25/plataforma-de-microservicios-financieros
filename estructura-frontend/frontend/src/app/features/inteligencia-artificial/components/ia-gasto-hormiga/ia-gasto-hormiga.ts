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
  nivelPeligro?: string;
  alternativa?: string;
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
  consejoTextoOriginal = signal<string>('');
  consejoHtml = signal<string>('');
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
    if (proy === 0) return 0; // Detenido
    if (proy <= 1000) return 24; // Lento
    if (proy <= 2500) return 12; // Normal
    if (proy <= 5000) return 4;  // Rápido
    return 1.5; // Muy rápido
  });

  relojCaption = computed(() => {
    const proy = this.proyeccionAnual();
    if (proy === 0) return 'Excelente. No se detectan fugas de dinero.';
    if (proy <= 1000) return 'Fuga bajo control, tus finanzas están seguras.';
    if (proy <= 2500) return 'Atención: Pequeñas fugas detectadas con el tiempo.';
    if (proy <= 5000) return 'Tu dinero se escapa más rápido de lo que crees.';
    return '¡CRÍTICO! Tu dinero está desapareciendo en tiempo récord.';
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
      (g: any, i: number) => {
        const peligros = ['CRÍTICO', 'ALTO', 'MEDIO', 'BAJO'];
        const alternativas = ['Preparar en casa', 'Buscar opción free', 'Reducir a la mitad', 'Cancelar suscripción'];
        return {
          descripcion: g.descripcion,
          frecuencia: g.frecuencia,
          total: g.total,
          categoria: g.categoria,
          promedio_por_compra: g.promedio_por_compra,
          dia_mayor_gasto: g.dia_mayor_gasto,
          nivelPeligro: peligros[Math.floor(Math.random() * peligros.length)],
          alternativa: alternativas[Math.floor(Math.random() * alternativas.length)],
          rotacion: (Math.random() * 6 - 3),
          delay: i * 200,
        };
      }
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

  // ── Animación Typewriter (Soporte HTML) ──
  private iniciarTypewriter(texto: string): void {
    if (!texto) return;
    this.consejoTextoOriginal.set(texto);
    this.consejoVisible.set(true);
    this.consejoHtml.set('');
    
    // Parsear Markdown básico
    let parsed = texto.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    parsed = parsed.replace(/\\n/g, '<br>').replace(/\n/g, '<br>');
    
    // Tokenizar HTML para la animación
    const tokens: { type: string, value: string }[] = [];
    let isTag = false;
    let currentTag = '';
    
    for (let i = 0; i < parsed.length; i++) {
      const char = parsed[i];
      if (char === '<') {
        isTag = true;
        currentTag = char;
      } else if (char === '>') {
        isTag = false;
        currentTag += char;
        tokens.push({ type: 'tag', value: currentTag });
        currentTag = '';
      } else if (isTag) {
        currentTag += char;
      } else {
        tokens.push({ type: 'text', value: char });
      }
    }

    let i = 0;
    const escribir = () => {
      if (i < tokens.length) {
        let toAdd = '';
        while (i < tokens.length && tokens[i].type === 'tag') {
          toAdd += tokens[i].value;
          i++;
        }
        if (i < tokens.length && tokens[i].type === 'text') {
          toAdd += tokens[i].value;
          i++;
        }
        this.consejoHtml.update(prev => prev + toAdd);
        this.typewriterTimeout = setTimeout(escribir, 12);
      }
    };

    // Esperar a que las tarjetas caigan
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

  // ── Mocks para pruebas ──
  cargarMock(cantidad: number) {
    this.hayHormigas.set(true);
    this.fugaTotal.set(cantidad * 150);
    this.proyeccionAnual.set(cantidad * 150 * 12);
    this.variacion.set(cantidad === 0 ? 0 : 5.2);
    this.principalSospechoso.set(cantidad === 0 ? 'Ninguno' : 'Mock Principal');
    
    const mockGastos = Array.from({ length: cantidad }).map((_, i) => ({
      descripcion: `Gasto Identificado ${i + 1}`,
      frecuencia: `${(i+1)*2} veces/mes`,
      total: (i+1)*50,
      categoria: ['Cafetería', 'Delivery', 'Suscripciones', 'Transporte'][i % 4],
      promedio_por_compra: 15.5,
      dia_mayor_gasto: 'Viernes'
    }));

    const sospechosos: SospechosoHormiga[] = mockGastos.map(
      (g: any, i: number) => {
        const peligros = ['CRÍTICO', 'ALTO', 'MEDIO', 'BAJO'];
        const alternativas = ['Preparar en casa', 'Buscar opción free', 'Reducir a la mitad', 'Cancelar suscripción'];
        return {
          descripcion: g.descripcion,
          frecuencia: g.frecuencia,
          total: g.total,
          categoria: g.categoria,
          promedio_por_compra: g.promedio_por_compra,
          dia_mayor_gasto: g.dia_mayor_gasto,
          nivelPeligro: peligros[Math.floor(Math.random() * peligros.length)],
          alternativa: alternativas[Math.floor(Math.random() * alternativas.length)],
          rotacion: (Math.random() * 6 - 3),
          delay: i * 200,
        };
      }
    );
    this.sospechosos.set(sospechosos);
    
    // Reiniciar animaciones
    if (this.animFrameId) cancelAnimationFrame(this.animFrameId);
    if (this.typewriterTimeout) clearTimeout(this.typewriterTimeout);
    
    setTimeout(() => this.iniciarOdometro(), 100);
    if (cantidad === 0) {
      this.iniciarTypewriter('¡Felicidades! No he encontrado gastos hormiga. Tus finanzas están en perfecto estado.');
    } else {
      this.iniciarTypewriter('He analizado tus gastos y hay pequeños consumos que están afectando tu meta. ¡Ajustemos esos hábitos y recuperemos el control! Puedes hacerlo.');
    }
  }
}
