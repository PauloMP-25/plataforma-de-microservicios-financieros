import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { SuscripcionDTO } from '../../../../core/models/financiero/suscripcion-gasto.model';

import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-suscripcion-exito',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './suscripcion-exito.html',
  styleUrl: './suscripcion-exito.scss'
})
export class SuscripcionExito implements OnInit {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  // Señales de datos
  readonly suscripcion = signal<SuscripcionDTO | null>(null);
  
  // Señales agregadas para resolver los errores de "Object is possibly 'undefined'"
  readonly plan = signal<string>('');
  readonly monto = signal<number>(0);
  readonly error = signal<string>('');

  ngOnInit(): void {
    this.authService.obtenerUsuarioActual().subscribe({
      next: () => {
        if (this.authService.esPremium()) {
          this.plan.set('PREMIUM');
          this.monto.set(25.00);
        } else if (this.authService.esPro()) {
          this.plan.set('PRO');
          this.monto.set(15.00);
        } else {
          this.error.set('No se pudo verificar la activación de tu plan. Tu pago está siendo procesado.');
        }
      },
      error: (err) => {
        console.error('Error al actualizar el rol del usuario:', err);
        this.error.set('Error al conectar con el servidor para verificar tu plan.');
      }
    });
  }

  /**
   * Volver a la lista de suscripciones
   */
  volverASuscripciones(): void {
    this.router.navigate(['/suscripcion/pagos']);
  }

  /**
   * Volver al dashboard de Luka
   */
  volverAlDashboard(): void {
    this.router.navigate(['/suscripcion/luka']);
  }

  /**
   * Navegar al dashboard principal
   */
  navegarAlDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  /**
   * Formatear moneda
   */
  formatearMoneda(valor: number): string {
    return new Intl.NumberFormat('en-US', { 
      style: 'currency', 
      currency: 'USD' 
    }).format(valor);
  }
}