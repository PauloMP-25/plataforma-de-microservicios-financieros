import { Component, computed, inject, signal } from '@angular/core';
import { AvatarConfig, AvatarService, AuthService, ClientePerfilService } from '../../../core/services';
import { AvatarDisplay } from './components/avatar-display/avatar-display';
import { AvatarSelector } from './components/avatar-selector/avatar-selector';
import { RespuestaDatosPersonales } from '../../../core/models';
import { SolicitudCambioPassword } from '../../../core/models/auth/user.model';

interface ActividadReciente {
  titulo: string;
  detalle: string;
  fecha: string;
}

interface PaisCatalogo {
  codigo: string;
  nombre: string;
  banderaClase: string;
  prefijo: string;
  ciudades: string[];
}

interface PerfilForm {
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  dni: string;
  edad: string;
  correo: string;
  telefonoCodigoPais: string;
  telefonoNumero: string;
  pais: string;
  ciudad: string;
  genero: string;
}

type PerfilFormKey = keyof PerfilForm;

@Component({
  selector: 'app-perfil-cliente',
  standalone: true,
  imports: [AvatarDisplay, AvatarSelector],
  templateUrl: './perfil-cliente.html',
  styleUrl: './perfil-cliente.scss',
})
export class PerfilCliente {
  private readonly avatarService = inject(AvatarService);
  private readonly clientePerfilService = inject(ClientePerfilService);
  private readonly authService = inject(AuthService);

  readonly paisesCatalogo: PaisCatalogo[] = [
    { codigo: 'PE', nombre: 'Perú', banderaClase: 'flag flag--pe', prefijo: '+51', ciudades: ['Lima', 'Ica', 'Arequipa', 'Cusco', 'Trujillo', 'Piura'] },
    { codigo: 'CL', nombre: 'Chile', banderaClase: 'flag flag--cl', prefijo: '+56', ciudades: ['Santiago', 'Valparaíso', 'Concepción'] },
    { codigo: 'CO', nombre: 'Colombia', banderaClase: 'flag flag--co', prefijo: '+57', ciudades: ['Bogotá', 'Medellín', 'Cali'] },
    { codigo: 'AR', nombre: 'Argentina', banderaClase: 'flag flag--ar', prefijo: '+54', ciudades: ['Buenos Aires', 'Córdoba', 'Rosario'] },
    { codigo: 'MX', nombre: 'México', banderaClase: 'flag flag--mx', prefijo: '+52', ciudades: ['CDMX', 'Guadalajara', 'Monterrey'] },
    { codigo: 'US', nombre: 'Estados Unidos', banderaClase: 'flag flag--us', prefijo: '+1', ciudades: ['Miami', 'New York', 'Los Angeles'] },
  ];

  readonly generosCatalogo = ['Masculino', 'Femenino', 'Otro', 'Prefiero no decirlo'];
  readonly mesesCatalogo = [
    { value: 0, label: 'Enero' },
    { value: 1, label: 'Febrero' },
    { value: 2, label: 'Marzo' },
    { value: 3, label: 'Abril' },
    { value: 4, label: 'Mayo' },
    { value: 5, label: 'Junio' },
    { value: 6, label: 'Julio' },
    { value: 7, label: 'Agosto' },
    { value: 8, label: 'Septiembre' },
    { value: 9, label: 'Octubre' },
    { value: 10, label: 'Noviembre' },
    { value: 11, label: 'Diciembre' }
  ];

  loading = signal(true);
  modalAbierto = signal(false);
  mensajeExito = signal('');
  mensajeError = signal('');
  perfil = signal<RespuestaDatosPersonales | null>(null);
  guardandoPerfil = signal(false);
  guardandoPassword = signal(false);
  mostrarPasswordActual = signal(false);
  mostrarPasswordNueva = signal(false);
  mostrarPasswordConfirmar = signal(false);

  form = signal<PerfilForm>({
    nombres: '',
    apellidos: '',
    fechaNacimiento: '',
    dni: '',
    edad: '',
    correo: '',
    telefonoCodigoPais: '+51',
    telefonoNumero: '',
    pais: 'PE',
    ciudad: '',
    genero: ''
  });

  formOriginal = signal<PerfilForm | null>(null);
  errores = signal<Partial<Record<PerfilFormKey, string>>>({});

  cambioPassword = signal<SolicitudCambioPassword>({
    passwordActual: '',
    nuevoPassword: '',
    confirmarPassword: ''
  });

  avatarConfig = computed(() => this.avatarService.avatarConfig());
  avatarConfigActual = signal<AvatarConfig>(this.avatarService.avatarConfig());
  usuarioSesion = computed(() => this.authService.usuario());

  actividadesRecientes = signal<ActividadReciente[]>([
    { titulo: 'Actualización de perfil', detalle: 'Se editaron datos personales', fecha: 'Hace 2 días' },
    { titulo: 'Cambio de avatar', detalle: 'Avatar personalizado actualizado', fecha: 'Hace 5 días' },
    { titulo: 'Inicio de sesión', detalle: 'Acceso desde navegador web', fecha: 'Hoy' }
  ]);

  readonly nombreMostrado = computed(() => {
    const f = this.form();
    const full = `${f.nombres} ${f.apellidos}`.trim();
    return full || 'Usuario Luka';
  });

  readonly estadoVerificacion = computed(() => this.form().correo ? 'Verificación pendiente' : 'Sin verificar');

  readonly resumenCuenta = computed(() => {
    const p = this.perfil();
    return {
      estadoPerfil: p?.datosCompletos ? 'Perfil completo' : 'Perfil pendiente',
      ultimaActualizacion: this.formatearFecha(p?.fechaActualizacion)
    };
  });

  readonly miembroDesde = computed(() => this.formatearFecha(this.perfil()?.fechaCreacion));
  readonly estadoActividad = computed(() => this.usuarioSesion() ? 'En sesión' : 'Sin sesión');

  readonly ciudadesDisponibles = computed(() => {
    const pais = this.paisesCatalogo.find(p => p.codigo === this.form().pais);
    return pais?.ciudades ?? [];
  });

  readonly selectedPais = computed(() => this.paisesCatalogo.find(p => p.codigo === this.form().pais) ?? this.paisesCatalogo[0]);

  readonly fortalezaPassword = computed(() => {
    const value = this.cambioPassword().nuevoPassword ?? '';
    let percent = 10;
    let label = 'Débil';
    if (value.length >= 8) percent = 45;
    if (/[A-Z]/.test(value) && /\d/.test(value)) percent = 70;
    if (value.length >= 10 && /[^A-Za-z0-9]/.test(value)) {
      percent = 100;
      label = 'Fuerte';
    } else if (percent >= 70) {
      label = 'Media';
    }
    return { percent, label };
  });

  readonly fechaPartes = computed(() => {
    const raw = this.form().fechaNacimiento;
    if (!raw) return { dia: '', mes: '', anio: '' };
    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) return { dia: '', mes: '', anio: '' };
    return {
      dia: String(parsed.getDate()),
      mes: String(parsed.getMonth()),
      anio: String(parsed.getFullYear())
    };
  });

  readonly aniosNacimiento = computed(() => {
    const current = new Date().getFullYear();
    return Array.from({ length: 100 }, (_, i) => String(current - i));
  });

  constructor() {
    this.cargarDatosPerfil();
  }

  abrirModalAvatar(): void {
    this.avatarConfigActual.set(this.avatarConfig());
    this.modalAbierto.set(true);
  }

  cerrarModalAvatar(): void {
    this.modalAbierto.set(false);
  }

  guardarAvatar(config: AvatarConfig): void {
    this.avatarService.setAvatar(config);
    this.avatarConfigActual.set(config);
    this.cerrarModalAvatar();
    this.mensajeExito.set('Cambios guardados correctamente.');
    setTimeout(() => this.mensajeExito.set(''), 2500);
  }

  actualizarPreviewAvatar(config: AvatarConfig): void {
    this.avatarConfigActual.set(config);
  }

  actualizarCampo(campo: PerfilFormKey, valor: string): void {
    this.form.update(state => ({ ...state, [campo]: valor }));
    this.errores.update(e => ({ ...e, [campo]: undefined }));

    if (campo === 'pais') {
      const pais = this.paisesCatalogo.find(p => p.codigo === valor);
      this.form.update(state => ({
        ...state,
        telefonoCodigoPais: pais?.prefijo ?? state.telefonoCodigoPais,
        ciudad: ''
      }));
    }
  }

  actualizarCampoPassword(campo: keyof SolicitudCambioPassword, valor: string): void {
    this.cambioPassword.update(state => ({ ...state, [campo]: valor }));
  }

  togglePassword(campo: 'actual' | 'nueva' | 'confirmar'): void {
    if (campo === 'actual') this.mostrarPasswordActual.update(v => !v);
    if (campo === 'nueva') this.mostrarPasswordNueva.update(v => !v);
    if (campo === 'confirmar') this.mostrarPasswordConfirmar.update(v => !v);
  }

  actualizarFechaNacimientoParte(parte: 'dia' | 'mes' | 'anio', valor: string): void {
    const partes = this.fechaPartes();
    const dia = parte === 'dia' ? valor : partes.dia;
    const mes = parte === 'mes' ? valor : partes.mes;
    const anio = parte === 'anio' ? valor : partes.anio;

    if (!dia || !mes || !anio) return;

    const d = Number(dia);
    const m = Number(mes);
    const y = Number(anio);
    const fecha = new Date(y, m, d);
    if (Number.isNaN(fecha.getTime())) return;

    const edad = this.calcularEdadDesdeFecha(fecha);
    const iso = `${String(y).padStart(4, '0')}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

    this.form.update(state => ({ ...state, fechaNacimiento: iso, edad: String(edad) }));
  }

  cancelarCambiosPerfil(): void {
    const original = this.formOriginal();
    if (!original) return;
    this.form.set({ ...original });
    this.errores.set({});
    this.mensajeError.set('');
  }

  guardarDatosPerfil(): void {
    const usuarioId = this.authService.usuario()?.id;
    const p = this.perfil();
    if (!usuarioId || !p) return;

    if (!this.validarFormulario()) {
      this.mensajeError.set('Corrige los campos marcados antes de guardar.');
      return;
    }

    this.mensajeError.set('');
    this.guardandoPerfil.set(true);

    const f = this.form();
    const telefonoCompleto = `${f.telefonoCodigoPais}${f.telefonoNumero}`.trim();

    this.clientePerfilService.actualizarPerfil(usuarioId, {
      dni: f.dni || p.dni,
      nombres: f.nombres,
      apellidos: f.apellidos,
      genero: f.genero,
      edad: Number(f.edad || p.edad || 0),
      telefono: telefonoCompleto,
      fotoPerfilUrl: p.fotoPerfilUrl,
      direccion: p.direccion,
      ciudad: f.ciudad,
    }).subscribe({
      next: (perfilActualizado) => {
        this.perfil.set(perfilActualizado);
        this.hidratarFormularioDesdePerfil(perfilActualizado);
        this.guardandoPerfil.set(false);
        this.mensajeExito.set('Datos personales actualizados correctamente.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
      error: () => {
        this.guardandoPerfil.set(false);
        this.mensajeError.set('No se pudo guardar. Inténtalo nuevamente.');
      }
    });
  }

  guardarPassword(): void {
    if (this.cambioPassword().nuevoPassword !== this.cambioPassword().confirmarPassword) {
      this.mensajeError.set('La confirmación de contraseña no coincide.');
      return;
    }

    this.mensajeError.set('');
    this.guardandoPassword.set(true);
    this.authService.cambiarPassword(this.cambioPassword()).subscribe({
      next: () => {
        this.guardandoPassword.set(false);
        this.cambioPassword.set({ passwordActual: '', nuevoPassword: '', confirmarPassword: '' });
        this.mensajeExito.set('Contraseña actualizada correctamente.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
      error: () => {
        this.guardandoPassword.set(false);
        this.mensajeError.set('No se pudo actualizar la contraseña.');
      }
    });
  }

  ejecutarOpcionRapida(opcion: 'eliminar'): void {
    if (opcion === 'eliminar') {
      this.mensajeExito.set('Solicitud de eliminación registrada. Contacta soporte para completar el proceso.');
      setTimeout(() => this.mensajeExito.set(''), 3500);
    }
  }

  private cargarDatosPerfil(): void {
    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.clientePerfilService.obtenerPerfil(usuarioId).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.hidratarFormularioDesdePerfil(perfil);
        this.loading.set(false);
      },
      error: () => {
        this.perfil.set(null);
        this.loading.set(false);
      },
    });
  }

  private hidratarFormularioDesdePerfil(perfil: RespuestaDatosPersonales): void {
    const correo = this.usuarioSesion()?.nombreUsuario ?? '';
    const pais = this.paisDesdeTelefono(perfil.telefono) ?? 'PE';
    const prefijo = this.paisesCatalogo.find(x => x.codigo === pais)?.prefijo ?? '+51';
    const numero = this.numeroSinPrefijo(perfil.telefono, prefijo);

    const nextForm: PerfilForm = {
      nombres: perfil.nombres ?? '',
      apellidos: perfil.apellidos ?? '',
      fechaNacimiento: '',
      dni: perfil.dni ?? '',
      edad: String(perfil.edad ?? ''),
      correo,
      telefonoCodigoPais: prefijo,
      telefonoNumero: numero,
      pais,
      ciudad: perfil.ciudad ?? '',
      genero: perfil.genero ?? ''
    };

    this.form.set(nextForm);
    this.formOriginal.set({ ...nextForm });
  }

  private validarFormulario(): boolean {
    const f = this.form();
    const errores: Partial<Record<PerfilFormKey, string>> = {};

    if (!f.nombres.trim()) errores.nombres = 'Nombres es obligatorio.';
    if (!f.apellidos.trim()) errores.apellidos = 'Apellidos es obligatorio.';
    if (!/^\d+$/.test(f.dni.trim())) errores.dni = 'DNI debe contener solo números.';
    if (!/^\S+@\S+\.\S+$/.test(f.correo.trim())) errores.correo = 'Correo no tiene formato válido.';

    if (f.telefonoNumero.trim()) {
      if (!/^\d{6,12}$/.test(f.telefonoNumero.trim())) {
        errores.telefonoNumero = 'Teléfono inválido para el país seleccionado.';
      }
    }

    if (f.pais && !f.ciudad) errores.ciudad = 'Ciudad es obligatoria si seleccionas país.';
    if (!f.genero) errores.genero = 'Selecciona un género.';

    this.errores.set(errores);
    return Object.keys(errores).length === 0;
  }

  private paisDesdeTelefono(telefono?: string): string | null {
    if (!telefono) return null;
    const pref = this.paisesCatalogo.find(p => telefono.startsWith(p.prefijo));
    return pref?.codigo ?? null;
  }

  private numeroSinPrefijo(telefono: string | undefined, prefijo: string): string {
    if (!telefono) return '';
    return telefono.startsWith(prefijo) ? telefono.slice(prefijo.length).trim() : telefono;
  }

  private calcularEdadDesdeFecha(fecha: Date): number {
    const hoy = new Date();
    let edad = hoy.getFullYear() - fecha.getFullYear();
    const m = hoy.getMonth() - fecha.getMonth();
    if (m < 0 || (m === 0 && hoy.getDate() < fecha.getDate())) edad--;
    return Math.max(edad, 0);
  }

  private formatearFecha(fecha?: string): string {
    if (!fecha) return 'No especificado';
    const parsed = new Date(fecha);
    if (Number.isNaN(parsed.getTime())) return fecha;
    return new Intl.DateTimeFormat('es-PE', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
    }).format(parsed);
  }
}
