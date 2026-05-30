import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RutinaEjercicio } from '../../ia-zona-entrenamiento';

@Component({
  selector: 'app-ia-zona-rutina',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="glass-panel p-6 rounded-xl flex-grow flex flex-col h-full w-full">
      <h3 class="text-lg font-bold text-slate-200 mb-4 flex items-center gap-2">
        <i class="fa-solid fa-clipboard-list text-cyan-500"></i>
        Rutina Prescrita por Gemini
      </h3>
      
      <div class="flex flex-col gap-4">
        <div *ngFor="let rut of rutinas" 
             class="exercise-card relative p-4 rounded-lg border transition-all duration-500 overflow-hidden"
             [ngClass]="{
               'border-slate-700 bg-slate-900/50 hover:border-cyan-500/50': !rut.completado,
               'border-emerald-500/30 bg-emerald-950/40 opacity-80': rut.completado
             }">
          
          <!-- Stamp de "LOGRADO" -->
          <div *ngIf="rut.completado" class="stamp-logrado absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-0 pointer-events-none">
            LOGRADO
          </div>

          <div class="flex items-start gap-4 relative z-10">
            <!-- Checkbox Custom -->
            <div class="pt-1">
              <label class="cyber-checkbox">
                <input type="checkbox" [(ngModel)]="rut.completado">
                <span class="checkmark"></span>
              </label>
            </div>
            
            <!-- Detalles Ejercicio -->
            <div class="flex-grow">
              <div class="flex justify-between items-start">
                <h4 class="font-bold text-lg transition-all" [ngClass]="rut.completado ? 'text-slate-400 line-through' : 'text-slate-200'">{{ rut.nombre }}</h4>
                <span class="badge-muscle bg-slate-800 text-cyan-400 text-xs px-2 py-1 rounded border border-cyan-500/20">
                  <i class="fa-solid fa-bullseye"></i> {{ rut.musculoTrabajado }}
                </span>
              </div>
              <p class="text-sm mt-1 transition-all" [ngClass]="rut.completado ? 'text-slate-500' : 'text-slate-400'">{{ rut.descripcion }}</p>
              
              <div class="flex items-center gap-4 mt-3 text-xs">
                <div class="flex items-center gap-1" [ngClass]="rut.completado ? 'text-slate-500' : 'text-amber-500/80'">
                  <i class="fa-solid fa-repeat"></i> {{ rut.series }} series x {{ rut.repeticiones }} reps
                </div>
                <div class="flex items-center gap-1" [ngClass]="rut.completado ? 'text-slate-500' : 'text-emerald-400/80'">
                  <i class="fa-solid fa-flag-checkered"></i> {{ rut.metricaExito }}
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  `
})
export class IaZonaRutinaComponent {
  @Input() rutinas!: RutinaEjercicio[];
}
