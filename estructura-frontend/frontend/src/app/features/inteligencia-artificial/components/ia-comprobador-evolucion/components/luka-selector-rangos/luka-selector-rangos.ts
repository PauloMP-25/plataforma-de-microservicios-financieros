import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'luka-selector-rangos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="w-full flex flex-col justify-center items-center py-8">
      <div class="text-center mb-8">
        <h3 class="text-3xl font-black bg-gradient-to-r from-lime-400 to-emerald-400 bg-clip-text text-transparent uppercase tracking-wider">
          Selección de Períodos de Diagnóstico
        </h3>
        <p class="text-slate-400 text-sm mt-2">Compara dos etapas de tus finanzas para emitir tu radiografía evolutiva</p>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-8 w-full max-w-6xl px-4">
        <!-- Periodo A -->
        <div class="glass-card p-6 rounded-2xl border border-blue-500/30 bg-slate-900/40 relative overflow-hidden transition-all duration-300 hover:border-blue-500/60 shadow-[0_0_20px_rgba(59,130,246,0.05)]">
          <div class="absolute top-0 right-0 w-24 h-24 bg-blue-500/5 rounded-full blur-2xl"></div>
          <div class="flex items-center gap-3 mb-6">
            <span class="w-8 h-8 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold">A</span>
            <h4 class="text-lg font-bold text-slate-100">Período de Referencia (Base)</h4>
          </div>

          <div class="flex flex-col gap-4">
            <div>
              <label class="text-xs text-slate-400 uppercase tracking-wider block mb-2 font-semibold">Fecha de Inicio</label>
              <input type="date" [(ngModel)]="fechaA_inicio" class="w-full bg-slate-950/80 border border-slate-700 rounded-lg p-3 text-slate-100 font-mono focus:border-blue-500 outline-none">
            </div>
            <div>
              <label class="text-xs text-slate-400 uppercase tracking-wider block mb-2 font-semibold">Fecha de Cierre</label>
              <input type="date" [(ngModel)]="fechaA_fin" class="w-full bg-slate-950/80 border border-slate-700 rounded-lg p-3 text-slate-100 font-mono focus:border-blue-500 outline-none">
            </div>
          </div>
        </div>

        <!-- Periodo B -->
        <div class="glass-card p-6 rounded-2xl border border-cyan-500/30 bg-slate-900/40 relative overflow-hidden transition-all duration-300 hover:border-cyan-500/60 shadow-[0_0_20px_rgba(6,182,212,0.05)]">
          <div class="absolute top-0 right-0 w-24 h-24 bg-cyan-500/5 rounded-full blur-2xl"></div>
          <div class="flex items-center gap-3 mb-6">
            <span class="w-8 h-8 rounded-full bg-cyan-500/20 text-cyan-400 flex items-center justify-center font-bold">B</span>
            <h4 class="text-lg font-bold text-slate-100">Período de Evolución (Actual)</h4>
          </div>

          <div class="flex flex-col gap-4">
            <div>
              <label class="text-xs text-slate-400 uppercase tracking-wider block mb-2 font-semibold">Fecha de Inicio</label>
              <input type="date" [(ngModel)]="fechaB_inicio" class="w-full bg-slate-950/80 border border-slate-700 rounded-lg p-3 text-slate-100 font-mono focus:border-cyan-500 outline-none">
            </div>
            <div>
              <label class="text-xs text-slate-400 uppercase tracking-wider block mb-2 font-semibold">Fecha de Cierre</label>
              <input type="date" [(ngModel)]="fechaB_fin" class="w-full bg-slate-950/80 border border-slate-700 rounded-lg p-3 text-slate-100 font-mono focus:border-cyan-500 outline-none">
            </div>
          </div>
        </div>
      </div>

      <button (click)="confirmar()" class="mt-12 btn-glow px-8 py-4 bg-gradient-to-r from-lime-500 to-emerald-500 text-slate-950 font-black text-sm uppercase tracking-widest rounded-xl hover:shadow-[0_0_25px_rgba(132,204,22,0.5)] transition-all duration-300">
        Confirmar Diagnóstico Radiográfico
      </button>
    </div>
  `,
  styles: [`
    .glass-card {
      backdrop-filter: blur(12px);
    }
    input[type="date"]::-webkit-calendar-picker-indicator {
      filter: invert(1);
      cursor: pointer;
    }
  `]
})
export class LukaSelectorRangosComponent {
  fechaA_inicio = '2026-04-01';
  fechaA_fin = '2026-04-30';
  fechaB_inicio = '2026-05-01';
  fechaB_fin = '2026-05-30';

  @Output() onConfirm = new EventEmitter<{ rangoA_inicio: string, rangoA_fin: string, rangoB_inicio: string, rangoB_fin: string }>();

  confirmar() {
    this.onConfirm.emit({
      rangoA_inicio: this.fechaA_inicio,
      rangoA_fin: this.fechaA_fin,
      rangoB_inicio: this.fechaB_inicio,
      rangoB_fin: this.fechaB_fin
    });
  }
}
