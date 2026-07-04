import { Component, OnInit, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AdminDashboardService } from '../../services/admin-dashboard.service';
import { AuditoriaService } from '../../../../core/services/auditoria.service';
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
  id: string;
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

  // Signals para datos reales del backend
  usuarios = signal<AdminUsuario[]>([]);
  listaNegra = signal<any[]>([]);
  otpsBloqueados = signal<any[]>([]);

  // Paginación de usuarios
  paginaActual = signal(0);
  tamanioPagina = signal(10);
  totalPaginas = signal(0);
  totalElementos = signal(0);
  esUltima = signal(true);

  usuariosFiltrados = computed(() => this.usuarios());
  totalUsuarios = computed(() => this.totalElementos());
  usuariosActivos = computed(() => this.usuarios().filter(usuario => usuario.estado === 'ACTIVO').length);
  usuariosSuspendidos = computed(() => this.usuarios().filter(usuario => usuario.estado === 'SUSPENDIDO').length);

  constructor(
    private adminDashboardService: AdminDashboardService,
    private auditoriaService: AuditoriaService,
    public auth: AuthService
  ) {
    // Efecto reactivo para actualizar usuarios al cambiar filtros
    effect(() => {
      this.filtroEstadoUsuarios();
      this.busquedaUsuarios();
      this.cargarUsuarios(0);
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.cargarResumenDashboard();
    this.cargarListaNegra(0);
    this.cargarOtpsBloqueados();
  }

  cargarResumenDashboard(): void {
    this.cargando.set(true);
    this.adminDashboardService.obtenerResumen().subscribe({
      next: (resumen) => {
        const dataCloned = JSON.parse(JSON.stringify(resumen)) as AdminDashboardData;
        this.data.set(dataCloned);

        // Conectar KPIs reales de ingresos y pagos desde ms-pagos
        const anioActual = new Date().getFullYear();
        this.adminDashboardService.obtenerResumenPagos(anioActual).subscribe({
          next: (pagosRes) => {
            if (pagosRes.exito && pagosRes.datos) {
              const d = pagosRes.datos;
              const kpiIngresos = dataCloned.kpis.find(k => k.etiqueta.includes('Ingresos'));
              if (kpiIngresos) {
                kpiIngresos.valor = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(d.ingresosTotales);
                kpiIngresos.detalle = 'Acumulado real en pasarela';
              }
              const kpiExitosos = dataCloned.kpis.find(k => k.etiqueta.includes('exitosos') || k.etiqueta.includes('exitosos') || k.etiqueta.includes('Pagos exitosos'));
              if (kpiExitosos) {
                kpiExitosos.valor = (d.transaccionesPorEstado['EXITOSO'] || 0).toLocaleString();
                kpiExitosos.detalle = `Tasa exitosa real de ${d.totalTransacciones} transacciones`;
              }
              if (d.graficoIngresos && d.graficoIngresos.length > 0) {
                dataCloned.graficoIngresos = d.graficoIngresos;
              }
            }
          }
        });

        // Conectar total usuarios real desde ms-usuario
        this.adminDashboardService.obtenerUsuarios(undefined, undefined, undefined, 0, 1).subscribe({
          next: (userRes) => {
            if (userRes.exito && userRes.datos) {
              const kpiUsers = dataCloned.kpis.find(k => k.etiqueta.includes('Usuarios'));
              if (kpiUsers) {
                const total = userRes.datos.totalElementos ?? (userRes.datos as any).totalElements ?? 0;
                kpiUsers.valor = total.toLocaleString();
                kpiUsers.detalle = 'Usuarios registrados reales';
              }
            }
          }
        });

        // Conectar total auditorías reales desde ms-auditoria
        this.auditoriaService.listarRegistros({ pagina: 0, tamanio: 1 }).subscribe({
          next: (audRes) => {
            const kpiAudit = dataCloned.kpis.find(k => k.etiqueta.includes('auditados') || k.etiqueta.includes('Auditoría') || k.etiqueta.includes('Eventos'));
            if (kpiAudit) {
              const totalAudit = audRes.totalElements ?? (audRes as any).totalElementos ?? 0;
              kpiAudit.valor = totalAudit.toLocaleString();
              kpiAudit.detalle = 'Eventos registrados en ms-auditoria';
            }
          }
        });

        // Cargar lista de pagos reales para la tabla principal
        this.adminDashboardService.listarPagos(0, 5).subscribe({
          next: (pagosListRes) => {
            if (pagosListRes.exito && pagosListRes.datos) {
              dataCloned.pagos = pagosListRes.datos.contenido.map((p) => {
                const plan = p.detalles && p.detalles.length > 0 ? p.detalles[0].planSolicitado : 'FREE';
                const monto = p.detalles && p.detalles.length > 0 ? p.detalles[0].monto : 0;
                const userMatch = this.usuarios().find(u => u.id === p.usuarioId);
                const nombreUsuario = userMatch ? userMatch.nombre : p.usuarioId.slice(0, 8).toUpperCase();
                return {
                  id: `PAG-${p.id.slice(0, 4).toUpperCase()}`,
                  usuario: nombreUsuario,
                  monto: new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(monto),
                  plan,
                  estado: p.estado
                };
              });
            }
          }
        });

        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }

  cargarUsuarios(pagina: number): void {
    const estado = this.filtroEstadoUsuarios();
    const habilitado = estado === 'ACTIVO' ? true : estado === 'INACTIVO' ? false : undefined;

    this.adminDashboardService.obtenerUsuarios(habilitado, undefined, this.busquedaUsuarios() || undefined, pagina, this.tamanioPagina()).subscribe({
      next: (res) => {
        if (res.exito && res.datos) {
          const mapped = (res.datos.contenido || []).map((u: any): AdminUsuario => ({
            id: u.id,
            nombre: u.nombreUsuario,
            email: u.correo,
            rol: (u.planActual || 'FREE') as UsuarioRol,
            estado: (u.habilitado ? 'ACTIVO' : 'INACTIVO') as UsuarioEstado,
            fecha: u.fechaCreacion ? u.fechaCreacion.split('T')[0] : '',
            telefono: '+51 987 654 321', // mock
            suscripcion: (u.planActual || 'FREE') as UsuarioRol,
            metas: 0,
            limiteGasto: null,
            ultimoAcceso: u.fechaActualizacion ? u.fechaActualizacion.replace('T', ' ').slice(0, 16) : '',
            transacciones: 0
          }));
          this.usuarios.set(mapped);
          this.paginaActual.set(res.datos.numeroPagina);
          this.totalPaginas.set(res.datos.totalPaginas);
          this.totalElementos.set(res.datos.totalElementos);
          this.esUltima.set(res.datos.esUltima);

          if (mapped.length > 0) {
            const actual = this.usuarioSeleccionado();
            if (!actual || !mapped.some((m: any) => m.id === actual.id)) {
              this.usuarioSeleccionado.set(mapped[0]);
            }
          } else {
            this.usuarioSeleccionado.set(null);
          }
        }
      }
    });
  }

  cargarListaNegra(pagina: number): void {
    this.adminDashboardService.obtenerListaNegraIp(pagina, 10).subscribe({
      next: (res) => {
        if (res.exito && res.datos) {
          this.listaNegra.set(res.datos.contenido || []);
        }
      }
    });
  }

  cargarOtpsBloqueados(): void {
    this.adminDashboardService.obtenerOtpsBloqueados().subscribe({
      next: (res) => {
        if (res.exito && res.datos) {
          this.otpsBloqueados.set(res.datos || []);
        }
      }
    });
  }

  bloquearIp(ip: string, motivo: string): void {
    if (!ip || !motivo) return;
    this.adminDashboardService.bloquearIp(ip, motivo).subscribe({
      next: (res) => {
        if (res.exito) {
          this.cargarListaNegra(0);
          this.cargarResumenDashboard();
        }
      }
    });
  }

  desbloquearIp(ip: string): void {
    if (!ip) return;
    this.adminDashboardService.desbloquearIp(ip).subscribe({
      next: (res) => {
        if (res.exito) {
          this.cargarListaNegra(0);
          this.cargarResumenDashboard();
        }
      }
    });
  }

  desbloquearUsuarioOtp(usuarioId: string): void {
    if (!usuarioId) return;
    this.adminDashboardService.desbloquearUsuarioOtp(usuarioId).subscribe({
      next: (res) => {
        if (res.exito) {
          this.cargarOtpsBloqueados();
          this.cargarResumenDashboard();
        }
      }
    });
  }

  cambiarPaginaUsuarios(dir: number): void {
    const target = this.paginaActual() + dir;
    if (target >= 0 && target < this.totalPaginas()) {
      this.cargarUsuarios(target);
    }
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
  }

  actualizarBusquedaUsuarios(valor: string): void {
    this.busquedaUsuarios.set(valor);
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
