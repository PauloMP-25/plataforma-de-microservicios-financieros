import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ClientePerfilService } from '../../../core/services/cliente-perfil.service';
import { RespuestaDatosPersonales, SolicitudDatosPersonales } from '../../../core/models/cliente/perfil-cliente.model';
import { AvatarService } from '../../../core/services/avatar.service';

@Component({
  selector: 'app-configuracion',
  standalone:true,
  imports: [CommonModule],
  templateUrl: './configuracion.html',
  styleUrls: ['./configuracion.scss'],
})
export class Configuracion {
  // Servicios usados:
  readonly authService = inject(AuthService);
  readonly avatarService = inject(AvatarService);
  private readonly clientePerfilService = inject(ClientePerfilService);

  readonly tema = signal<'oscuro' | 'claro'>('claro');
  readonly colorPrincipal = signal<string>('#6d4aff');

  readonly perfil = signal<RespuestaDatosPersonales | null>(null);
  readonly modalEditarPerfilAbierto = signal(false);
  readonly guardandoPerfil = signal(false);
  readonly mensajePerfil = signal('');

  readonly formNombres = signal('');
  readonly formApellidos = signal('');
  readonly formTelefono = signal('');
  readonly formDireccion = signal('');
  readonly formCiudad = signal('');

  readonly coloresDisponibles = ['#6d4aff', '#4361ee', '#12b3a6', '#db2777', '#ea580c'];
  private readonly THEME_KEY = 'luka_theme';
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

  abrirModalEditarPerfil(): void {
    // Pre-carga el formulario del modal con el snapshot actual del perfil.
    const p = this.perfil();
    if (p) {
      this.formNombres.set(p.nombres ?? '');
      this.formApellidos.set(p.apellidos ?? '');
      this.formTelefono.set(p.telefono ?? '');
      this.formDireccion.set(p.direccion ?? '');
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
      direccion: this.formDireccion().trim(),
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
    // Mantiene preferencia persistida; por defecto inicia en claro.
    const guardado = localStorage.getItem(this.THEME_KEY);
    const temaInicial: 'oscuro' | 'claro' = guardado === 'oscuro' ? 'oscuro' : 'claro';
    this.tema.set(temaInicial);
    this.aplicarTemaGlobal(temaInicial);
  }

  private aplicarTemaGlobal(tema: 'oscuro' | 'claro'): void {
    // Punto único de verdad para el tema global vía clase en body.
    document.body.classList.toggle('theme-dark', tema === 'oscuro');
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
    localStorage.setItem(this.ACCENT_KEY, color);
  }

}
