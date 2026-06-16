import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-recommended-savings',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styleUrl: './meta-recommended-savings.component.scss',
  host: {
    'class': 'meta-form-page__recommended-savings anim-fade-up'
  },
  template: `
    <span class="recommend-title">AHORRO MENSUAL RECOMENDADO</span>
    <span class="recommend-value">S/ {{ formatMoneda(cuota()) }}</span>
    <span class="recommend-desc">Durante {{ meses() }} meses</span>
  `
})
export class MetaRecommendedSavingsComponent {
  cuota = input.required<number>();
  meses = input.required<number>();

  formatMoneda(valor: number): string {
    if (valor === null || valor === undefined) return '0.00';
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
