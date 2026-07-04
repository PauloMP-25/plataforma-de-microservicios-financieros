import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminDashboardService } from '../../services/admin-dashboard.service';
import { AdminDashboardData, AdminPagoReciente, AdminServicioEstado } from '../../models/admin-dashboard.model';
import { AdminKpiCardComponent } from '../../components/admin-kpi-card/admin-kpi-card';
import { AdminStatusBadgeComponent } from '../../components/admin-status-badge/admin-status-badge';
import { AdminPagosComponent } from '../../components/admin-pagos/admin-pagos.component';
import { AdminMicroserviciosComponent } from '../../components/admin-microservicios/admin-microservicios.component';
import { AdminAuditoriaComponent } from '../../components/admin-auditoria/admin-auditoria.component';
import { AuthService } from '../../../../core/services/auth.service';

type AdminSeccion = 'dashboard' | 'usuarios' | 'pagos' | 'microservicios' | 'seguridad' | 'auditoria' | 'perfil';
type UsuarioEstado = 'ACTIVO' | 'INACTIVO' | 'SUSPENDIDO';
type UsuarioRol = 'FREE' | 'PRO' | 'PREMIUM';

interface AdminUsuario {
  id: number;
  nombre: string;
  email: string;
  rol: UsuarioRol;
  estado: UsuarioEstado;
  fecha: string;
  telefono: string;
  suscripcion: string;
  metas: number;
  limiteGasto: number | null;
  ultimoAcceso: string;
  transacciones: number;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, AdminKpiCardComponent, AdminStatusBadgeComponent, AdminPagosComponent, AdminMicroserviciosComponent, AdminAuditoriaComponent],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss'
})
export class AdminDashboard implements OnInit {
  data = signal<AdminDashboardData | null>(null);
  cargando = signal(true);
  seccionActiva = signal<AdminSeccion>('dashboard');
  modoClaro = signal(false);
  busquedaUsuarios = signal('');
  filtroEstadoUsuarios = signal<UsuarioEstado | 'TODOS'>('TODOS');
  usuarioSeleccionado = signal<AdminUsuario | null>(null);

  readonly usuarios: AdminUsuario[] = [
    { id: 1, nombre: 'Ana Torres', email: 'ana@mail.com', rol: 'PREMIUM', estado: 'ACTIVO', fecha: '2024-01-15', telefono: '+51 987 654 321', suscripcion: 'PREMIUM', metas: 3, limiteGasto: 2000, ultimoAcceso: '2024-06-01 14:32', transacciones: 28 },
    { id: 2, nombre: 'Luis Ramírez', email: 'luis@mail.com', rol: 'PRO', estado: 'ACTIVO', fecha: '2024-02-03', telefono: '+51 912 345 678', suscripcion: 'PRO', metas: 0, limiteGasto: null, ultimoAcceso: '2024-06-01 08:10', transacciones: 5 },
    { id: 3, nombre: 'Carla Díaz', email: 'carla@mail.com', rol: 'FREE', estado: 'INACTIVO', fecha: '2024-03-18', telefono: '+51 945 112 233', suscripcion: 'FREE', metas: 1, limiteGasto: 500, ultimoAcceso: '2024-03-17 11:00', transacciones: 4 },
    { id: 4, nombre: 'Pedro Solano', email: 'pedro@mail.com', rol: 'PREMIUM', estado: 'ACTIVO', fecha: '2024-03-22', telefono: '+51 900 222 333', suscripcion: 'PREMIUM', metas: 5, limiteGasto: 3000, ultimoAcceso: '2024-06-01 09:55', transacciones: 41 },
    { id: 5, nombre: 'Sofía Vega', email: 'sofia@mail.com', rol: 'PRO', estado: 'ACTIVO', fecha: '2024-04-01', telefono: '+51 933 444 555', suscripcion: 'PRO', metas: 2, limiteGasto: 1500, ultimoAcceso: '2024-05-31 20:00', transacciones: 17 },
    { id: 6, nombre: 'Miguel Oros', email: 'miguel@mail.com', rol: 'FREE', estado: 'SUSPENDIDO', fecha: '2024-04-10', telefono: '+51 966 777 888', suscripcion: 'FREE', metas: 0, limiteGasto: 300, ultimoAcceso: '2024-04-09 15:30', transacciones: 2 },
  ];

  usuariosFiltrados = computed(() => {
    const busqueda = this.busquedaUsuarios().trim().toLowerCase();
    const estado = this.filtroEstadoUsuarios();
    return this.usuarios.filter(usuario => (estado === 'TODOS' || usuario.estado === estado) && (!busqueda || usuario.nombre.toLowerCase().includes(busqueda) || usuario.email.toLowerCase().includes(busqueda)));
  });

  totalUsuarios = computed(() => this.usuarios.length);
  usuariosActivos = computed(() => this.usuarios.filter(usuario => usuario.estado === 'ACTIVO').length);
  usuariosSuspendidos = computed(() => this.usuarios.filter(usuario => usuario.estado === 'SUSPENDIDO').length);

  constructor(
    private adminDashboardService: AdminDashboardService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.adminDashboardService.obtenerResumen().subscribe((resumen) => {
      this.data.set(resumen);
      this.cargando.set(false);
    });
    this.usuarioSeleccionado.set(this.usuarios[0]);
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

  mostrarSeccion(seccion: AdminSeccion): void {
    this.seccionActiva.set(seccion);
  }

  alternarTema(): void {
    this.modoClaro.update((valor) => !valor);
  }

  tituloSeccionActual(): string {
    const titulos: Record<AdminSeccion, string> = {
      dashboard: 'Dashboard Admin Luka',
      usuarios: 'Gestión de Usuarios',
      pagos: 'Pagos y suscripciones',
      microservicios: 'Microservicios',
      seguridad: 'Seguridad',
      auditoria: 'Auditoría',
      perfil: 'Perfil administrador'
    };

    return titulos[this.seccionActiva()];
  }

  descripcionSeccionActual(): string {
    const descripciones: Record<AdminSeccion, string> = {
      dashboard: 'Vista operativa separada del dashboard de usuario.',
      usuarios: 'Administra, filtra y revisa el detalle de cada usuario.',
      pagos: 'Monitorea pagos, ingresos y suscripciones del sistema.',
      microservicios: 'Revisa el estado operativo de los servicios backend.',
      seguridad: 'Gestiona IPs bloqueadas, OTP y alertas de seguridad.',
      auditoria: 'Consulta eventos, acciones y trazabilidad del sistema.',
      perfil: 'Información del administrador autenticado y permisos activos.'
    };

    return descripciones[this.seccionActiva()];
  }

  cambiarFiltroEstadoUsuarios(estado: UsuarioEstado | 'TODOS'): void {
    this.filtroEstadoUsuarios.set(estado);
    this.usuarioSeleccionado.set(this.usuariosFiltrados()[0] ?? null);
  }

  actualizarBusquedaUsuarios(valor: string): void {
    this.busquedaUsuarios.set(valor);
    const usuarios = this.usuariosFiltrados();
    const seleccionado = this.usuarioSeleccionado();
    if (!seleccionado || !usuarios.some(usuario => usuario.id === seleccionado.id)) {
      this.usuarioSeleccionado.set(usuarios[0] ?? null);
    }
  }

  seleccionarUsuario(usuario: AdminUsuario): void {
    this.usuarioSeleccionado.set(usuario);
  }

  cerrarDetalleUsuario(): void {
    this.usuarioSeleccionado.set(null);
  }

  inicialesUsuario(nombre: string): string {
    return nombre.split(' ').map(parte => parte.charAt(0)).join('').slice(0, 2).toUpperCase();
  }

  formatoLimiteGasto(limite: number | null): string {
    if (limite === null) return 'Sin límite';
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN', maximumFractionDigits: 0 }).format(limite);
  }

  rolClase(rol: UsuarioRol): string {
    return rol.toLowerCase();
  }

  estadoClase(estado: UsuarioEstado): string {
    return estado.toLowerCase();
  }

  salir(): void {
    this.auth.logout();
  }
}
