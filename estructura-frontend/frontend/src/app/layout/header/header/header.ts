
import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule }                     from '@angular/common';
import { RouterModule, Router,
         NavigationEnd, ActivatedRoute }    from '@angular/router';
import { filter, map }                      from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';

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
  imports:     [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrl:    './header.scss'
})
export class Header implements OnInit {


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

  constructor(
    public  auth:   AuthService,
    private router: Router,
    private route:  ActivatedRoute
  ) {}

  ngOnInit(): void {

    // Escucha cambios de ruta para actualizar título y breadcrumbs
    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        map(() => {
          let r = this.route;
          while (r.firstChild) r = r.firstChild;
          return r.snapshot.data;
        })
      )
      .subscribe(data => {
        this.pageTitle   = data['title']       ?? 'Luka';
        this.breadcrumbs = data['breadcrumbs'] ?? [];
        this.closeAll();
      });

    // Carga título inicial (sin esperar evento de navegación)
    let r = this.route;
    while (r.firstChild) r = r.firstChild;
    const data       = r.snapshot.data;
    this.pageTitle   = data['title']       ?? 'Resumen';
    this.breadcrumbs = data['breadcrumbs'] ?? [];

    // Mascota aparece automáticamente a los 4s
    // TODO: mensaje personalizado desde MascotaService
    setTimeout(() => {
      this.abrirMascota('¡Hola! 👋 ¿Ya registraste tus gastos de hoy?');
    }, 4000);
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

  logout(): void {
    // TODO: AuthService.logout() debe limpiar token y llamar
    //       POST /api/auth/logout antes de redirigir
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}