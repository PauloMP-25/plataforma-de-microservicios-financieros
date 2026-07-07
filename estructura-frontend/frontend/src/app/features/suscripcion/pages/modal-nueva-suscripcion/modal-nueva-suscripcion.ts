import { Component, signal, computed, EventEmitter, Output, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  CATEGORIAS_SUSCRIPCION,
  FRECUENCIAS_SUSCRIPCION,
  CrearSuscripcionRequest,
  ActualizarSuscripcionRequest,
  SuscripcionDTO,
  findPlatform
} from '../../../../core/models/financiero/suscripcion-gasto.model';
export interface PlataformaSuscripcion {
  nombre: string;
  categoria: string;
  icono: string;
  color: string;
}

export const PLATAFORMAS_SUSCRIPCION: PlataformaSuscripcion[] = [
  // Entretenimiento
  { nombre: 'Netflix', categoria: 'leisure', icono: 'fa-solid fa-film', color: '#e50914' },
  { nombre: 'Spotify', categoria: 'leisure', icono: 'fa-brands fa-spotify', color: '#1db954' },
  { nombre: 'YouTube Premium', categoria: 'leisure', icono: 'fa-brands fa-youtube', color: '#ff0000' },
  { nombre: 'Amazon Prime', categoria: 'leisure', icono: 'fa-brands fa-amazon', color: '#ff9900' },
  { nombre: 'Disney+', categoria: 'leisure', icono: 'fa-solid fa-play', color: '#113CCF' },
  { nombre: 'HBO Max', categoria: 'leisure', icono: 'fa-solid fa-tv', color: '#441864' },
  { nombre: 'Crunchyroll', categoria: 'leisure', icono: 'fa-solid fa-film', color: '#F47521' },
  { nombre: 'Apple Music', categoria: 'leisure', icono: 'fa-brands fa-apple', color: '#fa243c' },

  // Educación & Productividad
  { nombre: 'ChatGPT Plus', categoria: 'study', icono: 'fa-solid fa-robot', color: '#10a37f' },
  { nombre: 'Canva Pro', categoria: 'study', icono: 'fa-solid fa-palette', color: '#00c4cc' },
  { nombre: 'Notion', categoria: 'study', icono: 'fa-solid fa-book-open', color: '#000000' },
  { nombre: 'Platzi', categoria: 'study', icono: 'fa-solid fa-laptop-code', color: '#98ca3f' },
  { nombre: 'Coursera', categoria: 'study', icono: 'fa-solid fa-graduation-cap', color: '#0056D2' },
  { nombre: 'Duolingo', categoria: 'study', icono: 'fa-solid fa-language', color: '#58CC02' },
  { nombre: 'Domestika', categoria: 'study', icono: 'fa-solid fa-pen-nib', color: '#e02424' },
  { nombre: 'Udemy', categoria: 'study', icono: 'fa-solid fa-chalkboard-user', color: '#a435f0' },
  { nombre: 'Adobe Creative Cloud', categoria: 'study', icono: 'fa-solid fa-bezier-curve', color: '#ff0000' },
  { nombre: 'Zoom Pro', categoria: 'study', icono: 'fa-solid fa-video', color: '#2D8CFF' },

  // Juegos
  { nombre: 'PlayStation Plus', categoria: 'leisure', icono: 'fa-brands fa-playstation', color: '#003087' },
  { nombre: 'Xbox Game Pass', categoria: 'leisure', icono: 'fa-brands fa-xbox', color: '#107c10' },
  { nombre: 'Nintendo Switch Online', categoria: 'leisure', icono: 'fa-solid fa-gamepad', color: '#E60012' },

  // Hogar / Utilidades / Telecomunicaciones
  { nombre: 'Google One', categoria: 'home', icono: 'fa-brands fa-google', color: '#4285f4' },
  { nombre: 'iCloud', categoria: 'home', icono: 'fa-brands fa-apple', color: '#000000' },
  { nombre: 'Microsoft 365', categoria: 'study', icono: 'fa-brands fa-windows', color: '#00a4ef' },
  { nombre: 'Dropbox', categoria: 'home', icono: 'fa-brands fa-dropbox', color: '#0061FE' },
  { nombre: 'OneDrive', categoria: 'home', icono: 'fa-solid fa-cloud', color: '#0078d4' },
  { nombre: 'Claro', categoria: 'home', icono: 'fa-solid fa-wifi', color: '#E11B22' },
  { nombre: 'Movistar', categoria: 'home', icono: 'fa-solid fa-wifi', color: '#019DF4' },
  { nombre: 'WOW', categoria: 'home', icono: 'fa-solid fa-wifi', color: '#6A1B9A' },
  { nombre: 'Bitel', categoria: 'home', icono: 'fa-solid fa-wifi', color: '#FFD700' },

  // Salud / Deporte
  { nombre: 'SmartFit', categoria: 'health', icono: 'fa-solid fa-dumbbell', color: '#ffc700' },
  { nombre: 'Bodytech', categoria: 'health', icono: 'fa-solid fa-weight-hanging', color: '#e60000' },
  { nombre: 'Strava', categoria: 'health', icono: 'fa-solid fa-person-running', color: '#fc4c02' },

  // Universidad / Servicios de estudio e IA
  { nombre: 'UTP Pago Mensual', categoria: 'study', icono: 'fa-solid fa-university', color: '#C8102E' },
  { nombre: 'Gemini Advanced', categoria: 'study', icono: 'fa-solid fa-sparkles', color: '#1A73E8' }
];

@Component({
  selector: 'app-modal-nueva-suscripcion',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './modal-nueva-suscripcion.html',
  styleUrl: './modal-nueva-suscripcion.scss'
})
export class ModalNuevaSuscripcion {
  @Input() suscripcionEditar: SuscripcionDTO | null = null;
  @Output() cerrar = new EventEmitter<void>();
  @Output() crear = new EventEmitter<CrearSuscripcionRequest>();
  @Output() editar = new EventEmitter<ActualizarSuscripcionRequest>();
  @Output() pagar = new EventEmitter<string>();

  // Formulario
  readonly nombre = signal('');
  readonly descripcion = signal('');
  readonly categoria = signal('');
  readonly monto = signal('');
  readonly frecuencia = signal('MENSUAL');
  readonly fechaInicio = signal(this.obtenerFechaHoy());

  // Autocompletado
  readonly mostrarSugerencias = signal(false);
  readonly sugerencias = computed(() => {
    const texto = this.nombre().toLowerCase().trim();
    if (!texto) return [];

    return PLATAFORMAS_SUSCRIPCION.filter(p =>
      p.nombre.toLowerCase().includes(texto)
    );
  });

  ngOnInit() {
    if (this.suscripcionEditar) {
      this.nombre.set(this.suscripcionEditar.nombre);
      this.descripcion.set(this.suscripcionEditar.descripcion);
      this.categoria.set(this.suscripcionEditar.categoria);
      this.monto.set(this.suscripcionEditar.monto.toString());
      this.frecuencia.set(this.suscripcionEditar.frecuencia);
      this.fechaInicio.set(this.suscripcionEditar.proximoVencimiento || this.obtenerFechaHoy());
    }
  }

  // 👇 Calculamos los días automáticamente cuando cambia fechaInicio
  readonly diasRestantes = computed(() => {
    const fechaStr = this.fechaInicio();
    if (!fechaStr) return 0;

    // Fecha actual a la medianoche para evitar desajustes de horas
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    // Separar el string (YYYY-MM-DD) evita bugs de zona horaria al crear el Date
    const partes = fechaStr.split('-');
    if (partes.length !== 3) return 0;

    // En JavaScript los meses van del 0 al 11, por eso el - 1
    const fechaPago = new Date(Number(partes[0]), Number(partes[1]) - 1, Number(partes[2]));
    fechaPago.setHours(0, 0, 0, 0);

    const diferenciaMilisegundos = fechaPago.getTime() - hoy.getTime();
    const dias = Math.ceil(diferenciaMilisegundos / (1000 * 3600 * 24));

    return dias;
  });

  // 👇 Computed signal: detecta la plataforma usando findPlatform() del catálogo
  readonly plataformaDetectada = computed(() => findPlatform(this.nombre()));

  // 👇 Computed signal para el preview de ícono — prioriza catálogo, fallback a categoría
  readonly iconoPreview = computed(() => {
    const name = this.nombre().toLowerCase().trim();

    // Buscar si coincide con alguna plataforma predefinida
    const plataforma = PLATAFORMAS_SUSCRIPCION.find(p => p.nombre.toLowerCase() === name || p.nombre.toLowerCase().includes(name));

    if (plataforma) {
      return plataforma.icono;
    }

    // Si no es una marca conocida, buscar el ícono de la categoría seleccionada
    const catId = this.categoria();
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? `fa-solid ${cat.icon}` : 'fa-solid fa-circle-question';
  });

  // 👇 Computed signal para el color de marca unificado — prioriza catálogo, fallback a categoría
  readonly colorPreview = computed(() => {
    const plataforma = this.plataformaDetectada();
    if (plataforma?.color) return plataforma.color;

    const name = this.nombre().toLowerCase().trim();
    const plataformaCatalogo = PLATAFORMAS_SUSCRIPCION.find(p => p.nombre.toLowerCase() === name || p.nombre.toLowerCase().includes(name));
    if (plataformaCatalogo) return plataformaCatalogo.color;

    const catId = this.categoria();
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? cat.color : '#5B6AF0';
  });

  // UI
  readonly guardando = signal(false);
  readonly errores = signal<Record<string, string>>({});

  // Data para selects
  readonly categorias = CATEGORIAS_SUSCRIPCION;
  readonly frecuencias = FRECUENCIAS_SUSCRIPCION.filter(f => ['MENSUAL', 'ANUAL', 'QUINCENAL'].includes(f.id));

  // Métodos de autocompletado
  seleccionarPlataforma(plataforma: PlataformaSuscripcion): void {
    this.nombre.set(plataforma.nombre);
    this.categoria.set(plataforma.categoria);
    this.mostrarSugerencias.set(false);
  }

  ocultarSugerenciasConRetraso(): void {
    setTimeout(() => {
      this.mostrarSugerencias.set(false);
    }, 200);
  }

  /**
   * Cerrar modal
   */
  cerrarModal(): void {
    this.cerrar.emit();
  }

  /**
   * Guardar nueva suscripción
   */
  guardarSuscripcion(): void {
    // Validaciones
    if (!this.validar()) {
      return;
    }

    this.guardando.set(true);

    if (this.suscripcionEditar) {
      const request: ActualizarSuscripcionRequest = {
        id: this.suscripcionEditar.id,
        nombre: this.nombre(),
        descripcion: this.descripcion(),
        categoria: this.categoria(),
        monto: parseFloat(this.monto()),
        frecuencia: this.frecuencia() as any,
        fechaInicio: this.fechaInicio()
      };
      setTimeout(() => {
        this.editar.emit(request);
        this.limpiarFormulario();
        this.guardando.set(false);
      }, 500);
    } else {
      const request: CrearSuscripcionRequest = {
        nombre: this.nombre(),
        descripcion: this.descripcion(),
        categoria: this.categoria(),
        monto: parseFloat(this.monto()),
        frecuencia: this.frecuencia() as any,
        fechaInicio: this.fechaInicio()
      };

      setTimeout(() => {
        this.crear.emit(request);
        this.limpiarFormulario();
        this.guardando.set(false);
      }, 500);
    }
  }

  registrarPagoManual(): void {
    if (this.suscripcionEditar) {
      this.pagar.emit(this.suscripcionEditar.id);
    }
  }

  /**
   * Validar formulario
   */
  private validar(): boolean {
    const nuevosErrores: Record<string, string> = {};

    if (!this.nombre().trim()) {
      nuevosErrores['nombre'] = 'El nombre es requerido';
    }
    if (!this.categoria()) {
      nuevosErrores['categoria'] = 'La categoría es requerida';
    }
    if (!this.monto() || parseFloat(this.monto()) <= 0) {
      nuevosErrores['monto'] = 'El monto debe ser mayor a 0';
    }
    if (!this.frecuencia()) {
      nuevosErrores['frecuencia'] = 'La frecuencia es requerida';
    }
    if (!this.fechaInicio()) {
      nuevosErrores['fechaInicio'] = 'La fecha de inicio es requerida';
    }

    this.errores.set(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  }

  /**
   * Limpiar formulario
   */
  private limpiarFormulario(): void {
    this.nombre.set('');
    this.descripcion.set('');
    this.categoria.set('');
    this.monto.set('');
    this.frecuencia.set('MENSUAL');
    this.fechaInicio.set(this.obtenerFechaHoy());
    this.errores.set({});
  }

  /**
   * Obtener fecha de hoy en formato ISO
   */
  private obtenerFechaHoy(): string {
    const hoy = new Date();
    return hoy.toISOString().split('T')[0];
  }

  /**
   * Obtener categoría por ID
   */
  obtenerCategoria(id: string) {
    return this.categorias.find(c => c.id === id);
  }
}