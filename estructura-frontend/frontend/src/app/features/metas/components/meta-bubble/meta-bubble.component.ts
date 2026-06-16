import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-bubble',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  host: {
    'class': 'bubble-container',
    '[class.bubble-container--large]': 'size() === "large"',
    '[class.bubble-container--preview]': 'size() === "preview"',
    '[class.bubble-container--success]': 'estado() === "success"',
    '[class.bubble-container--danger]': 'estado() === "danger"',
    '[class.bubble-container--active]': 'estado() === "active"',
    '[style.background]': '"conic-gradient(" + color() + " " + porcentaje() + "%, var(--border-color) 0)"',
    'style': 'will-change: transform;'
  },
  template: `
    <div class="bubble">
      <div class="bubble__water" [style.transform]="'translateY(' + (porcentaje() >= 100 ? 15 : (100 - porcentaje())) + '%)'" style="will-change: transform;">
        <div class="bubble__wave bubble__wave--front"></div>
        <div class="bubble__wave bubble__wave--back"></div>
      </div>
      <div class="bubble__content">
        <i [class]="icono()" class="bubble__category-icon"></i>
        <span class="bubble__number">{{ porcentaje().toFixed(0) }}%</span>
      </div>
    </div>
  `
})
export class MetaBubbleComponent {
  icono = input.required<string>();
  porcentaje = input.required<number>();
  estado = input.required<string>();
  color = input.required<string>();
  size = input<'small' | 'large' | 'preview'>('small');
}
