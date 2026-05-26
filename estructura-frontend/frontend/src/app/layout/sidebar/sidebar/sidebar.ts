// =============================================
// LUKA — Sidebar Component
// =============================================
import { Component, OnInit, signal } from '@angular/core';
import { CommonModule }               from '@angular/common';
import { RouterModule }               from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SidebarStateService } from '../../../core/services/sidebar-state.service';
import { NAV_ITEMS, BOTTOM_NAV_ITEMS } from '../../../config/navigation.config';
import { Router } from '@angular/router';
import { filter } from 'rxjs';
import { NavigationEnd } from '@angular/router';

// TODO: reemplazar con MascotaService.getMensajeDiario()
const MASCOT_MSGS = [
  '¡Hola! 🌿 Registra tus gastos hoy y mantén tu racha.',
  '💡 Tip: Guarda al menos el 20% de tus ingresos.',
  '🎉 ¡Vas genial! Sigue así y llega al próximo nivel.',
  '⚠️ ¿Revisaste tu presupuesto esta semana?',
  '🌟 Cada sol ahorrado cuenta. ¡Tú puedes!',
  '📊 Mira tus estadísticas — ¡aprendes más de lo que crees!',
];

@Component({
  selector:    'app-sidebar',
  standalone:  true,
  imports:     [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl:    './sidebar.scss'
})
export class Sidebar implements OnInit {




  
  // TODO: reemplazar con MenuService.getNavItems(usuarioRol)
  //       GET /api/menu?rol=estudiante  →  NavItem[]
  navItems       = NAV_ITEMS;
  bottomNavItems = BOTTOM_NAV_ITEMS;

  // ── Mascota —  usar showMascotMsg() y mascotMsg() ──
  showMascotMsg = signal(false);
  mascotMsg     = signal('');

  // TODO: obtener desde GamificacionService

  rachaActual = 14;   // mock
  metaRacha   = 30;   // mock 

// dentro de la clase, agrega:
isPerfilSection = false;

readonly perfilNavItems = [
  { route: '/perfil/cliente',       label: 'Perfil Cliente',    icon: 'fa-solid fa-user' },
  { route: '/perfil/financiero',    label: 'Perfil Financiero', icon: 'fa-solid fa-chart-pie' },
  { route: '/perfil/configuracion', label: 'Configuración',     icon: 'fa-solid fa-gear' },
  { route: '/perfil/historial',     label: 'Historial',         icon: 'fa-solid fa-clock-rotate-left' },
  { route: '/perfil/transacciones', label: 'Transacciones',     icon: 'fa-solid fa-arrow-right-arrow-left' },
];

  constructor(
    private router: Router,
    public auth:         AuthService,
    public sidebarState: SidebarStateService
  ) {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        this.isPerfilSection = e.url.startsWith('/perfil');
      });
  }

  ngOnInit(): void {
    // Muestra mensaje automático al cargar (después de 2s)
    // TODO: traer mensaje desde MascotaService en lugar del array local
    setTimeout(() => {
      this.mostrarMensajeAleatorio();
      this.showMascotMsg.set(true);
      // Auto-ocultar después de 5s
      setTimeout(() => this.showMascotMsg.set(false), 5000);
    }, 2000);
  }

  toggle(): void {
    this.sidebarState.toggle();
  }

  toggleMascotMsg(): void {
    if (!this.showMascotMsg()) {
      this.mostrarMensajeAleatorio();
    }
    this.showMascotMsg.update(v => !v);
  }

  private mostrarMensajeAleatorio(): void {
    const idx = Math.floor(Math.random() * MASCOT_MSGS.length);
    this.mascotMsg.set(MASCOT_MSGS[idx]);
  }
}