import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'luka-veredicto-final',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="col-span-full bg-slate-900/60 border-t border-slate-800 p-6 flex flex-col md:flex-row items-center gap-6 rounded-2xl w-full">
      <!-- Icono Corazón Dinámico -->
      <div class="flex-none">
        <div class="w-16 h-16 rounded-full border border-slate-700 flex items-center justify-center bg-slate-950 shadow-inner relative">
          <i class="fa-solid fa-heart-pulse text-3xl" [ngClass]="getHeartAnimationClass()"></i>
          <span class="absolute -top-1 -right-1 text-[8px] font-black font-mono px-1 rounded bg-slate-900 border border-slate-800 text-slate-400">
            IMF: {{ imf }}
          </span>
        </div>
      </div>

      <!-- Texto Typewriter / Narrativa -->
      <div class="flex-grow text-center md:text-left">
        <h4 class="text-sm font-bold uppercase tracking-widest text-slate-400 font-mono flex items-center justify-center md:justify-start gap-2">
          Veredicto del Diagnóstico Evolutivo
          <span class="w-2 h-2 rounded-full" [ngClass]="getClasificacionColor()"></span>
        </h4>
        <h3 class="text-lg font-black text-slate-100 mt-1" style="font-family: 'Outfit', sans-serif;">
          {{ tituloVeredicto }}
        </h3>
        <p class="text-xs text-slate-400 mt-2 font-mono leading-relaxed typewriter-text">
          {{ textoTypewriter }}
        </p>
      </div>

      <!-- CTA Dinámico -->
      <div class="flex-none w-full md:w-auto">
        <button [ngClass]="getButtonClass()" class="w-full md:w-auto px-6 py-3 font-bold text-xs uppercase tracking-wider rounded-xl transition-all duration-300">
          {{ getButtonLabel() }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .heart-stable {
      color: #10b981;
      animation: pulse-stable 1.2s infinite;
    }
    .heart-arrhythmia {
      color: #fbbf24;
      animation: pulse-arrhythmia 0.8s infinite;
    }
    .heart-danger {
      color: #ef4444;
      animation: pulse-danger 0.4s infinite;
    }

    @keyframes pulse-stable {
      0% { transform: scale(1); }
      15% { transform: scale(1.18); }
      30% { transform: scale(1); }
      45% { transform: scale(1.1); }
      60% { transform: scale(1); }
      100% { transform: scale(1); }
    }

    @keyframes pulse-arrhythmia {
      0% { transform: scale(1); }
      10% { transform: scale(1.2); }
      20% { transform: scale(0.9); }
      35% { transform: scale(1.15); }
      50% { transform: scale(1); }
      100% { transform: scale(1); }
    }

    @keyframes pulse-danger {
      0% { transform: scale(1); }
      25% { transform: scale(1.3); }
      50% { transform: scale(1); }
      75% { transform: scale(1.3); }
      100% { transform: scale(1); }
    }
  `]
})
export class LukaVeredictoFinalComponent implements OnInit, OnChanges {
  @Input() imf!: number;
  @Input() narrativa!: string;

  tituloVeredicto = '';
  textoTypewriter = '';

  ngOnInit() {
    this.iniciarDiagnostico();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['imf'] || changes['narrativa']) {
      this.iniciarDiagnostico();
    }
  }

  iniciarDiagnostico() {
    if (this.imf >= 80) {
      this.tituloVeredicto = 'ÍNDICE DE MADUREZ FINANCIERA EXCELENTE';
    } else if (this.imf >= 60) {
      this.tituloVeredicto = 'ÍNDICE DE MADUREZ FINANCIERA ESTABLE';
    } else {
      this.tituloVeredicto = 'ÍNDICE DE MADUREZ FINANCIERA EN SITUACIÓN CRÍTICA';
    }

    // Efecto de simulación de typewriter rápido para la narrativa
    this.textoTypewriter = '';
    let i = 0;
    const txt = this.narrativa || 'Diagnóstico no disponible.';
    const speed = 10;
    const interval = setInterval(() => {
      if (i < txt.length) {
        this.textoTypewriter += txt.charAt(i);
        i++;
      } else {
        clearInterval(interval);
      }
    }, speed);
  }

  getHeartAnimationClass(): string {
    if (this.imf >= 80) return 'heart-stable';
    if (this.imf >= 60) return 'heart-arrhythmia';
    return 'heart-danger';
  }

  getClasificacionColor(): string {
    if (this.imf >= 80) return 'bg-emerald-500 shadow-[0_0_8px_#10b981]';
    if (this.imf >= 60) return 'bg-amber-500 shadow-[0_0_8px_#f59e0b]';
    return 'bg-red-500 shadow-[0_0_8px_#ef4444]';
  }

  getButtonClass(): string {
    if (this.imf >= 80) {
      return 'bg-emerald-500 text-slate-950 hover:bg-emerald-400 hover:shadow-[0_0_20px_rgba(16,185,129,0.4)]';
    }
    if (this.imf >= 60) {
      return 'bg-amber-500 text-slate-950 hover:bg-amber-400 hover:shadow-[0_0_20px_rgba(245,158,11,0.4)]';
    }
    return 'bg-red-500 text-slate-100 hover:bg-red-400 hover:shadow-[0_0_20px_rgba(239,68,68,0.4)] animate-pulse';
  }

  getButtonLabel(): string {
    if (this.imf >= 80) return 'Invertir Excedentes';
    if (this.imf >= 60) return 'Ajustar Presupuestos';
    return 'UCI Financiera: Emergencia';
  }
}
