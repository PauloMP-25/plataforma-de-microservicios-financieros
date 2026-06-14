import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-suscripcion-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './suscripcion-card.html',
  styleUrls: ['./suscripcion-card.scss']
})
export class SuscripcionCard {
  @Input() plan: string = 'PREMIUM';
  @Input() monto: number = 25.00;
  @Input() moneda: string = 'PEN';
  @Input() fechaVencimiento: string | null = null;
  @Input() cargando: boolean = false;

  @Output() irAlDashboard = new EventEmitter<void>();

  get iconClass(): string {
    return this.plan === 'PRO' ? 'fa-star' : 'fa-crown';
  }

  get planLabel(): string {
    return this.plan === 'PRO' ? 'Luka Pro' : 'Luka Premium';
  }

  emitirClick(): void {
    this.irAlDashboard.emit();
  }
}
