import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, signal } from '@angular/core';
import { SuscripcionService } from '../../../core/services/suscripcion.service';

@Component({
  selector: 'app-planes-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './planes-modal.html',
  styleUrl: './planes-modal.scss'
})
export class PlanesModalComponent {
  @Output() cerrar = new EventEmitter<void>();

  comprandoPlan = signal(false);

  constructor(private suscripcionService: SuscripcionService) {}

  cerrarModal(): void {
    this.cerrar.emit();
  }

  comprarPlan(plan: 'PRO' | 'PREMIUM'): void {
    if (this.comprandoPlan()) return;
    this.comprandoPlan.set(true);

    this.suscripcionService.crearSesionCheckout(plan).subscribe({
      next: (sesion) => {
        this.comprandoPlan.set(false);
        if (sesion?.urlCheckout) {
          window.location.href = sesion.urlCheckout;
        }
      },
      error: () => {
        this.comprandoPlan.set(false);
      }
    });
  }
}
