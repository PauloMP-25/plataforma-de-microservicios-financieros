import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-zona-score-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="gym-score-card glass-panel">
      <p class="score-card-title">Fuerza Luka (Score)</p>
      
      <div class="barbell-container">
        <!-- Metal Bar -->
        <div class="barbell-shaft"></div>
        
        <!-- Left Plates -->
        <div class="barbell-plates is-left">
          <div class="plate plate-large"></div>
          <div class="plate plate-medium"></div>
          <div class="plate plate-small"></div>
        </div>
        
        <!-- Center Display -->
        <div class="barbell-score-display">
          <div class="score-inner-border"></div>
          <span class="score-value" [ngClass]="{
            'is-critico': score < 60,
            'is-alerta': score >= 60 && score < 80,
            'is-excelente': score >= 80
          }">{{ score }}</span>
        </div>
        
        <!-- Right Plates -->
        <div class="barbell-plates is-right">
          <div class="plate plate-large"></div>
          <div class="plate plate-medium"></div>
          <div class="plate plate-small"></div>
        </div>
      </div>
    </div>
  `
})
export class IaZonaScoreBarComponent {
  @Input() score!: number;
}
