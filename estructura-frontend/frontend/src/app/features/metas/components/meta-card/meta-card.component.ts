import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetaBubbleComponent } from '../meta-bubble/meta-bubble.component';

@Component({
  selector: 'app-meta-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MetaBubbleComponent],
  styleUrl: './meta-card.component.scss',
  host: {
    'class': 'metas-page__card card',
    '[class.metas-page__card--selected]': 'isSelected()',
    '[class.metas-page__card--success]': 'meta().completada',
    '[class.metas-page__card--danger]': 'esVencida() && !meta().completada',
    '[class.metas-page__card--active]': '!meta().completada && !esVencida()',
    'style': 'will-change: transform, box-shadow;',
    '(click)': 'select.emit($event)'
  },
  template: `
    <!-- Botón de Opciones (Tres Puntos) -->
    <button type="button" class="card-options-btn" (click)="onOptionClick($event)">
      <i class="fa-solid fa-ellipsis-vertical"></i>
      <span class="tooltip">Ver detalles</span>
    </button>

    <!-- Encabezado de la Tarjeta Minimalista -->
    <div class="metas-page__card-header">
      <div class="metas-page__card-category">
        <i [class]="meta().iconoVisual" class="metas-page__card-category-icon" [ngClass]="'text-' + obtenerColorEstado()"></i>
        <h3>{{ meta().nombreVisual }}</h3>
      </div>
    </div>

    <!-- Burbuja de Progreso -->
    <div class="metas-page__card-body">
      <app-meta-bubble 
        [icono]="meta().iconoVisual" 
        [porcentaje]="meta().porcentajeProgreso" 
        [estado]="obtenerColorEstado()" 
        [color]="obtenerColorProgreso()" 
        [size]="'small'">
      </app-meta-bubble>

      <!-- Detalles Financieros Minimalistas -->
      <div class="metas-page__card-minimal-details">
        @if (mostrarCajaExito()) {
          <span class="monto-principal text-success">S/ {{ formatMoneda(meta().montoObjetivo) }}</span>
          <span class="sub-label text-secondary">{{ (meta().fechaActualizacion || meta().fechaCreacion) | date:'dd/MM/yyyy' }}</span>
        }
        @if (mostrarCajaProgreso()) {
          <span class="monto-principal">
            S/ {{ formatMoneda(meta().montoAplicado) }} 
            <span class="monto-total">/ S/ {{ formatMoneda(meta().montoObjetivo) }}</span>
          </span>
          <span class="sub-label" [ngClass]="esVencida() ? 'text-danger' : 'text-secondary'">
            {{ meta().fechaObjetivo | date:'dd/MM/yyyy' }}
          </span>
        }
      </div>
    </div>

    <!-- Footer de la Tarjeta (Acción y Estado) -->
    <div class="metas-page__card-footer" (click)="$event.stopPropagation()">
      @if (mostrarBotonCompletar()) {
        <button type="button" class="btn-action btn-action--complete" (click)="complete.emit()">
          <i class="fa-solid fa-circle-check"></i> Completar
        </button>
      }

      <!-- Badge de Estado -->
      <span class="card-badge badge"
            [class.badge-success]="meta().completada"
            [class.badge-danger]="esVencida() && !meta().completada"
            [class.badge-primary]="!meta().completada && !esVencida()">
        {{ obtenerTextoEstado() }}
      </span>
    </div>
  `
})
export class MetaCardComponent {
  meta = input.required<any>();
  disponible = input.required<number>();
  isSelected = input(false);

  select = output<Event>();
  optionsClick = output<void>();
  complete = output<void>();

  esVencida(): boolean {
    const meta = this.meta();
    if (!meta.fechaObjetivo) return false;
    const limite = new Date(meta.fechaObjetivo + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return limite < hoy;
  }

  obtenerColorEstado(): string {
    const meta = this.meta();
    if (meta.completada) return 'success';
    if (this.esVencida()) return 'danger';
    return 'active';
  }

  obtenerTextoEstado(): string {
    const meta = this.meta();
    if (meta.completada) return 'Cumplida';
    if (this.esVencida()) return 'Vencida';
    return 'Activa';
  }

  obtenerColorProgreso(): string {
    const meta = this.meta();
    if (meta.completada) return '#22c55e';
    if (this.esVencida()) return '#ef4444';
    return '#5b6af0';
  }

  formatMoneda(valor: number): string {
    if (valor === null || valor === undefined) return '0.00';
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  mostrarCajaExito(): boolean {
    return !!(this.meta() && this.meta().completada);
  }

  mostrarCajaProgreso(): boolean {
    return !!(this.meta() && !this.meta().completada);
  }

  mostrarBotonCompletar(): boolean {
    const meta = this.meta();
    return !!(meta && !meta.completada && meta.puedeCompletar);
  }

  onOptionClick(event: Event) {
    event.stopPropagation();
    this.optionsClick.emit();
  }
}
