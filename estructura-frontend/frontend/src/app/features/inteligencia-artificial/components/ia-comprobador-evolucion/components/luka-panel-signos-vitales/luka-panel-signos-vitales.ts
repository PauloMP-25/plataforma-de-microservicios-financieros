import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface KPIEvolucion {
  deltaAhorro: { valor: number, variacionRelativa: number };
  ivg: { valor: number, clasificacion: 'Controlado' | 'Moderado' | 'Caótico' | 'Crítico' };
  conquistas: string[];
  alertas: string[];
}

@Component({
  selector: 'luka-panel-signos-vitales',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col gap-4 h-full w-full">
      <!-- 1. Delta de Ahorro -->
      <div class="glass-panel p-6 rounded-2xl relative overflow-hidden flex-1 min-h-[140px] flex flex-col justify-between border border-slate-800">
        <!-- SVG Sparkline Background -->
        <div class="absolute inset-0 z-0 opacity-20">
          <svg class="w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 100">
            <path d="M 0,90 Q 20,40 40,80 T 80,20 T 100,50 L 100,100 L 0,100 Z" 
                  [attr.fill]="kpis.deltaAhorro.valor >= 0 ? '#10b981' : '#ef4444'"></path>
          </svg>
        </div>

        <div class="relative z-10">
          <span class="text-[10px] uppercase text-slate-400 font-bold tracking-widest block mb-1">Delta Tasa de Ahorro</span>
          <div class="flex items-baseline gap-2">
            <h4 class="text-4xl font-black font-mono tracking-tight" 
                [ngClass]="kpis.deltaAhorro.valor >= 0 ? 'text-emerald-400' : 'text-red-500'">
              {{ kpis.deltaAhorro.valor >= 0 ? '+' : '' }}S/ {{ kpis.deltaAhorro.valor.toFixed(2) }}
            </h4>
            <span class="text-xs font-bold font-mono" [ngClass]="kpis.deltaAhorro.variacionRelativa >= 0 ? 'text-emerald-500' : 'text-red-400'">
              ({{ kpis.deltaAhorro.variacionRelativa >= 0 ? '+' : '' }}{{ kpis.deltaAhorro.variacionRelativa.toFixed(1) }}%)
            </span>
          </div>
        </div>
        <p class="text-[10px] text-slate-400 relative z-10 font-medium">Variación neta en la capacidad de reserva acumulada.</p>
      </div>

      <!-- 2. Índice de Volatilidad (IVG) -->
      <div class="glass-panel p-6 rounded-2xl relative overflow-hidden flex-1 min-h-[140px] flex flex-col justify-between border border-slate-800">
        <div class="flex justify-between items-start z-10">
          <div>
            <span class="text-[10px] uppercase text-slate-400 font-bold tracking-widest block mb-1">Índice Volatilidad Gastos (IVG)</span>
            <h4 class="text-2xl font-black font-mono text-slate-100">{{ kpis.ivg.valor }}%</h4>
          </div>
          <span class="text-[10px] font-black uppercase px-2 py-0.5 rounded border tracking-wider"
                [ngClass]="{
                  'border-emerald-500/30 text-emerald-400 bg-emerald-950/20': kpis.ivg.clasificacion === 'Controlado',
                  'border-amber-500/30 text-amber-400 bg-amber-950/20': kpis.ivg.clasificacion === 'Moderado',
                  'border-red-500/30 text-red-400 bg-red-950/20': kpis.ivg.clasificacion === 'Caótico' || kpis.ivg.clasificacion === 'Crítico'
                }">
            {{ kpis.ivg.clasificacion }}
          </span>
        </div>

        <!-- SVG ECG dinámico -->
        <div class="h-16 w-full opacity-65 mt-2 relative z-0">
          <svg class="w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 30">
            <!-- ECG controlado (suave) -->
            <polyline *ngIf="kpis.ivg.clasificacion === 'Controlado' || kpis.ivg.clasificacion === 'Moderado'"
                      points="0,15 15,15 20,5 25,25 30,15 45,15 50,5 55,25 60,15 100,15" 
                      fill="none" stroke="#10b981" stroke-width="1.8" class="ecg-wave-smooth"></polyline>
            <!-- ECG caótico/crítico (irregular) -->
            <polyline *ngIf="kpis.ivg.clasificacion === 'Caótico' || kpis.ivg.clasificacion === 'Crítico'"
                      points="0,15 10,15 13,2 16,28 19,10 22,25 25,15 40,15 43,0 46,30 49,8 52,22 55,15 100,15" 
                      fill="none" stroke="#ef4444" stroke-width="1.8" class="ecg-wave-chaotic animate-pulse"></polyline>
          </svg>
        </div>
      </div>

      <!-- 3. Badges de Conquistas y Alertas -->
      <div class="glass-panel p-6 rounded-2xl flex-grow flex flex-col gap-4 border border-slate-800">
        <div>
          <h5 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
            <i class="fa-solid fa-medal text-lime-400"></i> Categorías Conquistadas
          </h5>
          <div class="flex flex-wrap gap-2">
            <span *ngFor="let c of kpis.conquistas" class="text-[10px] font-bold bg-slate-900 border border-emerald-500/20 text-emerald-400 px-2 py-1 rounded-lg">
              {{ c }}
            </span>
            <span *ngIf="kpis.conquistas.length === 0" class="text-xs text-slate-500">Ninguna conquista detectada en el periodo.</span>
          </div>
        </div>

        <div class="border-t border-slate-800/80 pt-3">
          <h5 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
            <i class="fa-solid fa-triangle-exclamation text-red-400 animate-pulse"></i> Alertas Críticas
          </h5>
          <div class="flex flex-wrap gap-2">
            <span *ngFor="let a of kpis.alertas" class="text-[10px] font-bold bg-slate-900 border border-red-500/20 text-red-400 px-2 py-1 rounded-lg">
              {{ a }}
            </span>
            <span *ngIf="kpis.alertas.length === 0" class="text-xs text-slate-500">Sin alertas. ¡Comportamiento excelente!</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .glass-panel {
      background: rgba(15, 23, 42, 0.6);
      backdrop-filter: blur(12px);
    }
    
    .ecg-wave-smooth {
      stroke-dasharray: 200;
      stroke-dashoffset: 200;
      animation: draw-ecg 4s linear infinite;
    }

    .ecg-wave-chaotic {
      stroke-dasharray: 200;
      stroke-dashoffset: 200;
      animation: draw-ecg 2s linear infinite;
    }

    @keyframes draw-ecg {
      to {
        stroke-dashoffset: 0;
      }
    }
  `]
})
export class LukaPanelSignosVitalesComponent {
  @Input() kpis!: KPIEvolucion;
}
