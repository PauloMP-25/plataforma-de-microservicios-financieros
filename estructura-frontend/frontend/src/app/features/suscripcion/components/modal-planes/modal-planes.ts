import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal-planes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './modal-planes.html',
  styleUrl: './modal-planes.scss'
})
export class ModalPlanes {
  @Input() comprando: boolean = false;
  @Output() close = new EventEmitter<void>();
  @Output() checkout = new EventEmitter<{ plan: 'PRO' | 'PREMIUM'; proveedor: 'STRIPE' | 'MERCADOPAGO' }>();

  cerrar(): void {
    if (!this.comprando) {
      this.close.emit();
    }
  }

  iniciarPago(plan: 'PRO' | 'PREMIUM', proveedor: 'STRIPE' | 'MERCADOPAGO'): void {
    if (!this.comprando) {
      this.checkout.emit({ plan, proveedor });
    }
  }
}
