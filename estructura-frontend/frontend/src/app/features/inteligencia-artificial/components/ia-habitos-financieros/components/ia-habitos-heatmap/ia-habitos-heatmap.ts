import { Component, Input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface CeldaHeatmap {
  fecha: string;
  diaSemana: string;
  diaNum: number;
  monto: number;
  nivel: number;
  transacciones: { descripcion: string; monto: number; categoria: string; icono: string }[];
}

@Component({
  selector: 'app-ia-habitos-heatmap',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-habitos-heatmap.html',
  styleUrl: './ia-habitos-heatmap.scss',
})
export class IaHabitosHeatmapComponent {
  @Input() datosRaw: CeldaHeatmap[] = [];

  celdaHover = signal<CeldaHeatmap | null>(null);
  tooltipPos = signal<{ x: number; y: number }>({ x: 0, y: 0 });

  heatmapSemanas = computed(() => {
    let datos = this.datosRaw;
    
    // Fallback if data is empty so the UI doesn't look broken
    if (!datos || datos.length === 0) {
      datos = this.generarDatosMock();
    }

    const semanas: CeldaHeatmap[][] = [];
    for (let i = 0; i < datos.length; i += 7) {
      semanas.push(datos.slice(i, i + 7));
    }
    return semanas;
  });

  onCellMouseEnter(event: MouseEvent, celda: CeldaHeatmap): void {
    this.celdaHover.set(celda);
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
    const d = dia.toLowerCase();
    return d === 'sábado' || d === 'domingo' || d === 'saturday' || d === 'sunday';
  }

  private generarDatosMock(): CeldaHeatmap[] {
    const list: CeldaHeatmap[] = [];
    const diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

    for (let i = 0; i < 28; i++) {
      const w = Math.floor(i / 7) + 1;
      const dIdx = i % 7;
      const diaSemana = diasSemana[dIdx];
      const diaNum = i + 1;

      let monto = 0;
      let nivel = 1;
      let transacciones: any[] = [];

      if (diaSemana === 'Sábado') {
        monto = w === 4 ? 185.00 : (w === 3 ? 150.00 : (w === 2 ? 120.00 : 95.00));
        nivel = 5;
        transacciones = [
          { descripcion: 'Cena', monto: monto * 0.6, categoria: 'Restaurantes', icono: 'fa-utensils' },
          { descripcion: 'Ocio', monto: monto * 0.4, categoria: 'Ocio', icono: 'fa-glass-water' }
        ];
      } else if (diaSemana === 'Viernes') {
        monto = 65.00;
        nivel = 4;
        transacciones = [
          { descripcion: 'Cine', monto: 45.00, categoria: 'Ocio', icono: 'fa-film' }
        ];
      } else {
        monto = Math.random() > 0.7 ? 15.00 : 0;
        nivel = monto > 0 ? 2 : 1;
        if (monto > 0) {
          transacciones = [{ descripcion: 'Café', monto: monto, categoria: 'Alimentación', icono: 'fa-mug-hot' }];
        }
      }

      list.push({
        fecha: `2026-05-${diaNum.toString().padStart(2, '0')}`,
        diaSemana,
        diaNum,
        monto,
        nivel,
        transacciones
      });
    }
    return list;
  }
}
