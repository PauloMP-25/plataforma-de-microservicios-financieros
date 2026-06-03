import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminDashboardService } from '../../services/admin-dashboard.service';
import { AdminDashboardData, AdminPagoReciente, AdminServicioEstado } from '../../models/admin-dashboard.model';
import { AdminKpiCardComponent } from '../../components/admin-kpi-card/admin-kpi-card';
import { AdminStatusBadgeComponent } from '../../components/admin-status-badge/admin-status-badge';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, AdminKpiCardComponent, AdminStatusBadgeComponent],
  templateUrl: './admin-dashboard.html'
})
export class AdminDashboard implements OnInit {
  data = signal<AdminDashboardData | null>(null);
  cargando = signal(true);

  constructor(
    private adminDashboardService: AdminDashboardService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.adminDashboardService.obtenerResumen().subscribe((resumen) => {
      this.data.set(resumen);
      this.cargando.set(false);
    });
  }

  estadoServicioClases(servicio: AdminServicioEstado): string {
    if (servicio.estado === 'healthy') return 'bg-success shadow-[0_0_12px_rgba(34,197,94,0.55)]';
    if (servicio.estado === 'warning') return 'bg-warning shadow-[0_0_12px_rgba(245,158,11,0.55)]';
    return 'bg-danger shadow-[0_0_12px_rgba(239,68,68,0.55)] animate-pulse';
  }

  estadoServicioTexto(servicio: AdminServicioEstado): string {
    return servicio.estado === 'healthy' ? 'OK' : servicio.estado === 'warning' ? 'WARN' : 'DOWN';
  }

  tipoPago(pago: AdminPagoReciente): 'ok' | 'warn' | 'error' {
    if (pago.estado === 'EXITOSO') return 'ok';
    if (pago.estado === 'PENDIENTE') return 'warn';
    return 'error';
  }

  alturaBarra(valor: number): string {
    return `${Math.max(12, valor)}%`;
  }

  salir(): void {
    this.auth.logout();
  }
}
