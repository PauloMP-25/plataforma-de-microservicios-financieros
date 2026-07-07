import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ClientePerfilService } from '../../../core/services/cliente-perfil.service';
import { RespuestaDatosPersonales, SolicitudDatosPersonales } from '../../../core/models/cliente/perfil-cliente.model';
import { AvatarService } from '../../../core/services/avatar.service';
import { ServicioTema } from '../../../core/services/servicio-tema';
import { SuscripcionService } from '../../../core/services/suscripcion.service';
import { ModalPlanes } from '../../suscripcion/components/modal-planes/modal-planes';
import { finalize } from 'rxjs';
import { OnboardingTour, TourStep } from '../../../shared/components/onboarding-tour/onboarding-tour';

@Component({
  selector: 'app-configuracion',
  standalone:true,
  imports: [CommonModule, FormsModule, RouterLink, ModalPlanes, OnboardingTour],
  templateUrl: './configuracion.html',
  styleUrls: ['./configuracion.scss'],
})
export class Configuracion implements OnInit {

  modalPlanesAbierto = signal(false);
  comprandoPlan = signal(false);

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: '#tour-apariencia',
      title: 'Apariencia y Tema',
      description: 'Elige entre tema Claro u Oscuro para adaptar la interfaz de Luka a tus preferencias visuales y condiciones de luz.',
      position: 'bottom'
    },
    {
      targetSelector: '#tour-color-principal',
      title: 'Color de Acento',
      description: 'Personaliza los botones y elementos interactivos de la aplicación seleccionando tu color favorito en esta paleta.',
      position: 'bottom'
    },
    {
      targetSelector: '#tour-ia-premium',
      title: 'Inteligencia Artificial y Premium',
      description: 'Mejora tu plan para acceder al Asistente de IA Financiera, análisis predictivos avanzados y gráficos históricos detallados.',
      position: 'top'
    },
    {
      targetSelector: '#tour-zona-peligro',
      title: 'Zona de Peligro',
      description: 'Desde aquí puedes gestionar opciones de alta sensibilidad de tu cuenta, como la eliminación definitiva del perfil.',
      position: 'top'
    }
  ];

  completarTour(): void {
    localStorage.setItem('luka_tour_configuracion_visto', 'true');
    this.mostrarTour.set(false);
  }

  ngOnInit(): void {
    const tourVisto = localStorage.getItem('luka_tour_configuracion_visto');
    if (!tourVisto) {
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
  }

  abrirModalPlanes(): void {
    this.modalPlanesAbierto.set(true);
  }

  cerrarModalPlanes(): void {
    this.modalPlanesAbierto.set(false);
  }

  comprarPlan(plan: 'PRO' | 'PREMIUM', proveedor: 'STRIPE' | 'MERCADOPAGO' = 'STRIPE'): void {
    if (this.comprandoPlan()) return;
    this.comprandoPlan.set(true);

    this.suscripcionService.crearSesionCheckout(plan, proveedor)
      .pipe(finalize(() => this.comprandoPlan.set(false)))
      .subscribe({
        next: (sesion) => {
          if (sesion && sesion.urlCheckout) {
            window.location.href = sesion.urlCheckout;
          } else {
            console.error('No se recibió la URL de redirección del checkout');
          }
        },
        error: (err) => {
          console.error(`Error al iniciar Checkout de ${proveedor}:`, err);
        }
      });
  }
  // Servicios usados:
  readonly authService = inject(AuthService);
  readonly avatarService = inject(AvatarService);
  readonly servicioTema = inject(ServicioTema);
  private readonly clientePerfilService = inject(ClientePerfilService);
  private readonly suscripcionService = inject(SuscripcionService);
  private readonly router = inject(Router);

  /**
   * Computed signal que centraliza la validación del plan.
   * true  → usuario PREMIUM o PRO  → muestra banner de IA.
   * false → usuario FREE            → muestra banner de upgrade.
   */
  readonly esPlanPremiumOPro = computed(() =>
    this.authService.esPremium() || this.authService.esPro()
  );

  /** Navega a la sección de Inteligencia Artificial. */
  navegarAIA(): void {
    this.router.navigate(['/inteligencia-artificial']);
  }

  readonly tema = signal<'oscuro' | 'claro'>('claro');
  readonly colorPrincipal = signal<string>('#6d4aff');

  readonly perfil = signal<RespuestaDatosPersonales | null>(null);
  readonly modalEditarPerfilAbierto = signal(false);
  readonly guardandoPerfil = signal(false);
  readonly mensajePerfil = signal('');
  readonly eliminandoCuenta = signal(false);
  readonly mensajeCuenta = signal('');
  readonly modalProAbierto = signal(false);
  readonly modalEliminarCuentaAbierto = signal(false);
  readonly confirmacionEliminarCuenta = signal('');
  readonly fraseConfirmacionEliminarCuenta = 'Estoy de acuerdo con la eliminación de la cuenta';
  readonly aceptaRiesgo = signal(false);

  readonly formNombres = signal('');
  readonly formApellidos = signal('');
  readonly formTelefono = signal('');
  readonly formPais = signal('');
  readonly formCiudad = signal('');

  readonly coloresDisponibles = ['#6d4aff', '#4361ee', '#12b3a6', '#db2777', '#ea580c'];
  private readonly THEME_KEY = 'luka-theme';
  private readonly ACCENT_KEY = 'luka_accent_color';

  constructor() {
    // Inicializa tema global y luego carga datos de perfil del backend.
    this.inicializarTemaGlobal();
    this.inicializarColorGlobal();
    this.cargarPerfil();
  }

  seleccionarTema(tema: 'oscuro' | 'claro'): void {
    // Cambio de tema reactivo + persistente para toda la aplicación.
    this.tema.set(tema);
    this.aplicarTemaGlobal(tema);
  }

  seleccionarColor(color: string): void {
    this.colorPrincipal.set(color);
    this.aplicarColorGlobal(color);
  }

  abrirModalPro(): void {
    this.modalProAbierto.set(true);
  }

  cerrarModalPro(): void {
    this.modalProAbierto.set(false);
  }

  abrirModalEliminarCuenta(): void {
    this.mensajeCuenta.set('');
    this.confirmacionEliminarCuenta.set('');
    this.modalEliminarCuentaAbierto.set(true);
  }

  cerrarModalEliminarCuenta(): void {
    if (this.eliminandoCuenta()) return;
    this.modalEliminarCuentaAbierto.set(false);
    this.confirmacionEliminarCuenta.set('');
  }

  confirmacionEliminarCuentaValida(): boolean {
    return this.confirmacionEliminarCuenta().trim() === this.fraseConfirmacionEliminarCuenta;
  }

  abrirModalEditarPerfil(): void {
    // Pre-carga el formulario del modal con el snapshot actual del perfil.
    const p = this.perfil();
    if (p) {
      this.formNombres.set(p.nombres ?? '');
      this.formApellidos.set(p.apellidos ?? '');
      this.formTelefono.set(p.telefono ?? '');
      this.formPais.set(p.pais ?? '');
      this.formCiudad.set(p.ciudad ?? '');
    }
    this.mensajePerfil.set('');
    this.modalEditarPerfilAbierto.set(true);
  }

  cerrarModalEditarPerfil(): void {
    this.modalEditarPerfilAbierto.set(false);
  }

  guardarPerfil(): void {
    const usuarioId = this.authService.usuario()?.id;
    const p = this.perfil();

    if (!usuarioId || !p) {
      this.mensajePerfil.set('No hay datos de perfil para actualizar.');
      return;
    }

    const payload: SolicitudDatosPersonales = {
      dni: p.dni,
      genero: p.genero,
      edad: p.edad,
      fotoPerfilUrl: p.fotoPerfilUrl,
      nombres: this.formNombres().trim(),
      apellidos: this.formApellidos().trim(),
      telefono: this.formTelefono().trim(),
      pais: this.formPais().trim(),
      ciudad: this.formCiudad().trim(),
    };

    this.guardandoPerfil.set(true);
    // Persistencia real de cambios del perfil en backend.
    this.clientePerfilService.actualizarPerfil(usuarioId, payload).subscribe({
      next: (perfilActualizado) => {
        this.guardandoPerfil.set(false);
        this.perfil.set(perfilActualizado);
        this.modalEditarPerfilAbierto.set(false);
        this.mensajePerfil.set('Perfil actualizado correctamente.');
      },
      error: () => {
        this.guardandoPerfil.set(false);
        this.mensajePerfil.set('No se pudo actualizar el perfil.');
      },
    });
  }

  eliminarCuenta(): void {
    if (!this.aceptaRiesgo()) {
      return;
    }

    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      this.mensajeCuenta.set('No se encontró sesión activa para eliminar la cuenta.');
      return;
    }

    this.eliminandoCuenta.set(true);
    this.mensajeCuenta.set('');

    this.authService
      .eliminarMiCuenta()
      .pipe(finalize(() => this.eliminandoCuenta.set(false)))
      .subscribe({
        next: () => {
          this.authService.logout();
        },
        error: () => {
          this.mensajeCuenta.set('No se pudo procesar la solicitud de eliminación. Inténtalo nuevamente.');
        },
      });
  }

  private cargarPerfil(): void {
    // Carga inicial de perfil por usuario autenticado.
    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      return;
    }

    this.clientePerfilService.obtenerPerfil(usuarioId).subscribe({
      next: (perfil) => this.perfil.set(perfil),
      error: () => this.perfil.set(null),
    });
  }

  private inicializarTemaGlobal(): void {
    // Solo sincroniza estado visual local; NO cambia tema al cargar Configuración.
    // Fallback: claro por defecto si no existe preferencia.
    const guardado = localStorage.getItem(this.THEME_KEY);
    const temaInicial: 'oscuro' | 'claro' =
      guardado === 'oscuro' || guardado === 'claro'
        ? guardado
        : (this.servicioTema.temaOscuro() ? 'oscuro' : 'claro');

    console.debug('[TemaDebug][Configuracion] init', {
      guardado,
      temaInicial,
      bodyThemeDarkAntes: document.body.classList.contains('theme-dark'),
      bodyDarkAntes: document.body.classList.contains('dark')
    });

    this.tema.set(temaInicial);
    // Importante: no invocar aplicarTemaGlobal aquí.
  }

  private aplicarTemaGlobal(tema: 'oscuro' | 'claro'): void {
    // Punto único de verdad centralizado en ServicioTema.
    console.debug('[TemaDebug][Configuracion] aplicarTemaGlobal', { tema });
    this.servicioTema.setTema(tema);
    localStorage.setItem(this.THEME_KEY, tema);
  }

  private inicializarColorGlobal(): void {
    const guardado = localStorage.getItem(this.ACCENT_KEY);
    const colorInicial = guardado && this.coloresDisponibles.includes(guardado)
      ? guardado
      : this.colorPrincipal();

    this.colorPrincipal.set(colorInicial);
    this.aplicarColorGlobal(colorInicial);
  }

  private aplicarColorGlobal(color: string): void {
    document.documentElement.style.setProperty('--accent-primary', color);
    document.documentElement.style.setProperty('--color-primary', color);

    // Derivados para conservar coherencia visual global
    const light = this.mixHex(color, '#ffffff', 0.25);
    const dark = this.mixHex(color, '#000000', 0.22);
    const soft = this.hexToRgba(color, 0.14);

    document.documentElement.style.setProperty('--color-primary-light', light);
    document.documentElement.style.setProperty('--color-primary-dark', dark);
    document.documentElement.style.setProperty('--color-primary-soft', soft);
    localStorage.setItem(this.ACCENT_KEY, color);
  }

  private mixHex(base: string, mixWith: string, ratio: number): string {
    const b = this.hexToRgb(base);
    const m = this.hexToRgb(mixWith);
    if (!b || !m) return base;
    const r = Math.round(b.r * (1 - ratio) + m.r * ratio);
    const g = Math.round(b.g * (1 - ratio) + m.g * ratio);
    const bl = Math.round(b.b * (1 - ratio) + m.b * ratio);
    return `#${[r, g, bl].map(v => v.toString(16).padStart(2, '0')).join('')}`;
  }

  private hexToRgba(hex: string, alpha: number): string {
    const rgb = this.hexToRgb(hex);
    if (!rgb) return 'rgba(91,106,240,0.14)';
    return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
  }

  private hexToRgb(hex: string): { r: number; g: number; b: number } | null {
    const clean = hex.replace('#', '').trim();
    const full = clean.length === 3
      ? clean.split('').map(c => c + c).join('')
      : clean;
    if (!/^[0-9a-fA-F]{6}$/.test(full)) return null;
    const n = parseInt(full, 16);
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 };
  }

}
