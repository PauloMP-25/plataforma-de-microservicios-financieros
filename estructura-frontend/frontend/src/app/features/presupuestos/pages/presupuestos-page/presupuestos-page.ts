import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PresupuestoService } from '../../../../core/services/presupuesto.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { PresupuestoDTO } from '../../../../core/models/financiero/presupuesto.model';

@Component({
  selector: 'app-presupuestos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './presupuestos-page.html',
  styleUrls: ['./presupuestos-page.scss'],
})
export class PresupuestosPage implements OnInit {
  private fb = inject(FormBuilder);
  private presupuestoService = inject(PresupuestoService);
  private financieroService = inject(FinancieroService);

  formulario!: FormGroup;
  presupuestoActivo = signal<PresupuestoDTO | null>(null);
  gastoTotalMes = signal<number>(0);
  cargando = signal<boolean>(false);
  historialPresupuestos = signal<PresupuestoDTO[]>([]);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');

  // Computeds
  porcentajeConsumo = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo || activo.montoLimite <= 0) return 0;
    const porcentaje = (this.gastoTotalMes() / activo.montoLimite) * 100;
    return Math.min(Math.round(porcentaje), 100);
  });

  estadoAlerta = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo) return 'seguro';
    const porcentaje = (this.gastoTotalMes() / activo.montoLimite) * 100;
    if (porcentaje >= 100) return 'superado';
    if (porcentaje >= activo.porcentajeAlerta) return 'alerta';
    return 'seguro';
  });

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarDatos();
  }

  inicializarFormulario(): void {
    const hoy = new Date();
    const primerDia = new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString().split('T')[0];
    const ultimoDia = new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).toISOString().split('T')[0];

    this.formulario = this.fb.group({
      montoLimite: [null, [Validators.required, Validators.min(1)]],
      porcentajeAlerta: [80, [Validators.required, Validators.min(1), Validators.max(100)]],
      fechaInicio: [primerDia, [Validators.required]],
      fechaFin: [ultimoDia, [Validators.required]]
    }, { validators: this.validarFechas });
  }

  validarFechas(group: FormGroup): any {
    const inicio = group.get('fechaInicio')?.value;
    const fin = group.get('fechaFin')?.value;
    if (inicio && fin && new Date(inicio) > new Date(fin)) {
      return { fechasInvalidas: true };
    }
    return null;
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.errorMensaje.set('');

    this.financieroService.getResumen().subscribe({
      next: (resumen) => {
        this.gastoTotalMes.set(resumen.totalGastos);

        this.presupuestoService.obtenerActivo().subscribe({
          next: (presupuesto) => {
            if (presupuesto && presupuesto.activo) {
              this.presupuestoActivo.set(presupuesto);
              this.formulario.patchValue({
                montoLimite: presupuesto.montoLimite,
                porcentajeAlerta: presupuesto.porcentajeAlerta,
                fechaInicio: presupuesto.fechaInicio.split('T')[0],
                fechaFin: presupuesto.fechaFin.split('T')[0]
              });
            } else {
              this.presupuestoActivo.set(null);
            }
            this.cargando.set(false);
          },
          error: () => {
            this.presupuestoActivo.set(null);
            this.cargando.set(false);
          }
        });
      },
      error: () => {
        this.errorMensaje.set('No se pudo cargar el resumen financiero.');
        this.cargando.set(false);
      }
    });

    this.presupuestoService.listarHistorial().subscribe({
      next: (historial) => {
        this.historialPresupuestos.set(historial.filter(h => !h.activo));
      },
      error: (err) => {
        console.error('Error al cargar historial:', err);
      }
    });
  }

  guardarPresupuesto(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const payload = this.formulario.value;
    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const peticion = this.presupuestoActivo()
      ? this.presupuestoService.actualizar(payload)
      : this.presupuestoService.crear(payload);

    peticion.subscribe({
      next: (resp) => {
        this.exitoMensaje.set('Presupuesto configurado correctamente.');
        this.presupuestoActivo.set(resp);
        this.cargarDatos();
        setTimeout(() => this.exitoMensaje.set(''), 3000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al guardar el presupuesto.');
      }
    });
  }

  eliminarPresupuesto(): void {
    if (!confirm('¿Está seguro de que desea eliminar el presupuesto activo?')) {
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    this.presupuestoService.eliminarActivo().subscribe({
      next: () => {
        this.exitoMensaje.set('Presupuesto desactivado correctamente.');
        this.presupuestoActivo.set(null);
        this.formulario.reset({
          montoLimite: null,
          porcentajeAlerta: 80,
          fechaInicio: new Date().toISOString().split('T')[0],
          fechaFin: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString().split('T')[0]
        });
        this.cargarDatos();
        setTimeout(() => this.exitoMensaje.set(''), 3000);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMensaje.set(err.error?.mensaje || 'Error al desactivar el presupuesto.');
      }
    });
  }
}
