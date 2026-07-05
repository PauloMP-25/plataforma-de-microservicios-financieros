import { Component, signal, computed, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms'; 
import { 
  CATEGORIAS_SUSCRIPCION, 
  FRECUENCIAS_SUSCRIPCION,
  CrearSuscripcionRequest,
  ActualizarSuscripcionRequest,
  SuscripcionDTO
} from '../../../../core/models/financiero/suscripcion-gasto.model';

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

  // 👇 Computed signal para el preview de ícono
  readonly iconoPreview = computed(() => {
    const name = this.nombre().toLowerCase();
    
    if (name.includes('netflix')) return 'fa-brands fa-netflix';
    if (name.includes('spotify')) return 'fa-brands fa-spotify';
    if (name.includes('youtube')) return 'fa-brands fa-youtube';
    if (name.includes('prime') || name.includes('amazon')) return 'fa-brands fa-amazon';
    if (name.includes('apple')) return 'fa-brands fa-apple';
    if (name.includes('github')) return 'fa-brands fa-github';
    if (name.includes('google')) return 'fa-brands fa-google';
    if (name.includes('playstation') || name.includes('ps plus')) return 'fa-brands fa-playstation';
    if (name.includes('xbox')) return 'fa-brands fa-xbox';
    
    // Si no es una marca conocida, buscar el ícono de la categoría
    const catId = this.categoria();
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? `fa-solid ${cat.icon}` : 'fa-solid fa-circle-question';
  });

  // UI
  readonly guardando = signal(false);
  readonly errores = signal<Record<string, string>>({});

  // Data para selects
  readonly categorias = CATEGORIAS_SUSCRIPCION;
  readonly frecuencias = FRECUENCIAS_SUSCRIPCION.filter(f => ['MENSUAL', 'ANUAL', 'QUINCENAL'].includes(f.id));

  readonly colorPreview = computed(() => {
    const name = this.nombre().toLowerCase();
    if (name.includes('netflix')) return '#e50914';
    if (name.includes('spotify')) return '#1db954';
    if (name.includes('youtube')) return '#ff0000';
    if (name.includes('prime') || name.includes('amazon')) return '#ff9900';
    if (name.includes('apple')) return '#a3aaae';
    if (name.includes('github')) return '#24292e';
    if (name.includes('google')) return '#4285f4';
    if (name.includes('playstation')) return '#003087';
    if (name.includes('xbox')) return '#107c10';

    const catId = this.categoria();
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? cat.color : '#5B6AF0';
  });

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