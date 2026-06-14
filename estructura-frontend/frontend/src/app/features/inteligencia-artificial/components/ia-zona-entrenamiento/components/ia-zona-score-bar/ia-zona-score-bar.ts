import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-zona-score-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="glass-panel p-6 rounded-xl flex flex-col items-center justify-center min-h-[220px] w-full">
      <p class="text-xs text-slate-400 mb-6 uppercase tracking-widest font-bold">Fuerza Luka (Score)</p>
      
      <div class="barbell-container relative w-full flex items-center justify-center h-32">
        <!-- Barra de metal -->
        <div class="absolute w-full h-4 bg-gradient-to-b from-slate-300 via-slate-400 to-slate-500 rounded-sm z-0 shadow-lg border border-slate-600/50"></div>
        
        <!-- Discos Izquierda -->
        <div class="flex items-center gap-[2px] z-10 absolute left-4">
          <div class="plate plate-large bg-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.5)] border border-emerald-400"></div>
          <div class="plate plate-medium bg-amber-500 shadow-[0_0_10px_rgba(245,158,11,0.5)] border border-amber-400"></div>
          <div class="plate plate-small bg-slate-700 shadow-md border border-slate-500"></div>
        </div>
        
        <!-- Centro Score -->
        <div class="score-display z-20 flex flex-col items-center justify-center w-24 h-24 rounded-full border-4 border-slate-700 bg-slate-900 shadow-[0_0_25px_rgba(0,0,0,0.9)] relative">
          <div class="absolute inset-0 rounded-full border border-slate-500/30 m-1"></div>
          <span class="text-4xl font-black" [ngClass]="{
            'text-red-500 drop-shadow-[0_0_8px_rgba(239,68,68,0.8)]': score < 60,
            'text-amber-500 drop-shadow-[0_0_8px_rgba(245,158,11,0.8)]': score >= 60 && score < 80,
            'text-emerald-500 drop-shadow-[0_0_8px_rgba(16,185,129,0.8)]': score >= 80
          }">{{ score }}</span>
        </div>
        
        <!-- Discos Derecha -->
        <div class="flex items-center gap-[2px] z-10 absolute right-4 flex-row-reverse">
          <div class="plate plate-large bg-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.5)] border border-emerald-400"></div>
          <div class="plate plate-medium bg-amber-500 shadow-[0_0_10px_rgba(245,158,11,0.5)] border border-amber-400"></div>
          <div class="plate plate-small bg-slate-700 shadow-md border border-slate-500"></div>
        </div>
      </div>
    </div>
  `
})
export class IaZonaScoreBarComponent {
  @Input() score!: number;
}
