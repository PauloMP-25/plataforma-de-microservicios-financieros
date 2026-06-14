import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface CategoriaHueso {
  id: string;
  nombre: string;
  estado: 'sano' | 'fracturado';
  gastoPeriodoA: number;
  gastoPeriodoB: number;
  desviacion: number; // Porcentaje de desviación
  descripcionLogro: string;
}

@Component({
  selector: 'luka-esqueleto-svg',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="relative w-full h-[calc(100vh-220px)] min-h-[550px] bg-slate-950/40 border border-slate-800 rounded-2xl p-6 flex flex-col items-center justify-center overflow-hidden">
      <!-- Decoración Cyberpunk -->
      <div class="absolute top-4 left-4 font-mono text-[10px] text-slate-500 uppercase tracking-widest flex items-center gap-2">
        <span class="w-1.5 h-1.5 rounded-full bg-lime-500 animate-ping"></span>
        Radiografía Ósea Financiera Activa
      </div>

      <!-- Leyenda de colores -->
      <div class="absolute top-4 right-4 flex flex-col gap-2">
        <div class="flex items-center gap-2 text-xs">
          <span class="w-3 h-3 rounded-full bg-emerald-500 shadow-[0_0_8px_#10b981]"></span>
          <span class="text-slate-400">Conquistado / Sano</span>
        </div>
        <div class="flex items-center gap-2 text-xs">
          <span class="w-3 h-3 rounded-full bg-red-500 shadow-[0_0_8px_#ef4444]"></span>
          <span class="text-slate-400">Reincidente / Fracturado</span>
        </div>
      </div>

      <!-- SVG Esqueleto -->
      <div class="relative w-full max-w-[450px] h-full flex items-center justify-center">
        <svg viewBox="0 0 400 600" class="w-full h-full text-slate-700">
          <!-- Columna Vertebral / Cráneo (Categorías Macro) -->
          <g [ngClass]="getBoneClasses('macro')" 
             (mouseenter)="hoverBone($event, 'macro')" 
             (mouseleave)="clearHover()"
             (click)="clickBone('macro')"
             class="cursor-pointer transition-all duration-300 transform origin-center">
            <!-- Cabeza -->
            <circle cx="200" cy="80" r="28" fill="none" stroke="currentColor" stroke-width="2.5"></circle>
            <path d="M190 70 A1 1 0 0 0 190 72 Z M210 70 A1 1 0 0 0 210 72 Z" fill="none" stroke="currentColor" stroke-width="2.5"></path>
            <!-- Columna -->
            <path d="M200 108 L200 320" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round"></path>
            <!-- Fractura en columna si es fracturada -->
            <path *ngIf="isFractured('macro')" d="M194 200 L206 206" stroke="white" stroke-width="2.5" class="animate-pulse"></path>
          </g>

          <!-- Esternón (Ahorro Neto) -->
          <g [ngClass]="getBoneClasses('ahorro')"
             (mouseenter)="hoverBone($event, 'ahorro')" 
             (mouseleave)="clearHover()"
             (click)="clickBone('ahorro')"
             class="cursor-pointer transition-all duration-300 transform origin-center">
            <path d="M200 135 L200 240" fill="none" stroke="currentColor" stroke-width="8" stroke-linecap="round"></path>
            <path *ngIf="isFractured('ahorro')" d="M192 180 L208 185" stroke="white" stroke-width="2.5" class="animate-pulse"></path>
          </g>

          <!-- Costillas (Categorías Medianas) -->
          <g [ngClass]="getBoneClasses('medianas')"
             (mouseenter)="hoverBone($event, 'medianas')" 
             (mouseleave)="clearHover()"
             (click)="clickBone('medianas')"
             class="cursor-pointer transition-all duration-300 transform origin-center">
            <!-- Costillas Izquierdas -->
            <path d="M200 150 C160 150, 150 170, 160 190" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"></path>
            <path d="M200 170 C150 170, 140 195, 155 220" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"></path>
            <path d="M200 190 C140 190, 130 220, 150 250" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"></path>
            <!-- Costillas Derechas -->
            <path d="M200 150 C240 150, 250 170, 240 190" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"></path>
            <path d="M200 170 C250 170, 260 195, 245 220" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"></path>
            <path d="M200 190 C260 190, 270 220, 250 250" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"></path>
            <!-- Fractura en costillas si es fracturada -->
            <path *ngIf="isFractured('medianas')" d="M142 205 L154 215" stroke="white" stroke-width="2" class="animate-pulse"></path>
          </g>

          <!-- Extremidades Superiores / Brazos (Categorías Ocasionales / Ocio) -->
          <g [ngClass]="getBoneClasses('ocio')"
             (mouseenter)="hoverBone($event, 'ocio')" 
             (mouseleave)="clearHover()"
             (click)="clickBone('ocio')"
             class="cursor-pointer transition-all duration-300 transform origin-center">
            <!-- Brazo Izquierdo -->
            <path d="M190 120 C160 110, 120 120, 100 160 C90 180, 85 220, 80 250" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round"></path>
            <!-- Brazo Derecho -->
            <path d="M210 120 C240 110, 280 120, 300 160 C310 180, 315 220, 320 250" fill="none" stroke="currentColor" stroke-width="3.5" stroke-linecap="round"></path>
            <!-- Fractura en brazo -->
            <path *ngIf="isFractured('ocio')" d="M282 135 L294 145" stroke="white" stroke-width="2.5" class="animate-pulse"></path>
          </g>

          <!-- Extremidades Inferiores / Piernas (Categorías Compras Ocasionales) -->
          <g [ngClass]="getBoneClasses('compras')"
             (mouseenter)="hoverBone($event, 'compras')" 
             (mouseleave)="clearHover()"
             (click)="clickBone('compras')"
             class="cursor-pointer transition-all duration-300 transform origin-center">
            <!-- Pelvis -->
            <path d="M170 320 L230 320 L220 340 L180 340 Z" fill="none" stroke="currentColor" stroke-width="3"></path>
            <!-- Pierna Izquierda -->
            <path d="M185 340 L160 460 L150 560" fill="none" stroke="currentColor" stroke-width="4.5" stroke-linecap="round"></path>
            <!-- Pierna Derecha -->
            <path d="M215 340 L240 460 L250 560" fill="none" stroke="currentColor" stroke-width="4.5" stroke-linecap="round"></path>
            <!-- Fractura en pierna -->
            <path *ngIf="isFractured('compras')" d="M152 500 L168 506" stroke="white" stroke-width="2.5" class="animate-pulse"></path>
          </g>
        </svg>

        <!-- Tooltip flotante absoluto -->
        <div *ngIf="hoveredData" 
             [style.left.px]="tooltipX" 
             [style.top.px]="tooltipY" 
             class="absolute pointer-events-none bg-slate-950/95 border border-slate-700 p-4 rounded-xl z-50 text-xs w-60 shadow-[0_0_15px_rgba(0,0,0,0.5)] transition-all duration-150">
          <div class="font-bold text-slate-100 flex justify-between items-center mb-2">
            <span>{{ hoveredData.nombre }}</span>
            <span [ngClass]="hoveredData.estado === 'sano' ? 'text-emerald-400' : 'text-red-500'" class="uppercase tracking-wider text-[9px] px-1.5 py-0.5 rounded bg-slate-900 font-black border border-current/20">
              {{ hoveredData.estado }}
            </span>
          </div>
          <div class="space-y-1 text-slate-300 font-mono">
            <div class="flex justify-between"><span>Período A:</span><span class="text-slate-400">S/ {{ hoveredData.gastoPeriodoA.toFixed(2) }}</span></div>
            <div class="flex justify-between"><span>Período B:</span><span class="text-slate-100 font-bold">S/ {{ hoveredData.gastoPeriodoB.toFixed(2) }}</span></div>
            <div class="flex justify-between border-t border-slate-800 pt-1 mt-1 font-bold">
              <span>Desviación:</span>
              <span [ngClass]="hoveredData.desviacion > 0 ? 'text-red-400' : 'text-emerald-400'">
                {{ hoveredData.desviacion > 0 ? '+' : '' }}{{ hoveredData.desviacion.toFixed(1) }}%
              </span>
            </div>
          </div>
          <p class="text-[10px] text-slate-400 mt-2 italic border-t border-slate-800/80 pt-2">{{ hoveredData.descripcionLogro }}</p>
        </div>
      </div>
      
      <p class="text-xs text-slate-500 font-mono mt-4">Pasa el cursor para ver detalles, haz clic en zonas fracturadas para ver la Receta</p>
    </div>
  `,
  styles: [`
    .stroke-emerald-400 {
      color: #10b981;
    }
    .stroke-red-500 {
      color: #ef4444;
    }
  `]
})
export class LukaEsqueletoSvgComponent {
  @Input() data!: Record<string, CategoriaHueso>;
  @Output() onCategoriaClick = new EventEmitter<string>();

  hoveredData: CategoriaHueso | null = null;
  tooltipX = 0;
  tooltipY = 0;

  getBoneClasses(key: string): string {
    const item = this.data?.[key];
    if (!item) return 'text-slate-700';

    if (item.estado === 'sano') {
      return 'text-emerald-500 stroke-emerald-400 hover:scale-105 filter drop-shadow-[0_0_6px_rgba(16,185,129,0.3)]';
    } else {
      return 'text-red-500 stroke-red-500 hover:scale-105 filter drop-shadow-[0_0_8px_rgba(239,68,68,0.4)]';
    }
  }

  isFractured(key: string): boolean {
    return this.data?.[key]?.estado === 'fracturado';
  }

  hoverBone(event: MouseEvent, key: string) {
    const item = this.data?.[key];
    if (item) {
      this.hoveredData = item;
      
      // Obtener coordenadas relativas al contenedor
      const container = (event.currentTarget as HTMLElement).closest('.relative');
      if (container) {
        const rect = container.getBoundingClientRect();
        this.tooltipX = event.clientX - rect.left + 15;
        this.tooltipY = event.clientY - rect.top - 60;
      }
    }
  }

  clearHover() {
    this.hoveredData = null;
  }

  clickBone(key: string) {
    const item = this.data?.[key];
    if (item && item.estado === 'fracturado') {
      this.onCategoriaClick.emit(item.id);
    }
  }
}
