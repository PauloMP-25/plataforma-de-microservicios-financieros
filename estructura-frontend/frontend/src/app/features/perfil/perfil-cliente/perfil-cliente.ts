import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AvatarConfig, AvatarService, AuthService, ClientePerfilService } from '../../../core/services';
import { AvatarDisplay } from './components/avatar-display/avatar-display';
import { AvatarSelector } from './components/avatar-selector/avatar-selector';
import { RespuestaDatosPersonales } from '../../../core/models';
import { SolicitudCambioPassword } from '../../../core/models/auth/user.model';
import { PerfilDatosPersonales } from './components/perfil-datos-personales/perfil-datos-personales';
import { PerfilSeguridad } from './components/perfil-seguridad/perfil-seguridad';

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
  imports: [AvatarDisplay, AvatarSelector, FormsModule, PerfilDatosPersonales, PerfilSeguridad],
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

  fechaDia = signal<string>('');
  fechaMes = signal<string>('');
  fechaAnio = signal<string>('');

  cambioPassword = signal<SolicitudCambioPassword>({
    passwordActual: '',
    nuevoPassword: '',
    confirmarPassword: ''
  });

  avatarConfig = computed(() => this.avatarService.avatarConfig());
  avatarConfigActual = signal<AvatarConfig>(this.avatarService.avatarConfig());
  usuarioSesion = computed(() => this.authService.usuario());

  readonly correoUsuario = computed(() => {
    const token = this.authService.usuario()?.token;
    if (!token) return '';
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      return decoded.sub || '';
    } catch {
      return '';
    }
  });

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

  get longitudMaximaTelefono(): number {
    const pref = this.form().telefonoCodigoPais;
    const longitudesMinimasPorPrefijo: Record<string, number> = {
      '+51': 9,
      '+56': 9,
      '+57': 10,
      '+54': 10,
      '+52': 10,
      '+1': 10
    };
    return longitudesMinimasPorPrefijo[pref] ?? 9;
  }

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
    return {
      dia: this.fechaDia(),
      mes: this.fechaMes(),
      anio: this.fechaAnio()
    };
  });

  readonly aniosNacimiento = computed(() => {
    const current = new Date().getFullYear();
    return Array.from({ length: 100 }, (_, i) => String(current - i));
  });

  readonly diasNacimiento = computed(() => {
    const mes = Number(this.fechaMes());
    const anio = Number(this.fechaAnio()) || new Date().getFullYear();
    const mesValido = Number.isInteger(mes) && mes >= 0 && mes <= 11;
    const diasDelMes = mesValido ? new Date(anio, mes + 1, 0).getDate() : 31;
    return Array.from({ length: diasDelMes }, (_, i) => i + 1);
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

    const usuarioId = this.authService.usuario()?.id;
    const p = this.perfil();
    if (usuarioId && p) {
      const f = this.form();
      const telefonoCompleto = `${f.telefonoCodigoPais}${f.telefonoNumero}`.trim();
      const avatarUrl = `/assets/avatares/figuras/${encodeURIComponent(config.figura)}.png?accesorio=${encodeURIComponent(config.accesorio || '')}`;

      const generoMapa: Record<string, string> = {
        'Masculino': 'MASCULINO',
        'Femenino': 'FEMENINO',
        'Otro': 'OTRO',
        'Prefiero no decirlo': 'PREFIERO_NO_DECIR'
      };
      const generoBackend = generoMapa[f.genero] || f.genero;

      this.clientePerfilService.actualizarPerfil(usuarioId, {
        dni: f.dni || p.dni,
        nombres: f.nombres,
        apellidos: f.apellidos,
        genero: generoBackend,
        edad: Number(f.edad || p.edad || 0),
        telefono: telefonoCompleto,
        fotoPerfilUrl: avatarUrl,
        pais: f.pais,
        ciudad: f.ciudad,
      }).subscribe({
        next: (perfilActualizado) => {
          this.perfil.set(perfilActualizado);
          this.hidratarFormularioDesdePerfil(perfilActualizado);
        }
      });
    }

    this.cerrarModalAvatar();
    this.mensajeExito.set('Cambios guardados correctamente.');
    setTimeout(() => this.mensajeExito.set(''), 2500);
  }

  actualizarPreviewAvatar(config: AvatarConfig): void {
    this.avatarConfigActual.set(config);
  }

  actualizarCampo(campo: PerfilFormKey, valor: string): void {
    let valorSaneado = valor;
    if (campo === 'dni') {
      valorSaneado = valor.replace(/\D/g, '').slice(0, 8);
    }
    if (campo === 'telefonoNumero') {
      const limit = this.longitudMaximaTelefono;
      valorSaneado = valor.replace(/\D/g, '').slice(0, limit);
    }

    this.form.update(state => ({ ...state, [campo]: valorSaneado }));
    this.errores.update(e => ({ ...e, [campo]: undefined }));

    if (campo === 'pais') {
      const pais = this.paisesCatalogo.find(p => p.codigo === valorSaneado);
      const prefijo = pais?.prefijo ?? this.form().telefonoCodigoPais;
      const longitudesMinimasPorPrefijo: Record<string, number> = {
        '+51': 9,
        '+56': 9,
        '+57': 10,
        '+54': 10,
        '+52': 10,
        '+1': 10
      };
      const limit = longitudesMinimasPorPrefijo[prefijo] ?? 9;
      this.form.update(state => ({
        ...state,
        telefonoCodigoPais: prefijo,
        telefonoNumero: state.telefonoNumero.slice(0, limit),
        ciudad: ''
      }));
    }
  }

  filtrarTeclasNumericas(event: KeyboardEvent): boolean {
    const charCode = event.key;
    if (charCode >= '0' && charCode <= '9') {
      return true;
    }
    // Permitir teclas especiales del sistema
    if (['Backspace', 'Tab', 'Delete', 'ArrowLeft', 'ArrowRight', 'Home', 'End', 'Enter'].includes(event.code)) {
      return true;
    }
    event.preventDefault();
    return false;
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
    if (parte === 'dia') this.fechaDia.set(valor);
    if (parte === 'mes') this.fechaMes.set(valor);
    if (parte === 'anio') this.fechaAnio.set(valor);

    this.ajustarDiaNacimientoAlMes();

    const dia = this.fechaDia();
    const mes = this.fechaMes();
    const anio = this.fechaAnio();

    if (!dia || !mes || !anio) return;

    const d = Number(dia);
    const m = Number(mes);
    const y = Number(anio);
    const fecha = new Date(y, m, d);
    if (
      Number.isNaN(fecha.getTime()) ||
      fecha.getFullYear() !== y ||
      fecha.getMonth() !== m ||
      fecha.getDate() !== d
    ) return;

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

    if (original.fechaNacimiento) {
      const parsed = this.parseFechaNacimiento(original.fechaNacimiento);
      if (parsed) {
        this.fechaDia.set(String(parsed.dia));
        this.fechaMes.set(String(parsed.mes));
        this.fechaAnio.set(String(parsed.anio));
        return;
      }
    }
    this.fechaDia.set('');
    this.fechaMes.set('');
    this.fechaAnio.set('');
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
    const currentAvatar = this.avatarService.avatarConfig();
    const avatarUrl = `/assets/avatares/figuras/${encodeURIComponent(currentAvatar.figura)}.png?accesorio=${encodeURIComponent(currentAvatar.accesorio || '')}`;

    const generoMapa: Record<string, string> = {
      'Masculino': 'MASCULINO',
      'Femenino': 'FEMENINO',
      'Otro': 'OTRO',
      'Prefiero no decirlo': 'PREFIERO_NO_DECIR'
    };
    const generoBackend = generoMapa[f.genero] || f.genero;

    this.clientePerfilService.actualizarPerfil(usuarioId, {
      dni: f.dni || p.dni,
      nombres: f.nombres,
      apellidos: f.apellidos,
      genero: generoBackend,
      edad: Number(f.edad || p.edad || 0),
      telefono: telefonoCompleto,
      fotoPerfilUrl: avatarUrl,
      pais: f.pais,
      ciudad: f.ciudad,
    }).subscribe({
      next: (perfilActualizado) => {
        this.perfil.set(perfilActualizado);
        this.hidratarFormularioDesdePerfil(perfilActualizado);
        this.guardandoPerfil.set(false);
        this.mensajeExito.set('Datos personales actualizados correctamente.');
        setTimeout(() => this.mensajeExito.set(''), 2500);
      },
      error: (err: any) => {
        this.guardandoPerfil.set(false);
        let mensaje = 'No se pudo guardar los cambios. Intente nuevamente.';
        if (err?.error?.mensaje) {
          mensaje = err.error.mensaje;
          if (err.error.detalles && err.error.detalles.length > 0) {
            mensaje += ' Detalles: ' + err.error.detalles.join(', ');
          }
        }
        this.mensajeError.set(mensaje);
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

  // --- Bridges para @Output de componentes hijos ---

  onCampoChange(event: { campo: string; valor: string }): void {
    this.actualizarCampo(event.campo as any, event.valor);
  }

  onFechaParteChange(event: { parte: 'dia' | 'mes' | 'anio'; valor: string }): void {
    this.actualizarFechaNacimientoParte(event.parte, event.valor);
  }

  onCampoPasswordChange(event: { campo: keyof SolicitudCambioPassword; valor: string }): void {
    this.actualizarCampoPassword(event.campo, event.valor);
  }

  onTogglePasswordVisibility(campo: 'actual' | 'nueva' | 'confirmar'): void {
    this.togglePassword(campo);
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
    const correo = this.correoUsuario();
    const pais = perfil.pais ?? 'PE';
    const prefijo = this.paisesCatalogo.find(x => x.codigo === pais)?.prefijo ?? '+51';
    const numero = this.numeroSinPrefijo(perfil.telefono, prefijo);

    if (perfil.fotoPerfilUrl) {
      try {
        const config = JSON.parse(perfil.fotoPerfilUrl);
        if (config && config.figura) {
          this.avatarService.setAvatar(config);
        }
      } catch {
        // Ignorar si no es una cadena JSON válida
      }
    }

    const generoMapaInv: Record<string, string> = {
      'MASCULINO': 'Masculino',
      'FEMENINO': 'Femenino',
      'OTRO': 'Otro',
      'PREFIERO_NO_DECIR': 'Prefiero no decirlo'
    };
    const generoFrontend = generoMapaInv[perfil.genero] || perfil.genero || '';

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
      genero: generoFrontend
    };

    this.fechaDia.set('');
    this.fechaMes.set('');
    this.fechaAnio.set('');

    this.form.set(nextForm);
    this.formOriginal.set({ ...nextForm });
  }

  private validarFormulario(): boolean {
    const f = this.form();
    const errores: Partial<Record<PerfilFormKey, string>> = {};

    if (!f.nombres.trim()) errores.nombres = 'Nombres es obligatorio.';
    if (!f.apellidos.trim()) errores.apellidos = 'Apellidos es obligatorio.';

    // DNI: exactamente 8 dígitos numéricos
    if (!/^\d{8}$/.test(f.dni.trim())) {
      errores.dni = 'DNI debe tener exactamente 8 dígitos numéricos.';
    }

    if (!/^\S+@\S+\.\S+$/.test(f.correo.trim())) errores.correo = 'Correo no tiene formato válido.';

    // Teléfono: solo números, con validación por país (mínimo longitud por prefijo si existe, si no aplicar mínimo 9 dígitos)
    const telTrimmed = f.telefonoNumero.trim();
    if (telTrimmed) {
      if (!/^\d+$/.test(telTrimmed)) {
        errores.telefonoNumero = 'Teléfono debe contener solo números.';
      } else {
        const longitudesMinimasPorPrefijo: Record<string, number> = {
          '+51': 9,
          '+56': 9,
          '+57': 10,
          '+54': 10,
          '+52': 10,
          '+1': 10
        };
        const minLongitud = longitudesMinimasPorPrefijo[f.telefonoCodigoPais] ?? 9;
        if (telTrimmed.length < minLongitud) {
          errores.telefonoNumero = `Teléfono debe tener al menos ${minLongitud} dígitos para el país seleccionado.`;
        }
      }
    }

    // Edad: solo números, rango 0–120
    const edadTrimmed = f.edad ? String(f.edad).trim() : '';
    if (!edadTrimmed) {
      errores.edad = 'Edad es obligatoria.';
    } else if (!/^\d+$/.test(edadTrimmed)) {
      errores.edad = 'Edad debe contener solo números.';
    } else {
      const edadNum = Number(edadTrimmed);
      if (edadNum < 0 || edadNum > 120) {
        errores.edad = 'Edad debe estar entre 0 y 120 años.';
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

  private ajustarDiaNacimientoAlMes(): void {
    const dia = Number(this.fechaDia());
    if (!dia) return;

    const maxDia = this.diasNacimiento().length;
    if (dia > maxDia) {
      this.fechaDia.set(String(maxDia));
    }
  }

  private parseFechaNacimiento(fecha: string): { dia: number; mes: number; anio: number } | null {
    const matchIso = fecha.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (matchIso) {
      const anio = Number(matchIso[1]);
      const mes = Number(matchIso[2]) - 1;
      const dia = Number(matchIso[3]);
      const parsed = new Date(anio, mes, dia);
      if (parsed.getFullYear() === anio && parsed.getMonth() === mes && parsed.getDate() === dia) {
        return { dia, mes, anio };
      }
    }

    const parsed = new Date(fecha);
    if (Number.isNaN(parsed.getTime())) return null;
    return {
      dia: parsed.getDate(),
      mes: parsed.getMonth(),
      anio: parsed.getFullYear()
    };
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
