import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HasUnsavedChanges } from '../../../../core/guards/pending-changes.guard';
import { OnboardingTour, TourStep } from '../../../../shared/components/onboarding-tour/onboarding-tour';
import { GastoFormComponent } from '../../components/gasto-form/gasto-form';
import { GastoPreviewComponent } from '../../components/gasto-preview/gasto-preview';
import { GastoRecentListComponent } from '../../components/gasto-recent-list/gasto-recent-list';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { IaService } from '../../../../core/services/ia.service';
import { CategoriaSugerida } from '../../../../core/models/ia_coach/ia-base.model';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { NotificacionService } from '../../../../core/services/notificacion.service';
import { DistribucionCategoria, GastoFormData, GastoReciente, OptionItem } from '../../types/gastos.interfaces';

@Component({
  selector: 'app-nuevo-gasto-page',
  standalone: true,
  imports: [CommonModule, RouterLink, GastoFormComponent, GastoPreviewComponent, GastoRecentListComponent, OnboardingTour],
  templateUrl: './nuevo-gasto-page.html',
  styleUrl: './nuevo-gasto-page.scss',
})
export class NuevoGastoPage implements HasUnsavedChanges, OnInit {
  private readonly stateService = inject(GastosStateService);
  private readonly authService = inject(AuthService);
  private readonly transaccionesService = inject(Transacciones);
  private readonly financieroService = inject(FinancieroService);
  private readonly iaService = inject(IaService);
  private readonly eventBus = inject(AppEventBus);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notificacionService = inject(NotificacionService);

  formularioGuardado = false;
  private initialForm = '';

  hasUnsavedChanges(): boolean {
    return !this.formularioGuardado && JSON.stringify(this.form) !== this.initialForm;
  }

  readonly sugerenciaSeleccionadaSignal = signal<CategoriaSugerida | null>(null);
  readonly categoriaIAPendiente = signal<{ nombre: string; icono: string } | null>(null);
  get sugerenciaSeleccionada(): CategoriaSugerida | null { return this.sugerenciaSeleccionadaSignal(); }

  readonly metodos: OptionItem[] = [
    { label: 'Efectivo', value: 'EFECTIVO' },
    { label: 'Tarjeta', value: 'TARJETA' },
    { label: 'Transferencia', value: 'TRANSFERENCIA' },
    { label: 'Digital (Yape/Plin)', value: 'DIGITAL' },
  ];

  form: GastoFormData = {
    nombreGasto: '',
    monto: 0,
    fechaTransaccion: new Date().toLocaleDateString('es-PE'), // dd/mm/yyyy
    descripcion: '',
    categoria: '',
    categoriaNombre: '',
    metodoPago: 'TRANSFERENCIA',
    etiquetas: [],
  };

  readonly sugerenciasSignal = signal<CategoriaSugerida[]>([]);
  readonly guardando = signal<boolean>(false);
  readonly clasificandoIa = signal<boolean>(false);
  get sugerencias(): CategoriaSugerida[] { return this.sugerenciasSignal(); }
  get intentosIaRestantes(): number { return this.iaService.clasificacionesRestantes(); }
  get intentosIaMaximos(): number { return this.iaService.clasificacionesMaximas(); }

  // ── Signals computados para enlazar al estado real ──
  readonly categoriasSignal = computed<OptionItem[]>(() => {
    const cats = this.stateService.categorias();
    const pending = this.categoriaIAPendiente();
    let options: OptionItem[] = [];
    if (cats.length > 0) {
      const match = cats.find(c => c.nombre.toLowerCase() === 'salario');
      if (match && this.form.categoria === 'Salario') {
        this.form.categoria = match.id;
        this.form.categoriaNombre = match.nombre;
      }
      options = cats.map(c => ({ label: c.nombre, value: c.id }));
    } else {
      options = [
        { label: 'Salario', value: 'salario' },
        { label: 'Freelance', value: 'freelance' },
        { label: 'Inversiones', value: 'inversion' },
        { label: 'Ventas', value: 'venta' },
        { label: 'Otros Gastos', value: 'otros' },
      ];
    }
    if (pending) {
      if (!options.some(o => o.label.toLowerCase() === pending.nombre.toLowerCase())) {
        options.push({ label: pending.nombre, value: 'PENDIENTE_IA' });
      }
    }
    return options;
  });


  readonly distribucionSignal = computed<DistribucionCategoria[]>(() => {
    const transacciones = this.stateService.gastos();
    if (!transacciones.length) return [];
    const map = new Map<string, number>();
    let total = 0;
    for (const t of transacciones) {
      const cat = t.categoria || 'Otros';
      const m = t.monto || 0;
      map.set(cat, (map.get(cat) ?? 0) + m);
      total += m;
    }
    const colores = ['#22c55e', '#7c3aed', '#f59e0b', '#06b6d4', '#ec4899', '#64748b'];
    return Array.from(map.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([categoria, monto], idx) => ({
        categoria,
        monto,
        porcentaje: total > 0 ? (monto / total) * 100 : 0,
        color: colores[idx % colores.length]
      }));
  });

  readonly recientesSignal = computed<GastoReciente[]>(() => {
    const transacciones = this.stateService.gastos();
    return transacciones.slice(0, 5).map(t => {
      const fecha = new Date(t.fechaTransaccion);
      return {
        categoria: t.categoria || 'Otros',
        descripcion: t.descripcion || t.notas || 'Gasto registrado',
        monto: t.monto || 0,
        fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
      };
    });
  });

  // Getters para mantener bindings de la plantilla HTML
  get categorias(): OptionItem[] { return this.categoriasSignal(); }
  get distribucion(): DistribucionCategoria[] { return this.distribucionSignal(); }
  get recientes(): GastoReciente[] { return this.recientesSignal(); }

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: 'input[name="monto"]',
      title: 'Monto del Gasto',
      description: 'Ingresa la cantidad recibida en Soles (S/).',
      position: 'bottom'
    },
    {
      targetSelector: 'textarea[name="descripcion"]',
      title: 'Descripción del Gasto',
      description: 'Describe brevemente de dónde proviene este dinero (ej: Salario, Venta, Freelance).',
      position: 'bottom'
    },
    {
      targetSelector: 'select[name="categoria"]',
      title: 'Categoría de Gasto',
      description: 'Clasifica tu Gasto para organizar mejor el origen de tus flujos.',
      position: 'top'
    }
  ];

  ngOnInit(): void {
    const tourVisto = localStorage.getItem('luka_tour_nuevo_Gasto_visto');
    if (!tourVisto) {
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
    
    // Leer query params para auto-completar desde Metas
    this.route.queryParams.subscribe(params => {
      if (params['monto']) {
        this.form.monto = Number(params['monto']);
      }
      if (params['nombre']) {
        this.form.nombreGasto = params['nombre'];
      }
      if (params['descripcion']) {
        this.form.descripcion = params['descripcion'];
      }
      if (params['metaId']) {
        // Añadir una etiqueta de la meta
        if (!this.form.etiquetas.includes('META')) {
          this.form.etiquetas.push('META');
        }
      }
    });
  }

  completarTour(): void {
    localStorage.setItem('luka_tour_nuevo_Gasto_visto', 'true');
    this.mostrarTour.set(false);
  }

  iniciarTourManualmente(): void {
    this.mostrarTour.set(true);
  }

  constructor() {
    this.stateService.cargarDatos();
    this.initialForm = JSON.stringify(this.form);
  }

  clasificarConIa(): void {
    const desc = this.form.descripcion?.trim();
    if (!desc || desc.length < 4) {
      this.sugerenciasSignal.set([]);
      return;
    }
    if (this.clasificandoIa() || this.intentosIaRestantes <= 0) return;
    this.clasificandoIa.set(true);

    this.iaService.getClasificarTransaccion({
      id_temporal: 'nuevo-gasto',
      tipo_movimiento: 'GASTO',
      descripcion: desc,
      etiquetas: this.form.etiquetas.join(',')
    }).subscribe({
      next: (res) => {
        this.clasificandoIa.set(false);
        if (res.datos) {
          const sugerencias = res.datos.sugerencias;
          if (sugerencias) {
            this.sugerenciasSignal.set(sugerencias);
          }
        }
      },
      error: () => {
        this.clasificandoIa.set(false);
        // Fallback local
        const matched = ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Otros Gastos'].filter(c =>
          c.toLowerCase().includes(desc.toLowerCase())
        );
        const fallbackList: CategoriaSugerida[] = (matched.length > 0 ? matched : ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Otros Gastos']).slice(0, 5).map(c => ({
          categoria: c,
          icono: this.iconoCategoria(c)
        }));
        this.sugerenciasSignal.set(fallbackList);
      }
    });
  }

  private iconoCategoria(nombre: string): string {
    const key = nombre.toLowerCase();
    if (key.includes('salario') || key.includes('sueldo')) return 'briefcase';
    if (key.includes('freelance') || key.includes('independiente')) return 'code';
    if (key.includes('invers') || key.includes('dividendo')) return 'trending-up';
    if (key.includes('venta') || key.includes('comercio')) return 'tag';
    if (key.includes('bono') || key.includes('regalo')) return 'gift';
    return 'plus-circle';
  }

  crearCategoriaManualmente(nombre: string, icono?: string): void {
    const nameTrim = nombre.trim();
    if (!nameTrim) return;

    // Verificar si ya existe por nombre
    const match = this.categorias.find(
      c => c.label.toLowerCase() === nameTrim.toLowerCase()
    );
    if (match) {
      this.form.categoria = match.value;
      this.form.categoriaNombre = match.label;
      return;
    }

    // Crear la categoría en base de datos
    this.financieroService.crearCategoria({
      nombre: nameTrim,
      descripcion: 'Categoría personalizada de Gastos',
      icono: icono || this.iconoCategoria(nameTrim),
      tipo: 'GASTO'
    }).subscribe({
      next: (cat) => {
        // Añadirla reactivamente a la lista local
        this.stateService.categorias.update(cats => [...cats, cat]);
        // Pre-seleccionar el UUID y actualizar el nombre legible
        this.form.categoria = cat.id;
        this.form.categoriaNombre = cat.nombre;
      },
      error: (err) => {
        console.error('Error al crear categoría:', err);
      }
    });
  }

  seleccionarSugerencia(sug: CategoriaSugerida): void {
    this.sugerenciaSeleccionadaSignal.set(sug);
  }

  confirmarSugerencia(): void {
    const sug = this.sugerenciaSeleccionadaSignal();
    if (!sug) return;

    const match = this.categorias.find(
      c => c.label.toLowerCase() === sug.categoria.toLowerCase()
    );
    if (match) {
      this.form.categoria = match.value;
      this.form.categoriaNombre = match.label;
      this.categoriaIAPendiente.set(null);
    } else {
      this.categoriaIAPendiente.set({ nombre: sug.categoria, icono: sug.icono });
      this.form.categoria = 'PENDIENTE_IA';
      this.form.categoriaNombre = sug.categoria;
    }
    this.sugerenciaSeleccionadaSignal.set(null);
  }


  private nombreCategoriaPorId(id: string): string {
    const match = this.categorias.find(c => c.value === id);
    return match ? match.label : 'Gastos';
  }

  guardar(): void {
    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      console.error('No se encontró sesión activa.');
      return;
    }

    this.formularioGuardado = true;

    const registrarGastoFinal = (catId: string) => {
      this.guardando.set(true);

      const getLocalIsoString = (date: Date): string => {
        const now = new Date();
        date.setHours(now.getHours(), now.getMinutes(), now.getSeconds());
        const tzOffset = date.getTimezoneOffset() * 60000;
        return new Date(date.getTime() - tzOffset).toISOString().slice(0, 19);
      };

      let fechaTransaccion = getLocalIsoString(new Date());
      if (this.form.fechaTransaccion) {
        const parts = this.form.fechaTransaccion.split('/');
        if (parts.length === 3) {
          const local = new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0]));
          fechaTransaccion = getLocalIsoString(local);
        } else {
          const parsed = new Date(this.form.fechaTransaccion);
          if (!Number.isNaN(parsed.getTime())) {
            fechaTransaccion = getLocalIsoString(parsed);
          }
        }
      }

      const payload: TransaccionRequestDTO = {
        usuarioId,
        nombreCliente: this.form.nombreGasto || 'Gasto sin nombre',
        monto: Number(this.form.monto),
        tipo: 'GASTO',
        categoriaId: catId,
        fechaTransaccion,
        metodoPago: this.form.metodoPago,
        etiquetas: this.form.etiquetas.join(','),
        descripcion: this.form.descripcion
      };

      this.transaccionesService.registrar(payload).subscribe({
        next: () => {
          this.guardando.set(false);
          this.stateService.invalidarCache();
          this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
          const catNombre = this.categoriaIAPendiente()?.nombre || this.nombreCategoriaPorId(catId);
          this.notificacionService.mostrarGastoRegistrado(Number(this.form.monto), catNombre);
          this.router.navigate(['/gastos']);
        },
        error: (err) => {
          this.guardando.set(false);
          console.error('Error al registrar Gasto:', err);
        }
      });
    };

    const pendingCat = this.categoriaIAPendiente();
    if (this.form.categoria === 'PENDIENTE_IA' && pendingCat) {
      this.guardando.set(true);
      this.financieroService.crearCategoria({
        nombre: pendingCat.nombre,
        descripcion: 'Categoría personalizada de Gastos recomendada por IA',
        icono: pendingCat.icono,
        tipo: 'GASTO'
      }).subscribe({
        next: (cat) => {
          this.stateService.categorias.update(cats => [...cats, cat]);
          this.form.categoria = cat.id;
          registrarGastoFinal(cat.id);
        },
        error: (err) => {
          this.guardando.set(false);
          console.error('Error al crear categoría de IA para Gasto:', err);
        }
      });
    } else {
      let catId = '';
      const selectedCat = this.form.categoria;
      const match = this.categorias.find(
        c => c.label.toLowerCase() === selectedCat.toLowerCase() || c.value === selectedCat
      );
      catId = match ? match.value : selectedCat;
      registrarGastoFinal(catId);
    }
  }

  cancelar(): void {
    this.router.navigate(['/Gastos']);
  }
}
