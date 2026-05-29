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

export interface DimensionDetalle {
  etiqueta: string;
  valor: number;
  consejo: string;
}

export interface CeldaHeatmap {
  fecha: string;
  diaSemana: string; // 'Lunes' | 'Martes' | etc.
  diaNum: number;
  monto: number;
  nivel: number; // 1 (bajo) a 5 (alto)
  transacciones: { descripcion: string; monto: number; categoria: string; icono: string }[];
}

@Component({
  selector: 'app-ia-habitos-financieros',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-habitos-financieros.html',
  styleUrl: './ia-habitos-financieros.scss',
})
export class IaHabitosFinancierosComponent implements OnChanges, OnDestroy {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // ── Signals reactivos ──
  semanaSeleccionada = signal<'ESTA_SEMANA' | 'SEMANA_PASADA'>('ESTA_SEMANA');
  dimensionSeleccionada = signal<string | null>(null);
  celdaHover = signal<CeldaHeatmap | null>(null);
  tooltipPos = signal<{ x: number; y: number }>({ x: 0, y: 0 });
  
  consejoTexto = signal<string>('');
  consejoVisible = signal<boolean>(false);
  
  puntuacionHabito = signal<number>(72);
  esSaludable = signal<boolean>(false);
  diaMayorGasto = signal<string>('Saturday');
  categoriaMasFrecuente = signal<string>('Restaurantes');
  totalTransacciones = signal<number>(18);

  dimensionesKeys: string[] = ['Constancia', 'Ahorro', 'Control', 'Diversidad', 'Puntualidad', 'Equilibrio'];
  
  dimensionesConfig: Record<string, { label: string; icon: string }> = {
    Constancia: { label: 'Constancia', icon: 'fa-calendar-check' },
    Ahorro: { label: 'Ahorro', icon: 'fa-piggy-bank' },
    Control: { label: 'Control', icon: 'fa-sliders' },
    Diversidad: { label: 'Diversidad', icon: 'fa-circle-nodes' },
    Puntualidad: { label: 'Puntualidad', icon: 'fa-clock' },
    Equilibrio: { label: 'Equilibrio', icon: 'fa-scale-balanced' }
  };

  private typewriterTimeout: any = null;

  // ── Parámetros SVG Radar ──
  cx = 150;
  cy = 150;
  radius = 100;

  // ── Cálculo dinámico de puntos del Radar SVG ──
  puntosRadar = computed(() => {
    const semana = this.semanaSeleccionada();
    const res = this.resultado;
    if (!res || !res.insight || !res.insight.dimensiones) {
      return { actual: '', ideal: '', verticesActual: [], verticesIdeal: [] };
    }

    const dimsActual = res.insight.dimensiones[semana] || {};
    // El radar ideal de LUKA (línea punteada celeste)
    const dimsIdeal = { Constancia: 90, Ahorro: 85, Control: 90, Diversidad: 80, Puntualidad: 95, Equilibrio: 85 };

    const ptsActual: { x: number; y: number; label: string; valor: number }[] = [];
    const ptsIdeal: { x: number; y: number; label: string; valor: number }[] = [];

    this.dimensionesKeys.forEach((key, index) => {
      // Ángulo de cada vértice. 0 grados apunta hacia la derecha. Restamos PI/2 para apuntar hacia arriba.
      const angle = -Math.PI / 2 + (index * 2 * Math.PI) / 6;
      
      const valActual = dimsActual[key] || 50;
      const rActual = (valActual / 100) * this.radius;
      const xActual = this.cx + rActual * Math.cos(angle);
      const yActual = this.cy + rActual * Math.sin(angle);
      ptsActual.push({ x: xActual, y: yActual, label: key, valor: valActual });

      const valIdeal = dimsIdeal[key as keyof typeof dimsIdeal];
      const rIdeal = (valIdeal / 100) * this.radius;
      const xIdeal = this.cx + rIdeal * Math.cos(angle);
      const yIdeal = this.cy + rIdeal * Math.sin(angle);
      ptsIdeal.push({ x: xIdeal, y: yIdeal, label: key, valor: valIdeal });
    });

    const actualPath = ptsActual.map(p => `${p.x},${p.y}`).join(' ');
    const idealPath = ptsIdeal.map(p => `${p.x},${p.y}`).join(' ');

    return {
      actual: actualPath,
      ideal: idealPath,
      verticesActual: ptsActual,
      verticesIdeal: ptsIdeal
    };
  });

  // Retorna líneas del fondo de la red para dibujar la "telaraña" del radar
  redHexagonal = computed(() => {
    const niveles = [20, 40, 60, 80, 100];
    const lineas: string[] = [];
    
    // Generar los polígonos concéntricos
    niveles.forEach(lvl => {
      const pts: string[] = [];
      for (let i = 0; i < 6; i++) {
        const angle = -Math.PI / 2 + (i * 2 * Math.PI) / 6;
        const r = (lvl / 100) * this.radius;
        const x = this.cx + r * Math.cos(angle);
        const y = this.cy + r * Math.sin(angle);
        pts.push(`${x},${y}`);
      }
      lineas.push(pts.join(' '));
    });

    // Generar las líneas de los ejes
    const ejes: { x1: number; y1: number; x2: number; y2: number }[] = [];
    for (let i = 0; i < 6; i++) {
      const angle = -Math.PI / 2 + (i * 2 * Math.PI) / 6;
      const x2 = this.cx + this.radius * Math.cos(angle);
      const y2 = this.cy + this.radius * Math.sin(angle);
      ejes.push({ x1: this.cx, y1: this.cy, x2, y2 });
    }

    return { poligonos: lineas, ejes };
  });

  // Celdas del heatmap organizadas en semanas
  heatmapSemanas = computed(() => {
    const res = this.resultado;
    if (!res || !res.insight || !res.insight.heatmap_datos) {
      return [];
    }
    
    const datos: CeldaHeatmap[] = res.insight.heatmap_datos;
    const semanas: CeldaHeatmap[][] = [];
    for (let i = 0; i < datos.length; i += 7) {
      semanas.push(datos.slice(i, i + 7));
    }
    return semanas;
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

    this.puntuacionHabito.set(insight.puntuacion_habito ?? 72);
    this.esSaludable.set(insight.es_saludable ?? false);
    this.diaMayorGasto.set(insight.dia_mayor_gasto ?? 'Saturday');
    this.categoriaMasFrecuente.set(insight.categoria_mas_frecuente ?? 'Restaurantes');
    this.totalTransacciones.set(insight.total_transacciones_periodo ?? 18);
    this.dimensionSeleccionada.set(null);

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
        this.typewriterTimeout = setTimeout(escribir, 10);
      }
    };

    this.typewriterTimeout = setTimeout(escribir, 800);
  }

  toggleSemana(): void {
    const actual = this.semanaSeleccionada();
    this.semanaSeleccionada.set(actual === 'ESTA_SEMANA' ? 'SEMANA_PASADA' : 'ESTA_SEMANA');
    this.dimensionSeleccionada.set(null);
  }

  seleccionarDimension(dim: string): void {
    this.dimensionSeleccionada.set(dim === this.dimensionSeleccionada() ? null : dim);
  }

  getDetalleDimension(): DimensionDetalle | null {
    const dim = this.dimensionSeleccionada();
    if (!dim || !this.resultado?.insight?.dimensiones) return null;
    
    const semana = this.semanaSeleccionada();
    const valor = this.resultado.insight.dimensiones[semana]?.[dim] || 50;

    const consejosDims: Record<string, string> = {
      Constancia: 'Registras tus gastos con frecuencia. Mantener este hábito ayuda a evitar "gastos olvidados" al final de mes.',
      Ahorro: 'Tu nivel de ahorro está debajo del ideal de LUKA. Intenta destinar el 10% de tus ingresos a tu fondo de inmediato.',
      Control: 'Flaqueas los fines de semana en compras no planificadas. Activa recordatorios antes de salir el sábado por la noche.',
      Diversidad: 'Concentras más del 60% de tus egresos variables en comida y restaurantes. Diversifica en ocio o mantén un presupuesto fijo.',
      Puntualidad: '¡Excelente! Pagas tus deudas fijos antes de las fechas límite, lo que te ahorra comisiones por mora e intereses.',
      Equilibrio: 'Tu balance financiero roza el límite. Mantén a raya los gastos superfluos para asegurar tu Laptop Gamer.'
    };

    return {
      etiqueta: this.dimensionesConfig[dim]?.label || dim,
      valor,
      consejo: consejosDims[dim] || 'Sigue monitoreando esta dimensión para equilibrar tus finanzas.'
    };
  }

  onCellMouseEnter(event: MouseEvent, celda: CeldaHeatmap): void {
    this.celdaHover.set(celda);
    
    // Obtener posición relativa para el tooltip
    const cellElement = event.currentTarget as HTMLElement;
    const rect = cellElement.getBoundingClientRect();
    const container = cellElement.closest('.heatmap-grid-container');
    if (container) {
      const containerRect = container.getBoundingClientRect();
      this.tooltipPos.set({
        x: rect.left - containerRect.left + (rect.width / 2),
        y: rect.top - containerRect.top - 10
      });
    }
  }

  onCellMouseLeave(): void {
    this.celdaHover.set(null);
  }

  getNivelColorClase(nivel: number): string {
    return `color-level-${nivel}`;
  }

  esFinDeSemana(dia: string): boolean {
    return dia === 'Sábado' || dia === 'Domingo' || dia === 'Saturday' || dia === 'Sunday';
  }
}
