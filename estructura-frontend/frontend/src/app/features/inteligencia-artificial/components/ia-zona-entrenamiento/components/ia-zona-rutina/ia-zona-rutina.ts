import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RutinaEjercicio } from '../../ia-zona-entrenamiento';

@Component({
  selector: 'app-ia-zona-rutina',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="gym-routine-card glass-panel">
      <h3 class="routine-card-title">
        <i class="fa-solid fa-clipboard-list"></i>
        Rutina Prescrita por Gemini
      </h3>
      
      <div class="exercise-list">
        <div *ngFor="let rut of rutinas" 
             class="exercise-card"
             [ngClass]="{
               'is-active': !rut.completado,
               'is-completed': rut.completado
             }">
          
          <!-- Stamp de "LOGRADO" -->
          <div *ngIf="rut.completado" class="stamp-logrado">
            LOGRADO
          </div>

          <div class="exercise-content">
            <!-- Checkbox Custom -->
            <div class="checkbox-col">
              <label class="cyber-checkbox">
                <input type="checkbox" [(ngModel)]="rut.completado">
                <span class="checkmark"></span>
              </label>
            </div>
            
            <!-- Detalles Ejercicio -->
            <div class="details-col">
              <div class="details-header">
                <h4 class="exercise-name" [class.is-done]="rut.completado">{{ rut.nombre }}</h4>
                <span class="badge-muscle">
                  <i class="fa-solid fa-bullseye"></i> {{ rut.musculoTrabajado }}
                </span>
              </div>
              <p class="exercise-desc" [class.is-done]="rut.completado">{{ rut.descripcion }}</p>
              
              <div class="exercise-meta">
                <span class="meta-item is-series" [class.is-done]="rut.completado">
                  <i class="fa-solid fa-repeat"></i> {{ rut.series }} series x {{ rut.repeticiones }} reps
                </span>
                <span class="meta-item is-goal" [class.is-done]="rut.completado">
                  <i class="fa-solid fa-flag-checkered"></i> {{ rut.metricaExito }}
                </span>
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
