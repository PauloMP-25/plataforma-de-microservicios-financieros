import { Component, Input, Output, EventEmitter, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface DimensionDetalle {
  etiqueta: string;
  valor: number;
  consejo: string;
}

@Component({
  selector: 'app-ia-habitos-radar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-habitos-radar.html',
  styleUrl: './ia-habitos-radar.scss',
})
export class IaHabitosRadarComponent {
  // Input: dimensiones actual y de la semana pasada, o todo el objeto dimensiones.
  // Por simplicidad recibimos las dimensiones directamente.
  @Input() dimensiones: Record<string, any> = {}; 
  
  @Output() dimensionSeleccionadaChange = new EventEmitter<string | null>();

  semanaSeleccionada = signal<'ESTA_SEMANA' | 'SEMANA_PASADA'>('ESTA_SEMANA');
  dimensionSeleccionada = signal<string | null>(null);

  dimensionesKeys: string[] = ['Constancia', 'Ahorro', 'Control', 'Diversidad', 'Puntualidad', 'Equilibrio'];
  
  dimensionesConfig: Record<string, { label: string; icon: string }> = {
    Constancia: { label: 'Constancia', icon: 'fa-calendar-check' },
    Ahorro: { label: 'Ahorro', icon: 'fa-piggy-bank' },
    Control: { label: 'Control', icon: 'fa-sliders' },
    Diversidad: { label: 'Diversidad', icon: 'fa-circle-nodes' },
    Puntualidad: { label: 'Puntualidad', icon: 'fa-clock' },
    Equilibrio: { label: 'Equilibrio', icon: 'fa-scale-balanced' }
  };

  // ── Parámetros SVG Radar ──
  cx = 150;
  cy = 150;
  radius = 100;

  // ── Cálculo dinámico de puntos del Radar SVG ──
  puntosRadar = computed(() => {
    const semana = this.semanaSeleccionada();
    const dimsActual = this.dimensiones ? (this.dimensiones[semana] || {}) : {};
    
    // Fallback if dimensions are empty
    const dimsActualVal = Object.keys(dimsActual).length > 0 ? dimsActual : {
      Constancia: 50, Ahorro: 50, Control: 50, Diversidad: 50, Puntualidad: 50, Equilibrio: 50
    };

    // El radar ideal de LUKA (línea punteada celeste)
    const dimsIdeal = { Constancia: 90, Ahorro: 85, Control: 90, Diversidad: 80, Puntualidad: 95, Equilibrio: 85 };

    const ptsActual: { x: number; y: number; label: string; valor: number }[] = [];
    const ptsIdeal: { x: number; y: number; label: string; valor: number }[] = [];

    this.dimensionesKeys.forEach((key, index) => {
      const angle = -Math.PI / 2 + (index * 2 * Math.PI) / 6;
      
      const valActual = dimsActualVal[key] || 50;
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

    const ejes: { x1: number; y1: number; x2: number; y2: number }[] = [];
    for (let i = 0; i < 6; i++) {
      const angle = -Math.PI / 2 + (i * 2 * Math.PI) / 6;
      const x2 = this.cx + this.radius * Math.cos(angle);
      const y2 = this.cy + this.radius * Math.sin(angle);
      ejes.push({ x1: this.cx, y1: this.cy, x2, y2 });
    }

    return { poligonos: lineas, ejes };
  });

  toggleSemana(): void {
    const actual = this.semanaSeleccionada();
    this.semanaSeleccionada.set(actual === 'ESTA_SEMANA' ? 'SEMANA_PASADA' : 'ESTA_SEMANA');
    this.dimensionSeleccionada.set(null);
    this.dimensionSeleccionadaChange.emit(null);
  }

  seleccionarDimension(dim: string): void {
    const newVal = dim === this.dimensionSeleccionada() ? null : dim;
    this.dimensionSeleccionada.set(newVal);
    this.dimensionSeleccionadaChange.emit(newVal);
  }

  getDetalleDimension(): DimensionDetalle | null {
    const dim = this.dimensionSeleccionada();
    if (!dim || !this.dimensiones) return null;
    
    const semana = this.semanaSeleccionada();
    const valor = this.dimensiones[semana]?.[dim] || 50;

    const consejosDims: Record<string, string> = {
      Constancia: 'Registras tus gastos con frecuencia. Mantener este hábito ayuda a evitar "gastos olvidados" al final de mes.',
      Ahorro: 'Tu nivel de ahorro está debajo del ideal de LUKA. Intenta destinar el 10% de tus ingresos a tu fondo de inmediato.',
      Control: 'Flaqueas los fines de semana en compras no planificadas. Activa recordatorios antes de salir el sábado por la noche.',
      Diversidad: 'Concentras más del 60% de tus egresos variables en comida y restaurantes. Diversifica en ocio o mantén un presupuesto fijo.',
      Puntualidad: '¡Excelente! Pagas tus deudas fijos antes de las fechas límite, lo que te ahorra comisiones por mora e intereses.',
      Equilibrio: 'Tu balance financiero roza el límite. Mantén a raya los gastos superfluos para asegurar tus metas.'
    };

    return {
      etiqueta: this.dimensionesConfig[dim]?.label || dim,
      valor,
      consejo: consejosDims[dim] || 'Sigue monitoreando esta dimensión para equilibrar tus finanzas.'
    };
  }
}
