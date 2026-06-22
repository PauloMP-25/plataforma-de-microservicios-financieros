import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HasUnsavedChanges } from '../../../../core/guards/pending-changes.guard';
import { RespuestaMetaAhorro, SolicitudMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { NotificacionService } from '../../../../core/services/notificacion.service';
import { MetasUtilityService } from '../../services/metas-utility.service';
import { MetaPurposeSelectorComponent } from '../../components/meta-purpose-selector/meta-purpose-selector.component';
import { MetaPreviewCardComponent } from '../../components/meta-preview-card/meta-preview-card.component';
import { MetaRecommendedSavingsComponent } from '../../components/meta-recommended-savings/meta-recommended-savings.component';
import { MetasDataService } from '../../services/metas-data.service';

@Component({
  selector: 'app-meta-form-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MetaPurposeSelectorComponent,
    MetaPreviewCardComponent,
    MetaRecommendedSavingsComponent
  ],
  templateUrl: './meta-form-page.html',
  styleUrl: './meta-form-page.scss',
})
export class MetaFormPage implements OnInit, HasUnsavedChanges {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private metasDataService = inject(MetasDataService);
  private financieroService = inject(FinancieroService);
  private notificacionService = inject(NotificacionService);
  private metasUtility = inject(MetasUtilityService);

  formulario!: FormGroup;
  modoEdicion = false;
  metaId: string | null = null;
  cargando = signal<boolean>(false);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');
  fechaMinima = '';
  formularioGuardado = false;

  hasUnsavedChanges(): boolean {
    return this.formulario && this.formulario.dirty && !this.formularioGuardado;
  }

  protected readonly Math = Math;

  // Lista de categorías con sus íconos
  categorias = this.metasUtility.categorias;

  ngOnInit(): void {
    const hoy = new Date();
    this.fechaMinima = hoy.toISOString().split('T')[0];

    this.inicializarFormulario();

    this.financieroService.getResumen().subscribe({
      next: (resumen) => {
        if (!this.modoEdicion && resumen) {
          this.formulario.patchValue({
            montoActual: resumen.balance
          });
        }
      }
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.modoEdicion = true;
        this.metaId = id;
        this.cargarMetaParaEditar(id);
      }
    });
  }

  inicializarFormulario(): void {
    const hoy = new Date();
    const unMesDespues = new Date(hoy.getFullYear(), hoy.getMonth() + 1, hoy.getDate());
    const fechaDefecto = unMesDespues.toISOString().split('T')[0];

    this.formulario = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      categoria: ['Viaje', Validators.required],
      montoObjetivo: [null, [Validators.required, Validators.min(1.00)]],
      montoActual: [{ value: 0.00, disabled: true }, [Validators.required, Validators.min(0.00)]],
      fechaLimite: [fechaDefecto, [Validators.required, this.validarFechaFutura.bind(this)]]
    });
  }

  validarFechaFutura(control: any): { [key: string]: boolean } | null {
    if (control.value) {
      const fechaSeleccionada = new Date(control.value + 'T00:00:00');
      const hoy = new Date();
      hoy.setHours(0, 0, 0, 0);
      if (fechaSeleccionada <= hoy) {
        return { fechaPasada: true };
      }
    }
    return null;
  }

  cargarMetaParaEditar(id: string): void {
    this.cargando.set(true);
    this.errorMensaje.set('');

    this.metasDataService.obtenerMeta(id).subscribe({
      next: (meta) => {
        this.completarFormularioConMeta(meta);
        this.cargando.set(false);
      },
      error: () => {
        this.errorMensaje.set('No se pudo encontrar la meta seleccionada en el sistema.');
        this.cargando.set(false);
      }
    });
  }

  completarFormularioConMeta(meta: RespuestaMetaAhorro): void {
    const datosVisuales = this.metasUtility.obtenerCategoriaYNombre(meta.nombre);
    this.formulario.patchValue({
      nombre: datosVisuales.nombre,
      categoria: meta.proposito || datosVisuales.categoria || 'Otros',
      montoObjetivo: meta.montoObjetivo,
      montoActual: meta.montoActual,
      fechaLimite: meta.fechaLimite
    });
    if (this.modoEdicion) {
      this.formulario.get('nombre')?.disable();
      this.formulario.get('categoria')?.disable();
    }
  }

  obtenerIconoCategoria(catId: string): string {
    return this.metasUtility.obtenerIconoCategoria(catId);
  }

  setFechaRapida(meses: number): void {
    const hoy = new Date();
    const nuevaFecha = new Date(hoy.getFullYear(), hoy.getMonth() + meses, hoy.getDate());
    const fechaStr = nuevaFecha.toISOString().split('T')[0];
    this.formulario.get('fechaLimite')?.setValue(fechaStr);
  }

  seleccionarCategoriaForm(catId: string): void {
    if (this.formulario.get('categoria')?.disabled) return;
    this.formulario.get('categoria')?.setValue(catId);
  }

  obtenerColorProgreso(pct: number): string {
    if (pct >= 100) return '#22c55e';
    return '#5b6af0';
  }

  getAhorroSugeridoInfo(): { cuota: number; meses: number } {
    if (!this.formulario) return { cuota: 0, meses: 0 };
    
    const obj = this.formulario.get('montoObjetivo')?.value || 0;
    const act = this.formulario.get('montoActual')?.value || 0;
    const fecha = this.formulario.get('fechaLimite')?.value;

    if (!fecha || obj <= 0) return { cuota: 0, meses: 0 };

    const limite = new Date(fecha + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    let meses = (limite.getFullYear() - hoy.getFullYear()) * 12 + (limite.getMonth() - hoy.getMonth());
    
    if (meses <= 0) {
      const dias = Math.ceil((limite.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
      if (dias <= 0) return { cuota: 0, meses: 0 };
      return { cuota: Math.max(0, obj - act), meses: 1 };
    }

    const cuota = Math.max(0, (obj - act) / meses);
    return { cuota, meses };
  }

  guardarMeta(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const formVal = this.formulario.getRawValue();
    const nombreConPrefijo = formVal.categoria ? `[${formVal.categoria}] ${formVal.nombre}` : formVal.nombre;

    const payload: SolicitudMetaAhorro = {
      nombre: nombreConPrefijo,
      montoObjetivo: formVal.montoObjetivo,
      montoActual: formVal.montoActual ?? 0,
      fechaLimite: formVal.fechaLimite,
      proposito: formVal.categoria
    };

    if (this.modoEdicion && this.metaId) {
      // Flujo de edición
      this.metasDataService.actualizarMeta(this.metaId, payload).subscribe({
        next: () => {
          this.exitoMensaje.set(`Meta "${formVal.nombre}" actualizada con éxito.`);
          this.finalizarConExito(formVal.nombre, true);
        },
        error: () => {
          this.errorMensaje.set('Hubo un error al guardar la meta de ahorro.');
          this.cargando.set(false);
        }
      });
    } else {
      // Flujo de creación
      this.metasDataService.crearMeta(payload).subscribe({
        next: (nuevaMeta) => {
          const datosVisuales = this.metasUtility.obtenerCategoriaYNombre(nuevaMeta.nombre);
          this.exitoMensaje.set(`Meta "${datosVisuales.nombre}" creada con éxito.`);
          this.finalizarConExito(datosVisuales.nombre, false);
        },
        error: () => {
          this.errorMensaje.set('Hubo un error al crear la meta de ahorro.');
          this.cargando.set(false);
        }
      });
    }
  }

  private finalizarConExito(nombreMeta: string, esEdicion = false): void {
    this.formularioGuardado = true;
    this.cargando.set(false);
    if (esEdicion) {
      this.notificacionService.mostrarDatosGuardados(`Meta "${nombreMeta}" actualizada con éxito.`);
    } else {
      this.notificacionService.mostrarMetaCreada(nombreMeta);
    }
    setTimeout(() => {
      this.router.navigate(['/metas']);
    }, 1500);
  }
}
