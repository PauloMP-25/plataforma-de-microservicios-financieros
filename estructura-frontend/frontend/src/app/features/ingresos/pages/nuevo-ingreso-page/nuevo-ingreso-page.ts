import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { IngresoFormComponent } from '../../components/ingreso-form/ingreso-form';
import { IngresoPreviewComponent } from '../../components/ingreso-preview/ingreso-preview';
import { IngresoRecentListComponent } from '../../components/ingreso-recent-list/ingreso-recent-list';
import { IngresosStateService } from '../../../../core/services/ingresos-state.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { IaService } from '../../../../core/services/ia.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { DistribucionCategoria, IngresoFormData, IngresoReciente, OptionItem } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-nuevo-ingreso-page',
  standalone: true,
  imports: [CommonModule, RouterLink, IngresoFormComponent, IngresoPreviewComponent, IngresoRecentListComponent],
  templateUrl: './nuevo-ingreso-page.html',
  styleUrl: './nuevo-ingreso-page.scss',
})
export class NuevoIngresoPage {
  private readonly stateService = inject(IngresosStateService);
  private readonly authService = inject(AuthService);
  private readonly transaccionesService = inject(Transacciones);
  private readonly financieroService = inject(FinancieroService);
  private readonly iaService = inject(IaService);
  private readonly eventBus = inject(AppEventBus);
  private readonly router = inject(Router);

  readonly metodos: OptionItem[] = [
    { label: 'Efectivo', value: 'EFECTIVO' },
    { label: 'Tarjeta', value: 'TARJETA' },
    { label: 'Transferencia', value: 'TRANSFERENCIA' },
    { label: 'Digital (Yape/Plin)', value: 'DIGITAL' },
  ];

  form: IngresoFormData = {
    monto: 1500,
    fechaTransaccion: new Date().toLocaleDateString('es-PE'), // dd/mm/yyyy
    descripcion: 'Pago mensual de la empresa ABC',
    categoria: 'Salario',
    metodoPago: 'TRANSFERENCIA',
    etiquetas: ['Trabajo', 'Mensual'],
  };

  readonly sugerenciasSignal = signal<string[]>([]);
  get sugerencias(): string[] { return this.sugerenciasSignal(); }

  // ── Signals computados para enlazar al estado real ──
  readonly categoriasSignal = computed<OptionItem[]>(() => {
    const cats = this.stateService.categorias();
    if (cats.length > 0) {
      // Si la categoría por defecto es 'Salario', la mapeamos al UUID correspondiente una vez cargado
      const match = cats.find(c => c.nombre.toLowerCase() === 'salario');
      if (match && this.form.categoria === 'Salario') {
        this.form.categoria = match.id;
      }
      return cats.map(c => ({ label: c.nombre, value: c.id }));
    }
    return [
      { label: 'Salario', value: 'salario' },
      { label: 'Freelance', value: 'freelance' },
      { label: 'Inversiones', value: 'inversion' },
      { label: 'Ventas', value: 'venta' },
      { label: 'Otros Ingresos', value: 'otros' },
    ];
  });

  readonly distribucionSignal = computed<DistribucionCategoria[]>(() => {
    const transacciones = this.stateService.ingresos();
    if (!transacciones.length) return [];
    const map = new Map<string, number>();
    let total = 0;
    for (const t of transacciones) {
      const cat = t.categoriaNombre || 'Otros';
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
        categoria: t.categoriaNombre || 'Otros',
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

  constructor() {
    this.stateService.cargarDatos();
  }

  onDescripcionChange(): void {
    const desc = this.form.descripcion?.trim();
    if (!desc || desc.length < 4) {
      this.sugerenciasSignal.set([]);
      return;
    }

    this.iaService.getClasificarTransaccion({
      id_temporal: 'nuevo-ingreso',
      tipo_movimiento: 'INGRESO',
      descripcion: desc,
      etiquetas: this.form.etiquetas.join(',')
    }).subscribe({
      next: (res) => {
        if (res.datos && res.datos.sugerencias) {
          this.sugerenciasSignal.set(res.datos.sugerencias);
        }
      },
      error: () => {
        // Fallback local
        const matched = ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Otros Ingresos'].filter(c => 
          c.toLowerCase().includes(desc.toLowerCase())
        );
        this.sugerenciasSignal.set(matched.length > 0 ? matched : ['Salario', 'Otros Ingresos']);
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

  crearCategoriaManualmente(nombre: string): void {
    const nameTrim = nombre.trim();
    if (!nameTrim) return;

    // Verificar si ya existe por nombre
    const match = this.categorias.find(
      c => c.label.toLowerCase() === nameTrim.toLowerCase()
    );
    if (match) {
      this.form.categoria = match.value;
      return;
    }

    // Crear la categoría en base de datos
    this.financieroService.crearCategoria({
      nombre: nameTrim,
      descripcion: 'Categoría personalizada de ingresos',
      icono: this.iconoCategoria(nameTrim),
      tipo: 'INGRESO'
    }).subscribe({
      next: (cat) => {
        // Añadirla reactivamente a la lista local
        this.stateService.categorias.update(cats => [...cats, cat]);
        // Pre-seleccionar el UUID
        this.form.categoria = cat.id;
      },
      error: (err) => {
        console.error('Error al crear categoría:', err);
      }
    });
  }

  seleccionarSugerencia(nombre: string): void {
    this.crearCategoriaManualmente(nombre);
  }

  guardar(): void {
    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      console.error('No se encontró sesión activa.');
      return;
    }

    // Buscar el UUID de la categoría
    let catId = '';
    const selectedCat = this.form.categoria;
    const match = this.categorias.find(
      c => c.label.toLowerCase() === selectedCat.toLowerCase() || c.value === selectedCat
    );
    catId = match ? match.value : selectedCat;

    // Normalizar la fecha del formulario 'dd/mm/yyyy' o similar a formato ISO
    let fechaTransaccion = new Date().toISOString();
    if (this.form.fechaTransaccion) {
      const parts = this.form.fechaTransaccion.split('/');
      if (parts.length === 3) {
        // dd/mm/yyyy -> yyyy-mm-dd
        fechaTransaccion = new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0])).toISOString();
      } else {
        const parsed = new Date(this.form.fechaTransaccion);
        if (!Number.isNaN(parsed.getTime())) {
          fechaTransaccion = parsed.toISOString();
        }
      }
    }

    const payload: TransaccionRequestDTO = {
      usuarioId,
      nombreCliente: this.authService.usuario()?.nombreUsuario ?? 'Cliente',
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
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        this.router.navigate(['/ingresos']);
      },
      error: (err) => {
        console.error('Error al registrar ingreso:', err);
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/ingresos']);
  }
}
