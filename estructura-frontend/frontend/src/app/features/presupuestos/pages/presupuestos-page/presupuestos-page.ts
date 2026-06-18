import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { PresupuestoService } from '../../../../core/services/presupuesto.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { PresupuestoDTO } from '../../../../core/models/financiero/presupuesto.model';
import { NotificacionService } from '../../../../core/services/notificacion.service';


@Component({
  selector: 'app-presupuestos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  providers: [DatePipe],
  templateUrl: './presupuestos-page.html',
  styleUrls: ['./presupuestos-page.scss']
})
export class PresupuestosPage implements OnInit {
  private fb = inject(FormBuilder);
  private presupuestoService = inject(PresupuestoService);
  private financieroService = inject(FinancieroService);
  private datePipe = inject(DatePipe);
  private notificacionService = inject(NotificacionService);


  // --- SIGNALS DE ESTADO ---
  formulario!: FormGroup;
  cargando = signal<boolean>(false);
  temaOscuro = signal<boolean>(false);
  
  // Datos del Negocio
  presupuestoActivo = signal<PresupuestoDTO | null>(null);
  gastoTotalMes = signal<number>(0);
  historialPresupuestos = signal<any[]>([]);
  
  // UI States
  mostrarConfirmEliminar = signal<boolean>(false);
  mostrarTodoHistorial = signal<boolean>(false);
  exitoMensaje = signal<string | null>(null);
  errorMensaje = signal<string | null>(null);

  // Mock de Categorías
  categorias = [
    { nombre: 'Alimentos y Bebidas', porcentaje: 42, monto: 504, icono: 'fa-utensils', bg: 'rgba(245, 158, 11, 0.1)', color: '#F59E0B' },
    { nombre: 'Transporte', porcentaje: 25, monto: 300, icono: 'fa-car', bg: 'rgba(6, 182, 212, 0.1)', color: '#06B6D4' },
    { nombre: 'Servicios', porcentaje: 18, monto: 216, icono: 'fa-bolt', bg: 'rgba(91, 106, 240, 0.1)', color: '#5B6AF0' },
    { nombre: 'Entretenimiento', porcentaje: 15, monto: 180, icono: 'fa-gamepad', bg: 'rgba(239, 68, 68, 0.1)', color: '#EF4444' }
  ];

  // --- SIGNALS COMPUTADOS ---
  porcentajeConsumo = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo || activo.montoLimite <= 0) return 0;
    const pct = (this.gastoTotalMes() / activo.montoLimite) * 100;
    return Math.min(Math.round(pct), 999);
  });

  margenDisponible = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo) return 0;
    const margen = activo.montoLimite - this.gastoTotalMes();
    return margen > 0 ? margen : 0;
  });

  estadoAlerta = computed<'seguro' | 'alerta' | 'superado'>(() => {
    const pct = this.porcentajeConsumo();
    const activo = this.presupuestoActivo();
    const limiteAlerta = activo ? activo.porcentajeAlerta : 80;

    if (pct >= 100) return 'superado';
    if (pct >= limiteAlerta) return 'alerta';
    return 'seguro';
  });

  diasRestantes = computed<number | null>(() => {
    const activo = this.presupuestoActivo();
    if (!activo) return null;
    const hoy = new Date();
    const fin = new Date(activo.fechaFin);
    const diferenciaMs = fin.getTime() - hoy.getTime();
    if (diferenciaMs < 0) return 0;
    return Math.ceil(diferenciaMs / (1000 * 60 * 60 * 24));
  });

  labelBotonGuardar = computed(() => {
    return this.presupuestoActivo() ? 'Actualizar límite' : 'Establecer límite';
  });

  gaugeOffset = computed(() => {
    const pct = Math.min(this.porcentajeConsumo(), 100);
    return (314.16 - (314.16 * pct) / 100).toString();
  });

  gaugeColor = computed(() => {
    const estado = this.estadoAlerta();
    if (estado === 'superado') return '#EF4444';
    if (estado === 'alerta') return '#F59E0B';
    return '#22C55E';
  });

  historialVisible = computed(() => {
    const lista = this.historialPresupuestos();
    return this.mostrarTodoHistorial() ? lista : lista.slice(0, 3);
  });

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarDatosDashboard();
    this.detectarTemaActual();
  }

  private inicializarFormulario(): void {
    const hoyStr = new Date().toISOString().substring(0, 10);
    const finMes = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0);
    const finStr = finMes.toISOString().substring(0, 10);

    this.formulario = this.fb.group({
      nombre: ['Presupuesto Mensual', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      montoLimite: [1000, [Validators.required, Validators.min(1)]],
      porcentajeAlerta: [80, [Validators.required, Validators.min(50), Validators.max(100)]],
      fechaInicio: [hoyStr, [Validators.required]],
      fechaFin: [finStr, [Validators.required]]
    }, { validators: this.validarFechas });
  }

  private validarFechas(control: AbstractControl): { [key: string]: boolean } | null {
    const inicio = control.get('fechaInicio')?.value;
    const fin = control.get('fechaFin')?.value;
    if (inicio && fin && new Date(fin) < new Date(inicio)) {
      return { 'fechasInvalidas': true };
    }
    return null;
  }

  campoInvalido(campo: string): boolean {
    const control = this.formulario.get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  private cargarDatosDashboard(): void {
    this.cargando.set(true);
    
    this.financieroService.getResumen().subscribe({
      next: (resumen) => this.gastoTotalMes.set(resumen.totalGastos || 0),
      error: () => this.mostrarToast('Error al recuperar balance de gastos.', 'danger')
    });

    this.presupuestoService.obtenerActivo().subscribe({
      next: (presupuesto) => {
        if (presupuesto) {
          this.presupuestoActivo.set(presupuesto);
          this.formulario.patchValue({
            nombre: presupuesto.nombre || 'Presupuesto Mensual',
            montoLimite: presupuesto.montoLimite,
            porcentajeAlerta: presupuesto.porcentajeAlerta,
            fechaInicio: this.formatearFechaInput(presupuesto.fechaInicio),
            fechaFin: this.formatearFechaInput(presupuesto.fechaFin)
          });
          this.formulario.get('nombre')?.disable();
        } else {
          this.presupuestoActivo.set(null);
          this.formulario.get('nombre')?.enable();
        }
        this.cargando.set(false);
      },
      error: () => {
        this.mostrarToast('Error al cargar la configuración de presupuestos.', 'danger');
        this.cargando.set(false);
      }
    });

    this.presupuestoService.listarHistorial().subscribe({
      next: (historial) => this.historialPresupuestos.set(historial || []),
      error: () => console.error('Error silencioso al listar historial.')
    });
  }

  guardarPresupuesto(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    const payload = this.formulario.value;
    const activoActual = this.presupuestoActivo();

    // Si hay un presupuesto activo, le metemos su ID al payload para enviar un solo argumento
    const request$ = activoActual 
      ? this.presupuestoService.actualizar({ ...payload, id: activoActual.id })
      : this.presupuestoService.crear(payload);

    request$.subscribe({
      next: () => {
        this.mostrarToast('Límite actualizado correctamente.', 'success');
        this.notificacionService.mostrarPresupuestoCreado(payload.nombre || 'Presupuesto');
        this.cargarDatosDashboard();
      },
      error: () => {
        this.mostrarToast('Hubo un error al guardar la configuración.', 'danger');
        this.cargando.set(false);
      }
    });
  }
  pedirConfirmacionEliminar(): void {
    this.mostrarConfirmEliminar.set(true);
  }

  cancelarEliminar(): void {
    this.mostrarConfirmEliminar.set(false);
  }

  eliminarPresupuesto(): void {
    this.mostrarConfirmEliminar.set(false);
    this.cargando.set(true);

    this.presupuestoService.eliminarActivo().subscribe({
      next: () => {
        this.mostrarToast('Límite desactivado del período actual.', 'success');
        this.presupuestoActivo.set(null);
        this.limpiarFormulario();
        this.cargarDatosDashboard();
      },
      error: () => {
        this.mostrarToast('No se pudo desactivar el límite.', 'danger');
        this.cargando.set(false);
      }
    });
  }

  limpiarFormulario(): void {
    this.formulario.patchValue({
      nombre: 'Presupuesto Mensual',
      montoLimite: 1000,
      porcentajeAlerta: 80
    });
    this.formulario.get('nombre')?.enable();
  }

  montoAlertaCalculado(): number {
    const limite = this.formulario?.get('montoLimite')?.value || 0;
    const pct = this.formulario?.get('porcentajeAlerta')?.value || 80;
    return (limite * pct) / 100;
  }

  pctHistorial(h: any): number {
    if (h.montoLimite <= 0) return 0;
    return Math.round((h.montoConsumido / h.montoLimite) * 100);
  }

  estadoConsumoHistorial(h: any): 'seguro' | 'alerta' | 'superado' {
    const pct = this.pctHistorial(h);
    if (pct >= 100) return 'superado';
    if (pct >= h.porcentajeAlerta) return 'alerta';
    return 'seguro';
  }

  toggleHistorial(): void {
    this.mostrarTodoHistorial.update(v => !v);
  }

  toggleTema(): void {
    const body = document.body;
    if (body.classList.contains('theme-dark')) {
      body.classList.remove('theme-dark');
      this.temaOscuro.set(false);
    } else {
      body.classList.add('theme-dark');
      this.temaOscuro.set(true);
    }
  }

  private detectarTemaActual(): void {
    this.temaOscuro.set(document.body.classList.contains('theme-dark'));
  }

  formatearMonto(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor);
  }

  private formatearFechaInput(fechaIso: string): string {
    return fechaIso.substring(0, 10);
  }

  private mostrarToast(mensaje: string, tipo: 'success' | 'danger'): void {
    if (tipo === 'success') {
      this.exitoMensaje.set(mensaje);
      setTimeout(() => this.exitoMensaje.set(null), 4000);
    } else {
      this.errorMensaje.set(mensaje);
      setTimeout(() => this.errorMensaje.set(null), 4000);
    }
  }
}