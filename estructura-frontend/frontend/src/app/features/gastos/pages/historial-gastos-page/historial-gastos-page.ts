import { CommonModule } from '@angular/common';
import { Component, computed, effect, inject, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { GastosStateService } from '../../../../core/services/gastos-state.service';

@Component({
  selector: 'app-historial-gastos-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './historial-gastos-page.html',
  styleUrl: '../gastos-page/gastos-page.scss',
})
export class HistorialGastosPage implements OnDestroy {
  private readonly eventBus = inject(AppEventBus);
  private readonly stateService = inject(GastosStateService);

  readonly terminoBusqueda = signal('');
  readonly filtroEstado = signal<'TODOS' | 'Pagado' | 'Pendiente'>('TODOS');
  readonly filtroMes = signal(new Date().getMonth() + 1);
  readonly meses = [
    { label: 'Enero', value: 1 },
    { label: 'Febrero', value: 2 },
    { label: 'Marzo', value: 3 },
    { label: 'Abril', value: 4 },
    { label: 'Mayo', value: 5 },
    { label: 'Junio', value: 6 },
    { label: 'Julio', value: 7 },
    { label: 'Agosto', value: 8 },
    { label: 'Septiembre', value: 9 },
    { label: 'Octubre', value: 10 },
    { label: 'Noviembre', value: 11 },
    { label: 'Diciembre', value: 12 },
  ];
  readonly gastos = computed(() => this.stateService.gastos());
  readonly usarMockVisualPagados = signal(true);
  readonly eliminadosIds = signal<string[]>([]);

  readonly pagadosMock = signal<Array<{
    id: string;
    nombre: string;
    detalle: string;
    categoria: string;
    fecha: string;
    hora: string;
    monto: number;
    metodo: string;
    estado: 'Pagado' | 'Pendiente';
    icono: string;
    colorCategoria: 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud';
  }>>([
    {
      id: 'mock-g1',
      nombre: 'Pizza Hut',
      detalle: 'Cena familiar de fin de semana',
      categoria: 'Restaurantes',
      fecha: new Date(Date.now() - 86400000).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
      hora: '20:15',
      monto: 85.00,
      metodo: 'TARJETA',
      estado: 'Pagado',
      icono: 'utensils',
      colorCategoria: 'comida'
    },
    {
      id: 'mock-g2',
      nombre: 'Uber',
      detalle: 'Traslado a la oficina',
      categoria: 'Transporte',
      fecha: new Date().toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
      hora: '08:30',
      monto: 18.50,
      metodo: 'DIGITAL',
      estado: 'Pagado',
      icono: 'bus',
      colorCategoria: 'transporte'
    },
    {
      id: 'mock-g3',
      nombre: 'Netflix',
      detalle: 'Suscripción mensual estándar',
      categoria: 'Entretenimiento',
      fecha: new Date(Date.now() - 86400000 * 3).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
      hora: '00:05',
      monto: 44.90,
      metodo: 'TARJETA',
      estado: 'Pagado',
      icono: 'film',
      colorCategoria: 'entretenimiento'
    },
    {
      id: 'mock-g4',
      nombre: 'Luz del Sur',
      detalle: 'Recibo de luz del mes',
      categoria: 'Servicios',
      fecha: new Date(Date.now() - 86400000 * 5).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
      hora: '14:20',
      monto: 120.00,
      metodo: 'TRANSFERENCIA',
      estado: 'Pagado',
      icono: 'bolt',
      colorCategoria: 'servicios'
    },
    {
      id: 'mock-g5',
      nombre: 'Plaza Vea',
      detalle: 'Compras de víveres para la semana',
      categoria: 'Alimentos',
      fecha: new Date(Date.now() - 86400000 * 7).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
      hora: '11:00',
      monto: 250.00,
      metodo: 'TARJETA',
      estado: 'Pagado',
      icono: 'utensils',
      colorCategoria: 'comida'
    }
  ]);

  readonly filasPagadas = computed(() => {
    const eliminados = new Set(this.eliminadosIds());
    const base = this.usarMockVisualPagados()
      ? this.pagadosMock()
      : this.gastos().map((g) => {
          const fecha = new Date(g.fechaTransaccion);
          const categoria = g.categoria || 'Otros';
          const nombre = g.nombreCliente || categoria;
          const detalle = g.descripcion || 'Gasto registrado';

          return {
            id: g.id,
            nombre,
            detalle,
            categoria,
            categoriaId: g.categoriaId,
            fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' }),
            hora: fecha.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' }),
            monto: Number(g.monto || 0),
            metodo: g.metodoPago || 'DIGITAL',
            estado: 'Pagado' as const,
            icono: g.categoriaIcono || this.iconoCategoria(categoria),
            colorCategoria: this.colorCategoria(categoria),
          };
        });

    return base.filter((g) => !eliminados.has(g.id));
  });

  readonly gastosFiltradosPagados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    const estado = this.filtroEstado();
    return this.filasPagadas().filter((gasto) =>
      (!q || gasto.nombre.toLowerCase().includes(q) || gasto.categoria.toLowerCase().includes(q) || gasto.metodo.toLowerCase().includes(q)) &&
      (estado === 'TODOS' || gasto.estado === estado)
    );
  });

  readonly totalFiltrado = computed(() =>
    this.gastosFiltradosPagados().reduce((acc, gasto) => acc + Number(gasto.monto || 0), 0)
  );

  private txSub?: any;

  constructor() {
    this.stateService.cargarDatos();
    effect(() => {
      this.usarMockVisualPagados.set(this.stateService.gastos().length === 0 && this.stateService.resumenActual() === null);
    });

    this.txSub = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      this.stateService.invalidarCache();
    });
  }

  ngOnDestroy(): void {
    this.txSub?.unsubscribe();
  }

  actualizarBusqueda(valor: string): void {
    this.terminoBusqueda.set(valor);
  }

  actualizarEstado(valor: string): void {
    this.filtroEstado.set(valor as 'TODOS' | 'Pagado' | 'Pendiente');
  }

  actualizarMes(valor: string): void {
    this.filtroMes.set(Number(valor));
  }

  editarGasto(id: string): void {
    void id;
  }

  eliminarGasto(id: string): void {
    this.eliminadosIds.update(ids => [...ids, id]);
  }

  private parseNotas(notas: string | null, fallbackCategoria: string): { nombre: string; detalle: string } {
    if (!notas) return { nombre: fallbackCategoria, detalle: 'Gasto registrado' };
    const partes = notas.split('|').map(p => p.trim()).filter(Boolean);
    return {
      nombre: partes[0] || fallbackCategoria,
      detalle: partes[1] || 'Gasto registrado',
    };
  }

  private iconoCategoria(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('comida') || key.includes('alimento')) return 'utensils';
    if (key.includes('transporte')) return 'bus';
    if (key.includes('servicio')) return 'bolt';
    if (key.includes('hogar')) return 'house';
    if (key.includes('salud')) return 'heart-pulse';
    return 'receipt';
  }

  private colorCategoria(categoria: string): 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud' {
    const key = categoria.toLowerCase();
    if (key.includes('comida') || key.includes('alimento')) return 'comida';
    if (key.includes('transporte')) return 'transporte';
    if (key.includes('servicio')) return 'servicios';
    if (key.includes('hogar')) return 'hogar';
    if (key.includes('salud')) return 'salud';
    return 'entretenimiento';
  }
}
