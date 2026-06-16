import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetaBubbleComponent } from '../meta-bubble/meta-bubble.component';

@Component({
  selector: 'app-meta-preview-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MetaBubbleComponent],
  styleUrl: './meta-preview-card.component.scss',
  host: {
    'class': 'meta-form-page__preview-box'
  },
  template: `
    <h4>Vista previa de tu meta</h4>
    
    <app-meta-bubble 
      [icono]="icono()" 
      [porcentaje]="porcentaje()" 
      [estado]="'active'" 
      [color]="obtenerColorProgreso()" 
      [size]="'preview'">
    </app-meta-bubble>

    <!-- Ficha Resumen Interactiva -->
    <div class="meta-form-page__summary-details">
      <div class="summary-item">
        <span class="label">Propósito</span>
        <span class="value">
          <i [class]="icono()"></i> {{ categoria() }}
        </span>
      </div>
      <div class="summary-item">
        <span class="label">Nombre</span>
        <span class="value text-truncate" [title]="nombre() || 'Sin nombre'">
          {{ nombre() || 'Sin nombre aún' }}
        </span>
      </div>
      <div class="summary-item">
        <span class="label">Meta Objetivo</span>
        <span class="value font-mono">S/ {{ formatMoneda(montoObjetivo()) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">Ahorro Inicial</span>
        <span class="value text-success font-mono">S/ {{ formatMoneda(montoActual()) }}</span>
      </div>
      <div class="summary-item">
        <span class="label">Fecha Límite</span>
        <span class="value">{{ fechaLimite() ? (fechaLimite()! | date:'dd/MM/yyyy') : 'No establecida' }}</span>
      </div>
    </div>
  `
})
export class MetaPreviewCardComponent {
  nombre = input<string>('');
  categoria = input<string>('');
  icono = input<string>('');
  montoObjetivo = input<number>(0);
  montoActual = input<number>(0);
  fechaLimite = input<string | null>(null);

  porcentaje(): number {
    const obj = this.montoObjetivo();
    const act = this.montoActual();
    return obj > 0 ? Math.min(100, (act / obj) * 100) : 0;
  }

  obtenerColorProgreso(): string {
    return this.porcentaje() >= 100 ? '#22c55e' : '#5b6af0';
  }

  formatMoneda(valor: number): string {
    if (valor === null || valor === undefined) return '0.00';
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
