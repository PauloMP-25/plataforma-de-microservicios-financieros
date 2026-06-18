import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-confirm-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styleUrl: './meta-confirm-modal.component.scss',
  template: `
    <div class="metas-page__modal-backdrop" (click)="cancel.emit()">
      <div class="metas-page__confirm-modal card" (click)="$event.stopPropagation()">
        
        <header class="metas-page__confirm-header">
          <i class="fa-solid fa-circle-question icon-question"></i>
          <h3>¿Deseas completar esta meta?</h3>
        </header>

        <div class="metas-page__confirm-body">
          <p class="desc-warning">
            El monto objetivo será descontado de tu ahorro disponible y se registrará como un gasto en tu historial financiero.
          </p>

          <div class="metas-page__confirm-data">
            <div class="data-row">
              <span class="label">Meta:</span>
              <strong class="value text-primary">{{ meta().nombreVisual }}</strong>
            </div>
            <div class="data-row">
              <span class="label">Monto objetivo:</span>
              <strong class="value">S/ {{ formatMoneda(meta().montoObjetivo) }}</strong>
            </div>
            <div class="data-row">
              <span class="label">Saldo disponible actual:</span>
              <strong class="value text-success">S/ {{ formatMoneda(disponible()) }}</strong>
            </div>
            
            <div class="divider"></div>
            
            <div class="data-row">
              <span class="label">Saldo disponible restante:</span>
              <strong class="value" [ngClass]="saldoNegativoAlCompletar() ? 'text-danger' : 'text-success'">
                S/ {{ formatMoneda(disponible() - meta().montoObjetivo) }}
              </strong>
            </div>
          </div>
          
          @if (saldoNegativoAlCompletar()) {
            <small class="error-msg py-2 block"><i class="fa-solid fa-triangle-exclamation"></i> Advertencia: Tu saldo disponible quedará en negativo.</small>
          }
        </div>

        <footer class="metas-page__confirm-footer">
          <button type="button" class="btn-cancel" (click)="cancel.emit()">Cancelar</button>
          <button type="button" class="btn-confirm" [disabled]="cargando()" (click)="confirm.emit()">
            @if (cargando()) {
              <i class="fa-solid fa-spinner fa-spin"></i> Procesando...
            }
            @if (!cargando()) {
              Confirmar y completar
            }
          </button>
        </footer>

      </div>
    </div>
  `
})
export class MetaConfirmModalComponent {
  meta = input.required<any>();
  disponible = input.required<number>();
  cargando = input(false);

  confirm = output<void>();
  cancel = output<void>();

  formatMoneda(valor: number): string {
    if (valor === null || valor === undefined) return '0.00';
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  saldoNegativoAlCompletar(): boolean {
    const meta = this.meta();
    return !!(meta && meta.montoObjetivo > this.disponible());
  }
}
