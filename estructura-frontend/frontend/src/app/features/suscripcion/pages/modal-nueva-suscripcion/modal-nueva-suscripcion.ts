import { Component, signal, computed, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms'; 
import { 
  CATEGORIAS_SUSCRIPCION, 
  FRECUENCIAS_SUSCRIPCION,
  CrearSuscripcionRequest 
} from '../../../../core/models/financiero/suscripcion-gasto.model';

@Component({
  selector: 'app-modal-nueva-suscripcion',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule], 
  templateUrl: './modal-nueva-suscripcion.html',
  styleUrl: './modal-nueva-suscripcion.scss'
})
export class ModalNuevaSuscripcion {
  @Output() cerrar = new EventEmitter<void>();
  @Output() crear = new EventEmitter<CrearSuscripcionRequest>();

  // Formulario
  readonly nombre = signal('');
  readonly descripcion = signal('');
  readonly categoria = signal('');
  readonly monto = signal('');
  readonly frecuencia = signal('MENSUAL');
  readonly proximoPago = signal(this.obtenerFechaHoy()); // Reemplaza a fechaInicio

  // 👇 LA MAGIA: Calculamos los días automáticamente cuando cambia proximoPago
  readonly diasRestantes = computed(() => {
    const fechaStr = this.proximoPago();
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

  // UI
  readonly guardando = signal(false);
  readonly errores = signal<Record<string, string>>({});

  // Data para selects
  readonly categorias = CATEGORIAS_SUSCRIPCION;
  readonly frecuencias = FRECUENCIAS_SUSCRIPCION;

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

    const request: CrearSuscripcionRequest = {
      nombre: this.nombre(),
      descripcion: this.descripcion(),
      categoria: this.categoria(),
      monto: parseFloat(this.monto()),
      frecuencia: this.frecuencia() as any,
      fechaInicio: this.proximoPago() // Lo mandamos como fechaInicio si tu API lo espera así
    };

    // Simular delay de envío
    setTimeout(() => {
      this.crear.emit(request);
      this.limpiarFormulario();
      this.guardando.set(false);
    }, 500);
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
    if (!this.proximoPago()) {
      nuevosErrores['proximoPago'] = 'La fecha de próximo pago es requerida';
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
    this.proximoPago.set(this.obtenerFechaHoy());
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