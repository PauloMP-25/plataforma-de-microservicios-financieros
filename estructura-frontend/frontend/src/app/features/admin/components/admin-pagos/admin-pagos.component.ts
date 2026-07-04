import { Component, OnInit, ChangeDetectionStrategy, input, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminDashboardService } from '../../services/admin-dashboard.service';
import { PagoAdmin, ResumenPagosDTO, AdminKpiCard } from '../../models/admin-dashboard.model';
import { AdminStatusBadgeComponent } from '../admin-status-badge/admin-status-badge';
import { AdminKpiCardComponent } from '../admin-kpi-card/admin-kpi-card';

@Component({
  selector: 'app-admin-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminStatusBadgeComponent, AdminKpiCardComponent],
  templateUrl: './admin-pagos.component.html',
  styleUrl: './admin-pagos.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminPagosComponent implements OnInit {
  private readonly adminService = inject(AdminDashboardService);

  // Input de tema visual (claro/oscuro)
  modoClaro = input(false);

  // States mediante Signals
  resumen = signal<ResumenPagosDTO | null>(null);
  kpiCards = signal<AdminKpiCard[]>([]);
  pagos = signal<PagoAdmin[]>([]);
  cargandoResumen = signal(true);
  cargandoTabla = signal(true);
  errorMsg = signal<string | null>(null);

  // Paginación
  paginaActual = signal(0);
  tamanioPagina = signal(10);
  totalElementos = signal(0);
  totalPaginas = signal(0);
  esUltima = signal(true);

  // Edición de estado
  pagoEnEdicion = signal<PagoAdmin | null>(null);
  cambiandoEstado = signal(false);

  ngOnInit(): void {
    this.cargarResumen();
    this.cargarPagos(0);
  }

  cargarResumen(): void {
    this.cargandoResumen.set(true);
    this.adminService.obtenerResumenPagos().subscribe({
      next: (res) => {
        if (res.exito && res.datos) {
          this.resumen.set(res.datos);
          
          // Mapear resumen financiero a tarjetas KPI
          const tarjetas: AdminKpiCard[] = [
            {
              etiqueta: 'Ingresos Totales',
              valor: this.formatoMoneda(res.datos.ingresosTotales),
              detalle: 'Total acumulado en pasarela',
              tendencia: '+12.4%',
              tendenciaTipo: 'up',
              icono: 'fa-solid fa-sack-dollar',
              tono: 'success'
            },
            {
              etiqueta: 'Transacciones',
              valor: res.datos.totalTransacciones.toString(),
              detalle: 'Intentos totales de pago',
              tendencia: '+8.2%',
              tendenciaTipo: 'up',
              icono: 'fa-solid fa-money-bill-transfer',
              tono: 'primary'
            },
            {
              etiqueta: 'Transacciones Exitosas',
              valor: (res.datos.transaccionesPorEstado['EXITOSO'] || 0).toString(),
              detalle: `Fallidas: ${res.datos.transaccionesPorEstado['FALLIDO'] || 0}`,
              tendencia: '92%',
              tendenciaTipo: 'neutral',
              icono: 'fa-solid fa-circle-check',
              tono: 'info'
            },
            {
              etiqueta: 'Planes PRO / PREMIUM',
              valor: `${res.datos.suscripcionesPorPlan['PRO'] || 0} / ${res.datos.suscripcionesPorPlan['PREMIUM'] || 0}`,
              detalle: 'Suscripciones activas',
              tendencia: '+15.5%',
              tendenciaTipo: 'up',
              icono: 'fa-solid fa-gem',
              tono: 'purple'
            }
          ];
          this.kpiCards.set(tarjetas);
        } else {
          this.errorMsg.set(res.mensaje || 'Error al obtener resumen de pagos');
        }
        this.cargandoResumen.set(false);
      },
      error: (err) => {
        this.errorMsg.set('No se pudo conectar con el servicio de pagos.');
        this.cargandoResumen.set(false);
      }
    });
  }

  cargarPagos(pagina: number): void {
    this.cargandoTabla.set(true);
    this.adminService.listarPagos(pagina, this.tamanioPagina()).subscribe({
      next: (res) => {
        if (res.exito && res.datos) {
          this.pagos.set(res.datos.contenido || []);
          this.paginaActual.set(res.datos.numeroPagina);
          this.totalElementos.set(res.datos.totalElementos);
          this.totalPaginas.set(res.datos.totalPaginas);
          this.esUltima.set(res.datos.esUltima);
        } else {
          this.errorMsg.set(res.mensaje || 'Error al listar pagos');
        }
        this.cargandoTabla.set(false);
      },
      error: (err) => {
        this.errorMsg.set('Error de red al listar historial de pagos.');
        this.cargandoTabla.set(false);
      }
    });
  }

  abrirModalEdicion(pago: PagoAdmin): void {
    this.pagoEnEdicion.set(pago);
  }

  cerrarModalEdicion(): void {
    this.pagoEnEdicion.set(null);
  }

  guardarNuevoEstado(nuevoEstado: string): void {
    const pago = this.pagoEnEdicion();
    if (!pago) return;
    if (nuevoEstado !== 'EXITOSO' && nuevoEstado !== 'PENDIENTE' && nuevoEstado !== 'FALLIDO') return;

    this.cambiandoEstado.set(true);
    this.adminService.corregirEstadoPago(pago.id, nuevoEstado).subscribe({
      next: (res) => {
        this.cambiandoEstado.set(false);
        this.cerrarModalEdicion();
        // Recargar datos
        this.cargarResumen();
        this.cargarPagos(this.paginaActual());
      },
      error: (err) => {
        this.cambiandoEstado.set(false);
        alert('No se pudo actualizar el estado del pago.');
      }
    });
  }

  siguientePagina(): void {
    if (!this.esUltima()) {
      this.cargarPagos(this.paginaActual() + 1);
    }
  }

  anteriorPagina(): void {
    if (this.paginaActual() > 0) {
      this.cargarPagos(this.paginaActual() - 1);
    }
  }

  formatoMoneda(monto: number, moneda: string = 'PEN'): string {
    const code = moneda === 'PEN' ? 'es-PE' : 'en-US';
    return new Intl.NumberFormat(code, {
      style: 'currency',
      currency: moneda,
      minimumFractionDigits: 2
    }).format(monto);
  }

  formatoFecha(fechaStr?: string): string {
    if (!fechaStr) return '-';
    try {
      const fecha = new Date(fechaStr);
      return fecha.toLocaleDateString('es-PE', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return fechaStr;
    }
  }

  obtenerTipoEstado(estado: string): 'ok' | 'warn' | 'error' {
    if (estado === 'EXITOSO') return 'ok';
    if (estado === 'PENDIENTE') return 'warn';
    return 'error';
  }

  formatoUsuarioId(uuid: string): string {
    if (!uuid) return '-';
    if (uuid.length <= 8) return uuid;
    return `USR-${uuid.slice(0, 8).toUpperCase()}`;
  }

  obtenerPlan(pago: PagoAdmin): string {
    if (pago.detalles && pago.detalles.length > 0) {
      return pago.detalles[0].planSolicitado;
    }
    return 'LUKA FREE';
  }

  obtenerMonto(pago: PagoAdmin): string {
    if (pago.detalles && pago.detalles.length > 0) {
      const det = pago.detalles[0];
      return this.formatoMoneda(det.monto, det.moneda);
    }
    return this.formatoMoneda(0, 'PEN');
  }
}
