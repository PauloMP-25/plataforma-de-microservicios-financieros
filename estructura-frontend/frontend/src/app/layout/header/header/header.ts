
import { Component, OnInit, OnDestroy, HostListener, signal, effect, inject } from '@angular/core';
import { CommonModule }                     from '@angular/common';
import { RouterModule, Router,
         NavigationEnd, ActivatedRoute }    from '@angular/router';
import { filter, map }                      from 'rxjs/operators';
import { Subscription }                     from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AvatarService } from '../../../core/services/avatar.service';
import { AvatarDisplay } from '../../../features/perfil/perfil-cliente/components/avatar-display/avatar-display';
import { SidebarStateService } from '../../../core/services/sidebar-state.service';
import { FinancieroService } from '../../../core/services/Financiero.service';
import { AppEventBus } from '../../../core/services/app-event-bus.service';
import { Transacciones } from '../../../core/services/transacciones';
import { IaService } from '../../../core/services/ia.service';
import { DashboardStateService } from '../../../core/services/dashboard-state.service';

interface Breadcrumb  { label: string; route?: string; }

interface Notificacion {
  id: number; icon: string; color: string;
  text: string; time: string; read: boolean;
}

interface Tema {
  id: string; label: string; color: string;
  vars: Record<string, string>;
}

// TODO: reemplazar con MascotaService.getMensajeDiario()

const FLOAT_MSGS = [
  '¡Hola! 👋 ¿Ya registraste tus gastos de hoy?',
  '🌿 Recuerda: pequeños ahorros hacen grandes metas.',
  '🎯 Estás al 68% de tu meta principal. ¡Casi!',
  '⚠️ Tu gasto en comida subió 12% esta semana.',
  '🔥 ¡14 días de racha! Sigue así, estás imparable.',
  '💡 Tip: Usa presupuestos por categoría para ahorrar más.',
];

@Component({
  selector:    'app-header',
  standalone:  true,
  imports:     [CommonModule, RouterModule, AvatarDisplay],
  templateUrl: './header.html',
  styleUrl:    './header.scss'
})
export class Header implements OnInit, OnDestroy {
  public readonly iaService = inject(IaService);
  public readonly avatarService = inject(AvatarService);
  public readonly dashboardState = inject(DashboardStateService);


  pageTitle   = 'Resumen';
  breadcrumbs: Breadcrumb[] = [];


  // ── Estado dropdowns ──
  showNotifications = false;
  showUserMenu      = false;
  showThemePicker   = false;

  // ── Notificaciones mock ──
  // TODO: inyectar NotificacionService
  //       GET  /api/notificaciones?leidas=false  → Notificacion[]
  notificaciones: Notificacion[] = [
    { id: 1, icon: 'fa-solid fa-triangle-exclamation', color: '#f5a623',
      text: 'Estás al 80% de tu presupuesto de comida', time: 'Hace 5 min', read: false },
    { id: 2, icon: 'fa-solid fa-circle-check',         color: '#1db954',
      text: '¡Meta de ahorro "Viaje" completada!',      time: 'Hace 1h',   read: false },
    { id: 3, icon: 'fa-solid fa-arrow-trend-down',     color: '#00d4aa',
      text: 'Tus gastos bajaron un 12% esta semana',    time: 'Ayer',       read: true  },
  ];

  get notifCount(): number {
    return this.notificaciones.filter(n => !n.read).length;
  }

  // ── Temas ──
  // TODO: persistir temaActual en PreferenciasService
  //       GET  /api/usuario/preferencias → { tema: string }
  //       PATCH /api/usuario/preferencias { tema: string }
  temas: Tema[] = [
    { id: 'nature', label: 'Naturaleza', color: '#1db954',
      vars: { '--c-primary': '#1db954', '--c-accent': '#00d4aa' } },
    { id: 'ocean',  label: 'Océano',    color: '#0ea5e9',
      vars: { '--c-primary': '#0ea5e9', '--c-accent': '#06b6d4' } },
    { id: 'sunset', label: 'Sunset',    color: '#f97316',
      vars: { '--c-primary': '#f97316', '--c-accent': '#ef4444' } },
    { id: 'violet', label: 'Violeta',   color: '#8b5cf6',
      vars: { '--c-primary': '#8b5cf6', '--c-accent': '#a78bfa' } },
    { id: 'gold',   label: 'Dorado',    color: '#f59e0b',
      vars: { '--c-primary': '#f59e0b', '--c-accent': '#fbbf24' } },
  ];
  temaActual = 'nature';

  // ── Mascota flotante ──
 
  showFloatMsg    = false;
  floatMascotMsg  = '';
  floatBounce     = false;
  floatBadgeCount = 1;   // mock

  // Racha de transacciones del usuario
  rachaDias = signal<number>(0);

  private txSub?: Subscription;

  constructor(
    public  auth:   AuthService,
    public financiero: FinancieroService,
    public sidebarState: SidebarStateService,
    private router: Router,
    private route:  ActivatedRoute,
    private eventBus: AppEventBus,
    private transaccionesService: Transacciones
  ) {
    // Cargar la racha de forma reactiva ante cambios de usuario
    effect(() => {
      const user = this.auth.usuario();
      if (user) {
        this.cargarRacha();
      } else {
        this.rachaDias.set(0);
      }
    });
  }

  toggleSidebarMobile(): void {
    this.sidebarState.toggleMobile();
  }

  ngOnInit(): void {
    this.financiero.cargarResumen();

    this.txSub = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      this.financiero.cargarResumen();
      this.cargarRacha();
    });

    // Escucha cambios de ruta para actualizar título y breadcrumbs
    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        map(() => {
          let r = this.route;
          while (r.firstChild) r = r.firstChild;
          return { data: r.snapshot.data, queryParams: r.snapshot.queryParams };
        })
      )
      .subscribe(({data, queryParams}) => {
        this.pageTitle   = data['title']       ?? 'Luka';
        this.breadcrumbs = data['breadcrumbs'] ? [...data['breadcrumbs']] : [];
        this.actualizarBreadcrumbsModulo(queryParams);
        this.closeAll();
      });

    // Carga título inicial (sin esperar evento de navegación)
    let r = this.route;
    while (r.firstChild) r = r.firstChild;
    const data       = r.snapshot.data;
    const queryParams = r.snapshot.queryParams;
    this.pageTitle   = data['title']       ?? 'Resumen';
    this.breadcrumbs = data['breadcrumbs'] ? [...data['breadcrumbs']] : [];
    this.actualizarBreadcrumbsModulo(queryParams);

    // Mascota aparece automáticamente a los 4s
    // TODO: mensaje personalizado desde MascotaService
    setTimeout(() => {
      this.abrirMascota('¡Hola! 👋 ¿Ya registraste tus gastos de hoy?');
    }, 4000);
  }

  ngOnDestroy(): void {
    this.txSub?.unsubscribe();
  }

  private actualizarBreadcrumbsModulo(queryParams: any): void {
    const modulo = queryParams['modulo'];
    if (modulo) {
      const MODULO_NOMBRES: Record<string, string> = {
        'predecir-gastos': 'PREDICCIÓN DE GASTOS',
        'gasto-hormiga': 'GASTOS HORMIGA',
        'simular-meta': 'SIMULAR META',
        'reporte-completo': 'REPORTE EJECUTIVO',
        'estilo-vida': 'ESTILO DE VIDA',
        'habitos-financieros': 'HÁBITOS FINANCIEROS',
        'reto-ahorro': 'RETO DE AHORRO'
      };
      const nombre = MODULO_NOMBRES[modulo] || modulo.replace(/-/g, ' ').toUpperCase();
      this.breadcrumbs.push({ label: nombre });
    }
  }

  get totalIngresosMes(): number {
    return this.dashboardState.resumenYTD()?.totalIngresos ?? this.financiero.resumen()?.totalIngresos ?? 3850;
  }

  get totalGastosMes(): number {
    return this.dashboardState.resumenYTD()?.totalGastos ?? this.financiero.resumen()?.totalGastos ?? 2950;
  }

  get balanceActual(): number {
    return this.dashboardState.resumenYTD()?.balance ?? (this.totalIngresosMes - this.totalGastosMes);
  }

  get saludTexto(): 'Buena' | 'Atención' | 'Crítica' {
    if (this.totalIngresosMes <= 0) return 'Crítica';
    const ratio = this.totalGastosMes / this.totalIngresosMes;
    if (ratio <= 0.7) return 'Buena';
    if (ratio <= 0.95) return 'Atención';
    return 'Crítica';
  }

  get saludClase(): string {
    switch (this.saludTexto) {
      case 'Buena':
        return 'health-badge--good';
      case 'Atención':
        return 'health-badge--warn';
      default:
        return 'health-badge--bad';
    }
  }

  formatSoles(value: number): string {
    if (value === undefined || value === null) return 'S/ 0.00';
    const absolute = Math.abs(value);
    const sign = value < 0 ? '-' : '';
    
    let formatted = '';
    if (absolute >= 1_000_000) {
      formatted = `${(absolute / 1_000_000).toFixed(2).replace(/\.00$/, '')}M`;
    } else if (absolute >= 100_000) {
      formatted = `${(absolute / 1_000).toFixed(2).replace(/\.00$/, '')}K`;
    } else if (absolute >= 10_000) {
      formatted = `${(absolute / 1_000).toFixed(1).replace(/\.0$/, '')}K`;
    } else {
      return new Intl.NumberFormat('es-PE', {
        style: 'currency',
        currency: 'PEN',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(value);
    }
    
    return `${sign}S/ ${formatted}`;
  }

  // Cierra todos los dropdowns al hacer clic fuera del header
  @HostListener('document:click')
  onDocClick(): void { this.closeAll(); }

  closeAll(): void {
    this.showNotifications = false;
    this.showUserMenu      = false;
    this.showThemePicker   = false;
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    this.showUserMenu = this.showThemePicker = false;
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
    this.showNotifications = this.showThemePicker = false;
  }

  toggleThemePicker(): void {
    this.showThemePicker = !this.showThemePicker;
    this.showNotifications = this.showUserMenu = false;
  }

  markAllRead(): void {
    // llamar NotificacionService.marcarTodasLeidas()
    //       PATCH /api/notificaciones/leer-todas
    this.notificaciones.forEach(n => n.read = true);
  }

  setTema(tema: Tema): void {
    this.temaActual = tema.id;
    Object.entries(tema.vars).forEach(([k, v]) => {
      document.documentElement.style.setProperty(k, v);
      document.documentElement.style.setProperty(k + '-soft', v + '18');
    });
    
    // TODO: persistir en PreferenciasService
    //       PATCH /api/usuario/preferencias { tema: tema.id }
    this.showThemePicker = false;
  }

  toggleFloatMsg(): void {
    if (!this.showFloatMsg) {
      // TODO: obtener mensaje desde MascotaService
      const idx = Math.floor(Math.random() * FLOAT_MSGS.length);
      this.floatMascotMsg  = FLOAT_MSGS[idx];
      this.floatBadgeCount = 0;
    }
    this.showFloatMsg = !this.showFloatMsg;
    this.floatBounce  = true;
    setTimeout(() => this.floatBounce = false, 700);
  }

  closeFloatMsg(): void { this.showFloatMsg = false; }

  abrirMascota(msg: string): void {
    this.floatMascotMsg  = msg;
    this.showFloatMsg    = true;
    this.floatBounce     = true;
    this.floatBadgeCount = 0;
    setTimeout(() => this.floatBounce = false, 700);
  }

  calcularRacha(transacciones: any[]): number {
    if (!transacciones || transacciones.length === 0) {
      return 0;
    }

    // Extraer fechas en formato YYYY-MM-DD
    const fechasSet = new Set<string>();
    transacciones.forEach(t => {
      const fechaStr = t.fechaRegistro || t.fechaTransaccion;
      if (fechaStr) {
        const fecha = new Date(fechaStr);
        if (!isNaN(fecha.getTime())) {
          const yyyy = fecha.getFullYear();
          const mm = String(fecha.getMonth() + 1).padStart(2, '0');
          const dd = String(fecha.getDate()).padStart(2, '0');
          fechasSet.add(`${yyyy}-${mm}-${dd}`);
        }
      }
    });

    const fechasOrdenadas = Array.from(fechasSet).sort((a, b) => b.localeCompare(a));
    if (fechasOrdenadas.length === 0) {
      return 0;
    }

    const hoyObj = new Date();
    const hoyStr = `${hoyObj.getFullYear()}-${String(hoyObj.getMonth() + 1).padStart(2, '0')}-${String(hoyObj.getDate()).padStart(2, '0')}`;
    
    const ayerObj = new Date();
    ayerObj.setDate(ayerObj.getDate() - 1);
    const ayerStr = `${ayerObj.getFullYear()}-${String(ayerObj.getMonth() + 1).padStart(2, '0')}-${String(ayerObj.getDate()).padStart(2, '0')}`;

    const masReciente = fechasOrdenadas[0];
    
    // Si la más reciente no es hoy ni ayer, racha es 0
    if (masReciente !== hoyStr && masReciente !== ayerStr) {
      return 0;
    }

    let racha = 1;
    let fechaActual = new Date(masReciente + 'T00:00:00');

    for (let i = 1; i < fechasOrdenadas.length; i++) {
      const fechaSiguiente = new Date(fechasOrdenadas[i] + 'T00:00:00');
      const difTiempo = fechaActual.getTime() - fechaSiguiente.getTime();
      const difDias = Math.round(difTiempo / (1000 * 60 * 60 * 24));

      if (difDias === 1) {
        racha++;
        fechaActual = fechaSiguiente;
      } else if (difDias > 1) {
        break; // Hueco en la racha
      }
    }

    return racha;
  }

  cargarRacha(): void {
    const usuarioId = this.auth.usuario()?.id;
    if (!usuarioId) {
      this.rachaDias.set(0);
      return;
    }

    // Listar las últimas 100 transacciones para computar la racha
    this.transaccionesService.listarHistorial({ pagina: 0, tamanio: 100 }).subscribe({
      next: (pagina) => {
        if (pagina && pagina.content) {
          const racha = this.calcularRacha(pagina.content);
          this.rachaDias.set(racha);
          localStorage.setItem('luka_racha_mock', racha.toString());
        } else {
          this.rachaDias.set(0);
        }
      },
      error: (err) => {
        console.error('Error al recuperar historial para racha:', err);
        const localRacha = localStorage.getItem('luka_racha_mock');
        this.rachaDias.set(localRacha ? parseInt(localRacha, 10) : 14);
      }
    });
  }

  logout(): void {
    // TODO: AuthService.logout() debe limpiar token y llamar
    //       POST /api/auth/logout antes de redirigir
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
