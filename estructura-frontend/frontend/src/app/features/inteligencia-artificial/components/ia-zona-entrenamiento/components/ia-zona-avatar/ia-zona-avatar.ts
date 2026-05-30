import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EstadoAtleta } from '../../ia-zona-entrenamiento';

@Component({
  selector: 'app-ia-zona-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="glass-panel p-6 rounded-xl flex flex-col items-center justify-center text-center relative overflow-hidden h-full w-full">
      <div class="absolute top-0 w-full h-1" [ngClass]="{
        'bg-red-500 shadow-[0_0_10px_rgba(239,68,68,0.8)]': estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera',
        'bg-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.8)]': estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma',
        'bg-amber-500 shadow-[0_0_10px_rgba(245,158,11,0.8)]': estado.perfil === 'Sedentario'
      }"></div>
      
      <div class="avatar-container relative w-32 h-32 mb-4">
        <div class="absolute inset-0 rounded-full border-4 border-dashed animate-[spin_10s_linear_infinite]"
             [ngClass]="{
               'border-red-500/50': estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera',
               'border-emerald-500/50': estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma',
               'border-amber-500/50': estado.perfil === 'Sedentario'
             }"></div>
        
        <div class="w-full h-full rounded-full flex items-center justify-center bg-slate-900 border-2 shadow-inner"
             [ngClass]="{
               'border-red-500 text-red-500': estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera',
               'border-emerald-500 text-emerald-500': estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma',
               'border-amber-500 text-amber-500': estado.perfil === 'Sedentario'
             }">
          <i class="fa-solid fa-user-injured text-5xl" *ngIf="estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera'"></i>
          <i class="fa-solid fa-person-running text-5xl" *ngIf="estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma'"></i>
          <i class="fa-solid fa-couch text-5xl" *ngIf="estado.perfil === 'Sedentario'"></i>
        </div>
      </div>
      
      <h3 class="text-2xl font-black uppercase tracking-widest text-slate-100">{{ estado.perfil }}</h3>
      <p class="text-sm text-slate-400 mt-2 font-medium">Estado Físico Financiero</p>
    </div>
  `
})
export class IaZonaAvatarComponent {
  @Input() estado!: EstadoAtleta;
}
