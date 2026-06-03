import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-status-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-status-badge.html'
})
export class AdminStatusBadgeComponent {
  @Input({ required: true }) texto!: string;
  @Input() tipo: 'ok' | 'warn' | 'error' | 'info' = 'info';

  get clases(): string {
    return {
      ok: 'bg-success/10 text-success border-success/20',
      warn: 'bg-warning/10 text-warning border-warning/20',
      error: 'bg-danger/10 text-danger border-danger/20',
      info: 'bg-info/10 text-info border-info/20'
    }[this.tipo];
  }
}
