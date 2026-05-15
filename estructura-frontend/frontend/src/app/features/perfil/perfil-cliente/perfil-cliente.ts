import { Component, computed, inject, signal } from '@angular/core';
import { AvatarConfig, AvatarService } from '../../../core/services';
import { AvatarDisplay } from './components/avatar-display/avatar-display';
import { AvatarSelector } from './components/avatar-selector/avatar-selector';
import { ClientePerfilService } from '../../../core/services';
import { AuthService } from '../../../core/services';
import { RespuestaDatosPersonales } from '../../../core/models';

type FiltroInsignias = 'TODOS' | 'AHORROS' | 'GASTOS' | 'METAS' | 'ESPECIALIDAD';

interface CampoBasico {
  label: string;
  value: string;
}

interface Insignia {
  titulo: string;
  descripcion: string;
  categoria: Exclude<FiltroInsignias, 'TODOS'>;
  completado: boolean;
  imagen: string;
}

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [AvatarDisplay, AvatarSelector],
  templateUrl: './perfil-cliente.html',
  styleUrl: './perfil-cliente.scss',
})
export class PerfilCliente {


  // Servicios principales 
  private readonly avatarService = inject(AvatarService);
  private readonly clientePerfilService = inject(ClientePerfilService);
  private readonly authService = inject(AuthService);


  //Componentes temporales 
  loading = signal(true);
  modalAbierto = signal(false);
  mensajeExito = signal('');
  perfil = signal<RespuestaDatosPersonales | null>(null);

  //Lo que será automatico 
   avatarConfig = computed(() => this.avatarService.avatarConfig());
   usuarioSesion = computed(() => this.authService.usuario());

   filtroActivo = signal<FiltroInsignias>('TODOS');
   filtrosInsignias: FiltroInsignias[] = ['TODOS', 'AHORROS', 'GASTOS', 'METAS', 'ESPECIALIDAD'];

  readonly insignias = signal<Insignia[]>([]);

  // Vista de insignias filtradas por categoría.
  readonly insigniasFiltradas = computed(() => {
    const filtro = this.filtroActivo();
    const lista = this.insignias();
    if (filtro === 'TODOS') {
      return lista;
    }
    return lista.filter((insignia) => insignia.categoria === filtro);
  });


  // Combinación de datos del perfil backend + correo de sesión.
  readonly informacionBasica = computed<CampoBasico[]>(() => {
    const perfil = this.perfil();
    const usuario = this.usuarioSesion();

    const nombreCompleto = `${perfil?.nombres ?? ''} ${perfil?.apellidos ?? ''}`.trim();
    const miembroDesde = this.formatearFecha(perfil?.fechaCreacion);
    const ultimaActualizacion = this.formatearFecha(perfil?.fechaActualizacion);

    return [
      { label: 'Nombre completo', value: this.formatearValor(nombreCompleto) },
      { label: 'Email', value: this.formatearValor(usuario?.nombreUsuario) },
      { label: 'Teléfono', value: this.formatearValor(perfil?.telefono) },
      {
        label: 'Miembro desde',
        value: this.formatearValor(miembroDesde),
      },
      {
        label: 'Última actualización',
        value: this.formatearValor(ultimaActualizacion),
      },
      {
        label: 'Estado del perfil',
        value: perfil?.datosCompletos ? 'Completo' : 'Pendiente de completar',
      },
    ];
  });

  constructor() {
    this.cargarDatosPerfil();
  }

  guardarAvatar(config: AvatarConfig): void {
    // Guarda avatar en storage local (no backend) y notifica éxito breve.
    this.avatarService.setAvatar(config);
    this.cerrarModalAvatar();
    this.mensajeExito.set('Cambios guardados correctamente.');
    setTimeout(() => this.mensajeExito.set(''), 2500);
  }

  abrirModalAvatar(): void {
    this.modalAbierto.set(true);
  }

  cerrarModalAvatar(): void {
    this.modalAbierto.set(false);
  }

  cambiarFiltroInsignias(filtro: FiltroInsignias): void {
    this.filtroActivo.set(filtro);
  }

  private cargarDatosPerfil(): void {
    // Si no hay sesión válida, no intenta consultar backend.
    const usuarioId = this.authService.usuario()?.id;

    if (!usuarioId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.clientePerfilService.obtenerPerfil(usuarioId).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.loading.set(false);
      },
      error: () => {
        this.perfil.set(null);
        this.loading.set(false);
      },
    });
  }

  private formatearValor(value: unknown): string {
    if (value === null || value === undefined || value === '') {
      return 'No especificado';
    }
    return String(value);
  }

  private formatearFecha(fecha?: string): string {
    // Formato amigable para UI en locale es-PE.
    if (!fecha) {
      return 'No especificado';
    }

    const fechaParseada = new Date(fecha);
    if (Number.isNaN(fechaParseada.getTime())) {
      return fecha;
    }

    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    }).format(fechaParseada);
  }
}
