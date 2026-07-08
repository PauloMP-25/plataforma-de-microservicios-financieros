import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HasUnsavedChanges } from '../../../../core/guards/pending-changes.guard';
import { OnboardingTour, TourStep } from '../../../../shared/components/onboarding-tour/onboarding-tour';
import { IngresoFormComponent } from '../../components/ingreso-form/ingreso-form';
import { IngresoPreviewComponent } from '../../components/ingreso-preview/ingreso-preview';
import { IngresoRecentListComponent } from '../../components/ingreso-recent-list/ingreso-recent-list';
import { IngresosStateService } from '../../../../core/services/ingresos-state.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { IaService } from '../../../../core/services/ia.service';
import { CategoriaSugerida } from '../../../../core/models/ia_coach/ia-base.model';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { NotificacionService } from '../../../../core/services/notificacion.service';
import { DistribucionCategoria, IngresoFormData, IngresoReciente, OptionItem } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-nuevo-ingreso-page',
  standalone: true,
  imports: [CommonModule, RouterLink, IngresoFormComponent, IngresoPreviewComponent, IngresoRecentListComponent, OnboardingTour],
  templateUrl: './nuevo-ingreso-page.html',
  styleUrl: './nuevo-ingreso-page.scss',
})
export class NuevoIngresoPage implements HasUnsavedChanges, OnInit {
  private readonly stateService = inject(IngresosStateService);
  private readonly authService = inject(AuthService);
  private readonly transaccionesService = inject(Transacciones);
  private readonly financieroService = inject(FinancieroService);
  private readonly iaService = inject(IaService);
  private readonly eventBus = inject(AppEventBus);
  private readonly router = inject(Router);
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

  form: IngresoFormData = {
    nombreIngreso: '',
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
        { label: 'Otros Ingresos', value: 'otros' },
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
    const transacciones = this.stateService.ingresos();
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

  readonly recientesSignal = computed<IngresoReciente[]>(() => {
    const transacciones = this.stateService.ingresos();
    return transacciones.slice(0, 5).map(t => {
      const fecha = new Date(t.fechaTransaccion);
      return {
        categoria: t.categoria || 'Otros',
        descripcion: t.descripcion || t.notas || 'Ingreso registrado',
        monto: t.monto || 0,
        fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
      };
    });
  });

  // Getters para mantener bindings de la plantilla HTML
  get categorias(): OptionItem[] { return this.categoriasSignal(); }
  get distribucion(): DistribucionCategoria[] { return this.distribucionSignal(); }
  get recientes(): IngresoReciente[] { return this.recientesSignal(); }

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: 'input[name="monto"]',
      title: 'Monto del Ingreso',
      description: 'Ingresa la cantidad recibida en Soles (S/).',
      position: 'bottom'
    },
    {
      targetSelector: 'textarea[name="descripcion"]',
      title: 'Descripción del Ingreso',
      description: 'Describe brevemente de dónde proviene este dinero (ej: Salario, Venta, Freelance).',
      position: 'bottom'
    },
    {
      targetSelector: 'select[name="categoria"]',
      title: 'Categoría de Ingreso',
      description: 'Clasifica tu ingreso para organizar mejor el origen de tus flujos.',
      position: 'top'
    }
  ];

  ngOnInit(): void {
    const tourVisto = localStorage.getItem('luka_tour_nuevo_ingreso_visto');
    if (!tourVisto) {
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
  }

  completarTour(): void {
    localStorage.setItem('luka_tour_nuevo_ingreso_visto', 'true');
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
      id_temporal: 'nuevo-ingreso',
      tipo_movimiento: 'INGRESO',
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
        const matched = ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Otros Ingresos'].filter(c =>
          c.toLowerCase().includes(desc.toLowerCase())
        );
        const fallbackList: CategoriaSugerida[] = (matched.length > 0 ? matched : ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Otros Ingresos']).slice(0, 5).map(c => ({
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
      descripcion: 'Categoría personalizada de ingresos',
      icono: icono || this.iconoCategoria(nameTrim),
      tipo: 'INGRESO'
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
    return match ? match.label : 'Ingresos';
  }

  guardar(): void {
    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      console.error('No se encontró sesión activa.');
      return;
    }

    this.formularioGuardado = true;

    const registrarIngresoFinal = (catId: string) => {
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
        nombreCliente: this.form.nombreIngreso || 'Ingreso sin nombre',
        monto: Number(this.form.monto),
        tipo: 'INGRESO',
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
          this.notificacionService.mostrarIngresoRegistrado(Number(this.form.monto), catNombre);
          this.router.navigate(['/ingresos']);
        },
        error: (err) => {
          this.guardando.set(false);
          console.error('Error al registrar ingreso:', err);
        }
      });
    };

    const pendingCat = this.categoriaIAPendiente();
    if (this.form.categoria === 'PENDIENTE_IA' && pendingCat) {
      this.guardando.set(true);
      this.financieroService.crearCategoria({
        nombre: pendingCat.nombre,
        descripcion: 'Categoría personalizada de ingresos recomendada por IA',
        icono: pendingCat.icono,
        tipo: 'INGRESO'
      }).subscribe({
        next: (cat) => {
          this.stateService.categorias.update(cats => [...cats, cat]);
          this.form.categoria = cat.id;
          registrarIngresoFinal(cat.id);
        },
        error: (err) => {
          this.guardando.set(false);
          console.error('Error al crear categoría de IA para ingreso:', err);
        }
      });
    } else {
      let catId = '';
      const selectedCat = this.form.categoria;
      const match = this.categorias.find(
        c => c.label.toLowerCase() === selectedCat.toLowerCase() || c.value === selectedCat
      );
      catId = match ? match.value : selectedCat;
      registrarIngresoFinal(catId);
    }
  }

  cancelar(): void {
    this.router.navigate(['/ingresos']);
  }
}
