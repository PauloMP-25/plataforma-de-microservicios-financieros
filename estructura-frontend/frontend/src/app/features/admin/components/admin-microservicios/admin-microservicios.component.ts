import { Component, OnInit, ChangeDetectionStrategy, input, signal, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminDashboardService } from '../../services/admin-dashboard.service';
import { AdminServicioEstado } from '../../models/admin-dashboard.model';

@Component({
  selector: 'app-admin-microservicios',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-microservicios.component.html',
  styleUrl: './admin-microservicios.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AdminMicroserviciosComponent implements OnInit {
  private readonly adminService = inject(AdminDashboardService);

  // Input de tema visual (claro/oscuro)
  modoClaro = input(false);

  // States
  servicios = signal<AdminServicioEstado[]>([]);
  servicioSeleccionado = signal<AdminServicioEstado | null>(null);
  cargandoServicios = signal(true);
  reinicioEnProgreso = signal<string | null>(null);
  logs = signal<string[]>([]);
  cargandoLogs = signal(false);

  // Computed KPIs para Microservicios
  kpis = computed(() => {
    const lista = this.servicios();
    return {
      totales: lista.length,
      saludables: lista.filter(s => s.estado === 'healthy').length,
      advertencias: lista.filter(s => s.estado === 'warning').length,
      offline: lista.filter(s => s.estado === 'down').length
    };
  });

  ngOnInit(): void {
    this.cargarServicios();
  }

  cargarServicios(seleccionarSiguiente = false): void {
    this.cargandoServicios.set(true);
    this.adminService.obtenerServicios().subscribe({
      next: (lista) => {
        this.servicios.set(lista);
        
        // Seleccionar primer servicio por defecto si ninguno lo está
        const actual = this.servicioSeleccionado();
        if (lista.length > 0) {
          if (!actual) {
            this.seleccionarServicio(lista[0]);
          } else {
            // Actualizar referencia del seleccionado
            const encontrado = lista.find(s => s.nombre === actual.nombre);
            if (encontrado) {
              this.servicioSeleccionado.set(encontrado);
              if (seleccionarSiguiente) {
                this.cargarLogs(encontrado.nombre);
              }
            }
          }
        }
        this.cargandoServicios.set(false);
      },
      error: () => {
        this.cargandoServicios.set(false);
      }
    });
  }

  seleccionarServicio(serv: AdminServicioEstado): void {
    this.servicioSeleccionado.set(serv);
    this.cargarLogs(serv.nombre);
  }

  cargarLogs(nombre: string): void {
    this.cargandoLogs.set(true);
    this.adminService.obtenerLogs(nombre).subscribe({
      next: (lineas) => {
        this.logs.set(lineas);
        this.cargandoLogs.set(false);
      },
      error: () => {
        this.cargandoLogs.set(false);
      }
    });
  }

  reiniciarServicio(serv: AdminServicioEstado): void {
    if (this.reinicioEnProgreso()) return;

    this.reinicioEnProgreso.set(serv.nombre);
    
    // Cambiar estado local a "down" / "rebooting" de forma simulada
    this.servicios.update(lista => 
      lista.map(s => s.nombre === serv.nombre ? { ...s, estado: 'down', latencia: 'rebooting' } : s)
    );
    this.logs.update(lineas => [
      ...lineas, 
      `[${new Date().toLocaleTimeString()}] [SYSTEM] Reboot signal received. Stopping service...`,
      `[${new Date().toLocaleTimeString()}] [SYSTEM] Shutting down connection pools...`
    ]);

    this.adminService.reiniciarServicio(serv.nombre).subscribe({
      next: () => {
        this.reinicioEnProgreso.set(null);
        this.cargarServicios(true);
      },
      error: () => {
        this.reinicioEnProgreso.set(null);
      }
    });
  }

  obtenerEstadoClase(estado: string): string {
    if (estado === 'healthy') return 'bg-emerald-500 shadow-emerald-500/50';
    if (estado === 'warning') return 'bg-amber-500 shadow-amber-500/50';
    return 'bg-red-500 shadow-red-500/50 animate-pulse';
  }

  obtenerTextoEstado(estado: string): string {
    if (estado === 'healthy') return 'Saludable';
    if (estado === 'warning') return 'Advertencia';
    return 'Fuera de Línea';
  }
}
