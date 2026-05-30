import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-prediccion-oraculo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-prediccion-oraculo.html',
  styleUrl: './ia-prediccion-oraculo.scss'
})
export class IaPrediccionOraculoComponent {
  @Input() historialMeses: number[] = [];
  @Input() proyeccionProximoMes: number = 0;
  @Input() oraculoHablando: boolean = false;

  // ── Computes para Graficado SVG Autónomo ──
  chartPoints = computed(() => {
    const hist = this.historialMeses || [];
    const proj = this.proyeccionProximoMes || 0;
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
      const x = paddingX + (index / Math.max(1, hist.length - 1)) * graphWidth;
      const y = height - paddingY - ((val - minVal) / rango) * graphHeight;
      return { x, y, val };
    });

    // Punto proyectado (se coloca un poco más a la derecha del último histórico)
    // El gráfico original asume que todo el espacio es para el historial y el proyecto sale del cuadro?
    // Ajustaremos para que el historial ocupe el 80% del ancho y la proyeccion el 100%
    const puntosAjustados = hist.map((val, index) => {
      const x = paddingX + (index / Math.max(1, hist.length)) * graphWidth;
      const y = height - paddingY - ((val - minVal) / rango) * graphHeight;
      return { x, y, val };
    });

    const xProj = width - paddingX;
    const yProj = height - paddingY - ((proj - minVal) / rango) * graphHeight;
    const puntoProj = { x: xProj, y: yProj, val: proj };

    return { puntos: puntosAjustados, puntoProj };
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
    
    const last = pts[pts.length - 1];
    path += ` L ${last.x} ${last.y}`;
    return path;
  });

  // Trazar línea de proyección
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
    
    return `${histPath} L ${last.x} 140 L ${first.x} 140 Z`;
  });
}
