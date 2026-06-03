import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminKpiCard } from '../../models/admin-dashboard.model';

@Component({
  selector: 'app-admin-kpi-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-kpi-card.html'
})
export class AdminKpiCardComponent {
  @Input({ required: true }) kpi!: AdminKpiCard;

  get tonoClases(): string {
    const mapa = {
      primary: 'text-primary bg-primary/10 border-primary/20',
      info: 'text-info bg-info/10 border-info/20',
      success: 'text-success bg-success/10 border-success/20',
      warning: 'text-warning bg-warning/10 border-warning/20',
      danger: 'text-danger bg-danger/10 border-danger/20',
      purple: 'text-purple-500 bg-purple-500/10 border-purple-500/20'
    } as const;
    return mapa[this.kpi.tono];
  }

  get tendenciaClases(): string {
    if (this.kpi.tendenciaTipo === 'up') return 'text-success';
    if (this.kpi.tendenciaTipo === 'down') return 'text-danger';
    return 'text-text-muted';
  }
}
