import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { IaService } from '../../../../core/services/ia.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { NotificacionService } from '../../../../core/services/notificacion.service';
import { MetodoPago, TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { CategoriaSugerida } from '../../../../core/models/ia_coach/ia-base.model';
import { OnboardingTour, TourStep } from '../../../../shared/components/onboarding-tour/onboarding-tour';
import { HasUnsavedChanges } from '../../../../core/guards/pending-changes.guard';

@Component({
  selector: 'app-nuevo-gasto-page',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule, OnboardingTour],
  templateUrl: './nuevo-gasto-page.html',
  styleUrls: ['../gastos-page/gastos-page.scss']
})
export class NuevoGastoPage implements OnInit, HasUnsavedChanges {
  private readonly stateService = inject(GastosStateService);
  private readonly authService = inject(AuthService);
  private readonly transaccionesService = inject(Transacciones);
  private readonly financieroService = inject(FinancieroService);
  private readonly iaService = inject(IaService);
  private readonly eventBus = inject(AppEventBus);
  private readonly router = inject(Router);
  private readonly notificacionService = inject(NotificacionService);

  readonly nombreGasto = signal('');
  readonly monto = signal<number>(0);
  readonly categoria = signal('');
  readonly fecha = signal(new Date().toISOString().split('T')[0]);
  readonly metodoPago = signal('DIGITAL');
  readonly descripcion = signal('');
  readonly etiquetas = signal<string[]>([]);
  readonly nuevaEtiqueta = signal('');
  readonly errores = signal<Record<string, string>>({});

  readonly sugerenciasIa = signal<CategoriaSugerida[]>([]);
  readonly sugerenciaSeleccionada = signal<CategoriaSugerida | null>(null);
  readonly categoriaIAPendiente = signal<{ nombre: string; icono: string } | null>(null);
  readonly clasificandoIa = signal(false);
  readonly guardandoGasto = signal(false);
  readonly mensajeFormulario = signal<string | null>(null);

  formularioGuardado = false;

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: '#gasto-nombre',
      title: 'Nombre del Gasto',
      description: 'Ingresa el nombre o concepto de la compra (ej: Spotify, Almuerzo).',
      position: 'bottom'
    },
    {
      targetSelector: '#gasto-monto',
      title: 'Monto del Gasto',
      description: 'Digita el importe de la transacción en soles.',
      position: 'bottom'
    },
    {
      targetSelector: '#gasto-categoria',
      title: 'Categoría',
      description: 'Elige el rubro correspondiente para clasificar tu presupuesto.',
      position: 'top'
    },
    {
      targetSelector: '#tour-ia-gasto',
      title: 'Sugerencia Inteligente',
      description: 'Haz clic aquí para que nuestra IA analice la descripción y te recomiende la mejor categoría.',
      position: 'top'
    }
  ];

  readonly categoriasConCrear = computed(() => {
    const defaultCats = this.stateService.categorias();
    const result = [...defaultCats];
    const pending = this.categoriaIAPendiente();
    if (pending) {
      if (!result.some(c => c.nombre.toLowerCase() === pending.nombre.toLowerCase())) {
        result.push({ id: 'PENDIENTE_IA', nombre: pending.nombre, icono: pending.icono, tipo: 'GASTO' } as any);
      }
    }
    result.push({ id: 'CREAR_NUEVA', nombre: '+ Crear nueva categoría...', icono: 'plus', tipo: 'GASTO' } as any);
    return result;
  });

  readonly metodosPagoDisponibles = [
    { id: 'EFECTIVO', nombre: 'Efectivo' },
    { id: 'TARJETA', nombre: 'Tarjeta' },
    { id: 'TRANSFERENCIA', nombre: 'Transferencia' },
    { id: 'DIGITAL', nombre: 'Digital (Yape/Plin)' },
  ];

  get intentosIaMaximos(): number { return this.iaService.clasificacionesMaximas(); }
  get intentosIaRestantes(): number { return this.iaService.clasificacionesRestantes(); }

  ngOnInit(): void {
    this.stateService.cargarDatos();
    const tourVisto = localStorage.getItem('luka_tour_nuevo_gasto_visto');
    if (!tourVisto) {
      setTimeout(() => this.mostrarTour.set(true), 600);
    }
  }

  hasUnsavedChanges(): boolean {
    if (this.formularioGuardado) return false;
    return this.nombreGasto() !== '' || this.monto() > 0 || this.categoria() !== '';
  }

  completarTour(): void {
    localStorage.setItem('luka_tour_nuevo_gasto_visto', 'true');
    this.mostrarTour.set(false);
  }

  iniciarTourManualmente(): void {
    this.mostrarTour.set(true);
  }

  agregarEtiqueta(): void {
    const et = this.nuevaEtiqueta().trim();
    if (et && !this.etiquetas().includes(et)) {
      this.etiquetas.update(list => [...list, et]);
    }
    this.nuevaEtiqueta.set('');
  }

  eliminarEtiqueta(et: string): void {
    this.etiquetas.update(list => list.filter(e => e !== et));
  }

  puedeSugerirCategoriaIa(): boolean {
    return this.nombreGasto().length > 2 && !this.clasificandoIa() && this.intentosIaRestantes > 0;
  }

  clasificarConIa(): void {
    const texto = `${this.nombreGasto()} ${this.descripcion()}`.trim();
    if (!texto || texto.length < 3) return;
    this.clasificandoIa.set(true);
    this.sugerenciasIa.set([]);

    this.iaService.getClasificarTransaccion({
      id_temporal: 'gasto',
      tipo_movimiento: 'GASTO',
      descripcion: texto,
      etiquetas: this.etiquetas().join(',')
    }).subscribe({
      next: (res) => {
        this.clasificandoIa.set(false);
        if (res.datos?.sugerencias) {
          this.sugerenciasIa.set(res.datos.sugerencias);
        }
      },
      error: () => {
        this.clasificandoIa.set(false);
      }
    });
  }

  seleccionarSugerencia(sug: CategoriaSugerida): void {
    this.sugerenciaSeleccionada.set(sug);
  }

  confirmarSugerencia(): void {
    const sug = this.sugerenciaSeleccionada();
    if (!sug) return;
    const existe = this.stateService.categorias().find(c => c.nombre.toLowerCase() === sug.categoria.toLowerCase());
    if (existe) {
      this.categoria.set(existe.id);
      this.categoriaIAPendiente.set(null);
    } else {
      this.categoriaIAPendiente.set({ nombre: sug.categoria, icono: sug.icono });
      this.categoria.set('PENDIENTE_IA');
    }
    this.sugerenciasIa.set([]);
    this.sugerenciaSeleccionada.set(null);
  }

  confirmarCrearCategoriaGasto(nombre: string): void {
    const n = nombre.trim();
    if (!n) return;
    this.financieroService.crearCategoria({
      nombre: n, descripcion: 'Categoría personalizada', icono: 'receipt', tipo: 'GASTO'
    }).subscribe({
      next: (c) => {
        this.stateService.categorias.update(cats => [...cats, c]);
        this.categoria.set(c.id);
      },
      error: () => console.error('Error al crear categoría.')
    });
  }

  private validarFormulario(): boolean {
    const e: Record<string, string> = {};
    if (!this.nombreGasto().trim()) e['nombreGasto'] = 'El nombre es requerido';
    if (!this.categoria()) e['categoria'] = 'Selecciona una categoría';
    if (this.monto() <= 0) e['monto'] = 'El monto debe ser mayor a 0';
    if (!this.fecha()) e['fecha'] = 'La fecha es inválida';
    this.errores.set(e);
    return Object.keys(e).length === 0;
  }

  private registrarFinal(catId: string): void {
    const getLocalIsoString = (dateString: string): string => {
      let localDate = new Date();
      if (dateString) {
        const parts = dateString.split('-');
        if (parts.length === 3) {
          localDate = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        } else {
          localDate = new Date(dateString);
        }
      }
      const now = new Date();
      localDate.setHours(now.getHours(), now.getMinutes(), now.getSeconds());
      const tzOffset = localDate.getTimezoneOffset() * 60000;
      return new Date(localDate.getTime() - tzOffset).toISOString().slice(0, 19);
    };

    const payload: TransaccionRequestDTO = {
      usuarioId: this.authService.usuario()?.id || '',
      nombreCliente: this.nombreGasto().trim(),
      monto: this.monto(),
      tipo: 'GASTO',
      categoriaId: catId,
      fechaTransaccion: getLocalIsoString(this.fecha()),
      metodoPago: this.metodoPago() as MetodoPago,
      descripcion: this.descripcion().trim(),
      etiquetas: this.etiquetas().join(',')
    };

    this.transaccionesService.registrar(payload).subscribe({
      next: () => {
        this.guardandoGasto.set(false);
        this.formularioGuardado = true;
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        this.notificacionService.mostrarIngresoRegistrado(this.monto(), 'Gasto registrado');
        this.router.navigate(['/gastos']);
      },
      error: () => {
        this.guardandoGasto.set(false);
        this.mensajeFormulario.set('Ocurrió un error al registrar el gasto.');
      }
    });
  }

  guardarGasto(): void {
    if (!this.validarFormulario()) return;
    this.guardandoGasto.set(true);
    this.mensajeFormulario.set(null);

    const pending = this.categoriaIAPendiente();
    if (this.categoria() === 'PENDIENTE_IA' && pending) {
      this.financieroService.crearCategoria({
        nombre: pending.nombre, descripcion: 'Sugerida por IA', icono: pending.icono, tipo: 'GASTO'
      }).subscribe({
        next: (c) => {
          this.stateService.categorias.update(cats => [...cats, c]);
          this.categoria.set(c.id);
          this.registrarFinal(c.id);
        },
        error: () => {
          this.guardandoGasto.set(false);
          this.mensajeFormulario.set('Error al crear la categoría de IA.');
        }
      });
    } else {
      this.registrarFinal(this.categoria());
    }
  }

  cancelar(): void {
    this.router.navigate(['/gastos']);
  }
}
