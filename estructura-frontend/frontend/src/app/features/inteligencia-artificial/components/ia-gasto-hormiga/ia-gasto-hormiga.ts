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
import { RespuestaModuloDTO, ConsejoEstructuradoHormiga } from '../../../../core/models/ia_coach/ia-base.model';

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
  
  // Consejo estructurado
  consejoObj = signal<ConsejoEstructuradoHormiga | null>(null);
  consejoVisible = signal<boolean>(false);
  mostrarTodosLosPasos = signal<boolean>(false);

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
  private revealTimeout: any = null;

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
    if (this.revealTimeout) clearTimeout(this.revealTimeout);
  }

  // ── Procesamiento de Datos ──
  private procesarResultado(): void {
    const insight = this.resultado?.insight;
    if (!insight) return;

    // Adaptabilidad para leer de backend real (insight.hallazgos) o mockup (insight)
    const source = insight.hallazgos ? insight.hallazgos : insight;

    this.hayHormigas.set(source.hay_hormigas ?? false);
    this.fugaTotal.set(source.total_gastos_hormiga ?? 0);
    this.proyeccionAnual.set(source.proyeccion_fuga_anual ?? 0);
    this.variacion.set(source.variacion_vs_mes_anterior ?? 0);
    this.principalSospechoso.set(source.principal_gasto_hormiga ?? 'N/A');
    this.ingresoReferencia.set(source.ingreso_mensual_referencia ?? 3200);

    // Construir sospechosos con rotación aleatoria
    const detectados = source.gastos_detectados || [];
    
    // Primero, ordenar por total descendente
    const detectadosOrdenados = [...detectados].sort((a: any, b: any) => (b.total || 0) - (a.total || 0));

    const sospechosos: SospechosoHormiga[] = detectadosOrdenados.map(
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
          nivelPeligro: peligros[Math.min(i, peligros.length - 1)], // Peligro según el tamaño del gasto
          alternativa: alternativas[Math.floor(Math.random() * alternativas.length)],
          rotacion: (Math.random() * 6 - 3),
          delay: i * 200,
        };
      }
    );
    this.sospechosos.set(sospechosos);

    // Animaciones
    setTimeout(() => this.iniciarOdometro(), 800);
    
    const consejoRaw = this.resultado?.consejo;
    if (typeof consejoRaw === 'object' && consejoRaw !== null) {
      this.consejoObj.set(consejoRaw as ConsejoEstructuradoHormiga);
      this.revealTimeout = setTimeout(() => {
        this.consejoVisible.set(true);
      }, 1500);
    }
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
    
    const mockGastos = Array.from({ length: cantidad }).map((_, i) => ({
      descripcion: `Gasto Identificado ${i + 1}`,
      frecuencia: `${(i+1)*2} veces/mes`,
      total: (i+1)*50,
      categoria: ['Cafetería', 'Delivery', 'Suscripciones', 'Transporte'][i % 4],
      promedio_por_compra: 15.5,
      dia_mayor_gasto: 'Viernes'
    }));

    // Primero, ordenar por total descendente
    const mockGastosOrdenados = [...mockGastos].sort((a: any, b: any) => b.total - a.total);

    this.principalSospechoso.set(cantidad === 0 ? 'Ninguno' : mockGastosOrdenados[0].categoria);

    const sospechosos: SospechosoHormiga[] = mockGastosOrdenados.map(
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
          nivelPeligro: peligros[Math.min(i, peligros.length - 1)],
          alternativa: alternativas[Math.floor(Math.random() * alternativas.length)],
          rotacion: (Math.random() * 6 - 3),
          delay: i * 200,
        };
      }
    );
    this.sospechosos.set(sospechosos);
    
    this.mostrarTodosLosPasos.set(false);

    // Reiniciar animaciones
    if (this.animFrameId) cancelAnimationFrame(this.animFrameId);
    if (this.revealTimeout) clearTimeout(this.revealTimeout);
    
    setTimeout(() => this.iniciarOdometro(), 100);
    if (cantidad === 0) {
      this.consejoObj.set(null);
      this.consejoVisible.set(false);
    } else {
      this.consejoObj.set({
        pensamiento_interno_ia: "He analizado los gastos hormiga de Cesar, notando una mejora en la reducción de fugas. Mi objetivo es ofrecer 5 pasos prácticos y conectar la fuga con su meta.",
        analisis_ia: "¡Hola, Cesar! He estado revisando tus movimientos este mes. Tus gastos hormiga suman S/ 181.95, con el transporte siendo la categoría principal de fuga. ¡Pero hay buenas noticias! Has logrado disminuir tus gastos hormiga en un 11.2% respecto al mes anterior. Sin embargo, si esta tendencia de fuga se mantiene, podrías estar perdiendo S/ 2,183.40 al año.",
        conexion_emocional: "Esos S/ 181.95 que se te escaparon este mes, y la proyección anual de S/ 2,183.40, podrían ser una parte de tus ahorros para tu 'Viaje a Japón'. ¡Imagina todo lo que harías con ese dinero allá!",
        plan_accion_titulo: "Plan de Acción: ¡Ahorra en Transporte para Japón!",
        plan_accion_pasos: [
          "Identifica rutas alternativas o considera caminar/bicicleta para trayectos cortos.",
          "Revisa tus gastos de transporte diarios y busca al menos una forma de optimizarlos esta semana (ej. usar transporte público en lugar de taxi).",
          "Establece un presupuesto semanal específico para transporte y monitorea que no lo excedas.",
          "Organiza viajes compartidos con compañeros que tengan rutas similares para dividir los costos.",
          "Camina en los tramos cortos de menos de 10 cuadras; además de ahorrar, beneficiará tu salud diaria."
        ],
        comentario_positivo: "¡Sigue así, Cesar! Cada pequeño ajuste te acerca más a tu increíble viaje a Japón. ¡Estoy aquí para apoyarte en cada paso!"
      });
      this.revealTimeout = setTimeout(() => {
        this.consejoVisible.set(true);
      }, 1500);
    }
  }

  togglePasos() {
    this.mostrarTodosLosPasos.set(!this.mostrarTodosLosPasos());
  }
}
