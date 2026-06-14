import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { SuscripcionService } from '../../../../core/services/suscripcion.service';
import { SuscripcionCard } from '../../components/suscripcion-card/suscripcion-card';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-suscripcion-exito',
  standalone: true,
  imports: [CommonModule, SuscripcionCard],
  templateUrl: './suscripcion-exito.html',
  styleUrls: ['./suscripcion-exito.scss']
})
export class SuscripcionExito implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private suscripcionService = inject(SuscripcionService);
  private eventBus = inject(AppEventBus);

  cargando = signal(true);
  error = signal<string | null>(null);

  sessionId = signal<string | null>(null);
  plan = signal<string>('PREMIUM');
  monto = signal<number>(25.00);
  moneda = signal<string>('PEN');
  fechaVencimiento = signal<string | null>(null);

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.sessionId.set(params['session_id'] || null);
      this.sincronizarPago(0);
    });
  }

  sincronizarPago(intentos: number): void {
    this.cargando.set(true);
    this.authService.obtenerUsuarioActual().subscribe({
      next: (resp) => {
        const tienePremium = this.authService.esPremium();
        const tienePro = this.authService.esPro();

        if ((tienePremium || tienePro) || intentos >= 15) {
          this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
          this.obtenerDetallesSuscripcion();
        } else {
          setTimeout(() => {
            this.sincronizarPago(intentos + 1);
          }, 4000);
        }
      },
      error: (err) => {
        console.error('Error al sincronizar sesión:', err);
        if (intentos >= 15) {
          this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
          this.obtenerDetallesSuscripcion();
        } else {
          setTimeout(() => {
            this.sincronizarPago(intentos + 1);
          }, 4000);
        }
      }
    });
  }

  obtenerDetallesSuscripcion(): void {
    this.suscripcionService.obtenerMiSuscripcion()
      .pipe(finalize(() => this.cargando.set(false)))
      .subscribe({
        next: (suscripcion) => {
          if (suscripcion) {
            this.plan.set(suscripcion.plan);
            this.monto.set(suscripcion.monto);
            this.moneda.set(suscripcion.moneda);
            this.fechaVencimiento.set(suscripcion.fechaVencimiento);
          }
        },
        error: (err) => {
          console.error('Error al obtener detalles de la suscripción:', err);
          if (this.authService.esPremium()) {
            this.plan.set('PREMIUM');
            this.monto.set(25.00);
          } else if (this.authService.esPro()) {
            this.plan.set('PRO');
            this.monto.set(15.00);
          } else {
            this.error.set('No se pudo verificar la activación de tu plan. Tu pago está siendo procesado.');
          }
        }
      });
  }

  navegarAlDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
