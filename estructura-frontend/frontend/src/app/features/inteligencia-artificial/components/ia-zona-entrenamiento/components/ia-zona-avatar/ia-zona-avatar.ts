import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EstadoAtleta } from '../../ia-zona-entrenamiento';

@Component({
  selector: 'app-ia-zona-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="gym-avatar-card glass-panel">
      <!-- Status Top Bar -->
      <div class="status-top-bar" [ngClass]="{
        'is-critico': estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera',
        'is-excelente': estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma',
        'is-alerta': estado.perfil === 'Sedentario'
      }"></div>
      
      <div class="avatar-circle-wrapper">
        <!-- Dashed Ring -->
        <div class="dashed-ring"
             [ngClass]="{
               'is-critico': estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera',
               'is-excelente': estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma',
               'is-alerta': estado.perfil === 'Sedentario'
             }"></div>
        
        <!-- Icon Container -->
        <div class="avatar-icon-box"
             [ngClass]="{
               'is-critico': estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera',
               'is-excelente': estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma',
               'is-alerta': estado.perfil === 'Sedentario'
             }">
          <i class="fa-solid fa-user-injured" *ngIf="estado.perfil === 'Lesionado' || estado.perfil === 'UCI Financiera'"></i>
          <i class="fa-solid fa-person-running" *ngIf="estado.perfil === 'Atleta de Élite' || estado.perfil === 'En Forma'"></i>
          <i class="fa-solid fa-couch" *ngIf="estado.perfil === 'Sedentario'"></i>
        </div>
      </div>
      
      <h3 class="athlete-name">{{ estado.perfil }}</h3>
      <p class="athlete-subtitle">Estado Físico Financiero</p>
    </div>
  `
})
export class IaZonaAvatarComponent {
  @Input() estado!: EstadoAtleta;
}
