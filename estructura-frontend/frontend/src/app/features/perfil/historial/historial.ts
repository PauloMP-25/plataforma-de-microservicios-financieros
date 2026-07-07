import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GastosStateService } from '../../../core/services/gastos-state.service';
import { IngresosStateService } from '../../../core/services/ingresos-state.service';
import { Transacciones } from '../../../core/services/transacciones';
import { AppEventBus } from '../../../core/services/app-event-bus.service';
import { MetodoPago } from '../../../core/models/financiero/transaccion.model';
import { NotificacionService } from '../../../core/services/notificacion.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historial.html',
  styleUrls: ['./historial.scss']
})
export class Historial implements OnInit, OnDestroy {
  private readonly gastosState = inject(GastosStateService);
  private readonly ingresosState = inject(IngresosStateService);
  private readonly transaccionesService = inject(Transacciones);
  private readonly eventBus = inject(AppEventBus);
  private readonly notificacionService = inject(NotificacionService);
  private readonly authService = inject(AuthService);

  private txSub?: any;

  // Filtros
  readonly busqueda = signal('');
  readonly filtroEstado = signal<'TODOS' | 'INGRESO' | 'GASTO' | 'Pagado' | 'Pendiente'>('TODOS');
  readonly filtroMes = signal<number>(new Date().getMonth() + 1); // default to current month
  readonly filtroCategoria = signal<string>('TODOS');
  readonly orden = signal<string>('fecha_desc');
  readonly periodoSeleccionado = signal<'TODOS' | 'HOY' | '7_DIAS' | '15_DIAS' | '30_DIAS' | '90_DIAS'>('7_DIAS');
  readonly cargando = computed(() => this.gastosState.cargando() || this.ingresosState.cargando());

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

  // Data de prueba enriquecida con más movimientos y categorías
  readonly mockHistorial = signal<any[]>([]);

  // Modal de edición
  modalEdicionAbierto = signal(false);
  editandoMovimientoId = signal<string | null>(null);
  editandoMovimientoTipo = signal<'ingreso' | 'gasto'>('gasto');
  
  // Campos del formulario de edición
  nombreEdit = signal('');
  montoEdit = signal('');
  categoriaEdit = signal('');
  fechaEdit = signal('');
  metodoPagoEdit = signal<MetodoPago>('DIGITAL');
  descripcionEdit = signal('');

  // Modal de confirmación de eliminación
  movimientoAEliminar = signal<{ id: string; nombre: string; tipo: 'gasto' | 'ingreso' } | null>(null);

  readonly usarMockVisual = computed(() => false);

  readonly historialCombinado = computed(() => {
    if (this.usarMockVisual()) {
      return this.mockHistorial().map(item => ({
        ...item,
        fechaObjeto: new Date(item.fecha + 'T12:00:00')
      }));
    }

    const gList = this.gastosState.gastos().map(g => {
      const cat = g.categoria || 'Otros';
      return {
        id: g.id,
        nombre: g.nombreCliente || cat,
        detalle: g.descripcion || 'Gasto registrado',
        categoria: cat,
        monto: -Number(g.monto || 0),
        fecha: g.fechaTransaccion.substring(0, 10),
        fechaObjeto: new Date(g.fechaTransaccion),
        tipo: 'gasto',
        metodo: g.metodoPago || 'DIGITAL',
        estado: 'Pagado',
        icono: g.categoriaIcono || this.iconoCategoriaGasto(cat),
        colorCategoria: this.colorCategoriaGasto(cat)
      };
    });

    const iList = this.ingresosState.ingresos().map(i => {
      const cat = this.nombreCategoriaIngreso(i.categoria, i.categoriaId);
      return {
        id: i.id,
        nombre: i.nombreCliente || cat,
        detalle: i.descripcion || i.notas || 'Ingreso registrado',
        categoria: cat,
        monto: Number(i.monto || 0),
        fecha: i.fechaTransaccion.substring(0, 10),
        fechaObjeto: new Date(i.fechaTransaccion),
        tipo: 'ingreso',
        metodo: i.metodoPago || 'DIGITAL',
        estado: 'Recibido',
        icono: i.categoriaIcono || this.iconoCategoriaIngreso(cat),
        colorCategoria: this.colorCategoriaIngreso(cat)
      };
    });

    return [...gList, ...iList];
  });

  readonly categoriasDisponibles = computed(() => {
    const nombres = new Set<string>();
    for (const item of this.historialCombinado()) {
      if (item.categoria) {
        nombres.add(item.categoria);
      }
    }
    return Array.from(nombres).sort();
  });

  readonly categoriasEdicion = computed(() => {
    const tipo = this.editandoMovimientoTipo();
    const list = tipo === 'gasto' ? this.gastosState.categorias() : this.ingresosState.categorias();
    if (list.length > 0) {
      return list.map(c => c.nombre);
    }
    return tipo === 'gasto'
      ? ['Alimentos', 'Transporte', 'Servicios', 'Hogar', 'Salud', 'Entretenimiento', 'Otros']
      : ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Otros'];
  });

  readonly historialFiltrado = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const estado = this.filtroEstado();
    const mes = this.filtroMes();
    const cat = this.filtroCategoria();
    const ordenVal = this.orden();
    const periodo = this.periodoSeleccionado();

    let filtrados = this.historialCombinado().filter((item) => {
      const coincideBusqueda = !q || 
        item.nombre.toLowerCase().includes(q) || 
        item.categoria.toLowerCase().includes(q) || 
        item.metodo.toLowerCase().includes(q) ||
        (item.detalle && item.detalle.toLowerCase().includes(q));

      let coincideEstado = false;
      if (estado === 'TODOS') {
        coincideEstado = true;
      } else if (estado === 'INGRESO') {
        coincideEstado = item.tipo === 'ingreso';
      } else if (estado === 'GASTO') {
        coincideEstado = item.tipo === 'gasto';
      } else {
        coincideEstado = item.tipo === 'gasto' && item.estado === estado;
      }

      let coincideFecha = true;
      if (periodo === 'TODOS') {
        coincideFecha = item.fechaObjeto.getMonth() + 1 === mes;
      } else {
        const hoy = new Date();
        // Compare dates ignoring time
        const itemFecha = new Date(item.fechaObjeto);
        itemFecha.setHours(0, 0, 0, 0);
        
        const hoySinHora = new Date(hoy);
        hoySinHora.setHours(0, 0, 0, 0);
        
        const diffMs = hoySinHora.getTime() - itemFecha.getTime();
        const diffDays = diffMs / (1000 * 60 * 60 * 24);

        if (periodo === 'HOY') {
          coincideFecha = itemFecha.toDateString() === hoySinHora.toDateString();
        } else if (periodo === '7_DIAS') {
          coincideFecha = diffDays >= 0 && diffDays <= 7;
        } else if (periodo === '15_DIAS') {
          coincideFecha = diffDays >= 0 && diffDays <= 15;
        } else if (periodo === '30_DIAS') {
          coincideFecha = diffDays >= 0 && diffDays <= 30;
        } else if (periodo === '90_DIAS') {
          coincideFecha = diffDays >= 0 && diffDays <= 90;
        }
      }

      const coincideCat = cat === 'TODOS' || item.categoria === cat;

      return coincideBusqueda && coincideEstado && coincideFecha && coincideCat;
    });

    return filtrados.sort((a, b) => {
      if (ordenVal === 'fecha_desc') return b.fechaObjeto.getTime() - a.fechaObjeto.getTime();
      if (ordenVal === 'fecha_asc') return a.fechaObjeto.getTime() - b.fechaObjeto.getTime();

      if (ordenVal === 'mayor_gasto' || ordenVal === 'mayor_monto') {
        return Math.abs(b.monto) - Math.abs(a.monto);
      }
      if (ordenVal === 'menor_gasto' || ordenVal === 'menor_monto') {
        return Math.abs(a.monto) - Math.abs(b.monto);
      }

      if (ordenVal === 'mayor_ingreso') {
        const aVal = a.tipo === 'ingreso' ? a.monto : 0;
        const bVal = b.tipo === 'ingreso' ? b.monto : 0;
        return bVal - aVal;
      }
      if (ordenVal === 'menor_ingreso') {
        const aVal = a.tipo === 'ingreso' ? a.monto : 0;
        const bVal = b.tipo === 'ingreso' ? b.monto : 0;
        return aVal - bVal;
      }

      return 0;
    });
  });

  readonly balance = computed(() => {
    return this.historialFiltrado().reduce((acc, item) => acc + item.monto, 0);
  });

  readonly sumaIngresos = computed(() => {
    return this.historialFiltrado()
      .filter(item => item.tipo === 'ingreso')
      .reduce((acc, item) => acc + item.monto, 0);
  });

  readonly sumaGastos = computed(() => {
    return this.historialFiltrado()
      .filter(item => item.tipo === 'gasto')
      .reduce((acc, item) => acc + Math.abs(item.monto), 0);
  });

  ngOnInit(): void {
    const todayStr = new Date().toISOString().substring(0, 10);
    const yesterdayStr = new Date(Date.now() - 86400000).toISOString().substring(0, 10);
    const twoDaysAgoStr = new Date(Date.now() - 2 * 86400000).toISOString().substring(0, 10);
    const threeDaysAgoStr = new Date(Date.now() - 3 * 86400000).toISOString().substring(0, 10);

    this.mockHistorial.set([
      { id: 'mock-1', nombre: 'Sueldo mensual', detalle: 'Empresa XYZ', categoria: 'Salario', monto: 3000.00, fecha: todayStr, tipo: 'ingreso', metodo: 'TRANSFERENCIA', estado: 'Recibido', icono: 'briefcase', colorCategoria: 'salario' },
      { id: 'mock-2', nombre: 'Supermercado Plaza Vea', detalle: 'Compras de la semana', categoria: 'Alimentación', monto: -85.60, fecha: todayStr, tipo: 'gasto', metodo: 'TARJETA', estado: 'Pagado', icono: 'basket-shopping', colorCategoria: 'alimentacion' },
      { id: 'mock-3', nombre: 'Transporte a oficina', detalle: 'Uber de ida', categoria: 'Transporte', monto: -6.00, fecha: todayStr, tipo: 'gasto', metodo: 'EFECTIVO', estado: 'Pagado', icono: 'car', colorCategoria: 'transporte' },
      { id: 'mock-4', nombre: 'Freelance - Diseño web', detalle: 'Proyecto Cliente A', categoria: 'Trabajo Freelance', monto: 850.00, fecha: yesterdayStr, tipo: 'ingreso', metodo: 'DIGITAL', estado: 'Recibido', icono: 'laptop-code', colorCategoria: 'freelance' },
      { id: 'mock-5', nombre: 'Restaurante La Trattoria', detalle: 'Cena familiar', categoria: 'Salidas', monto: -120.00, fecha: yesterdayStr, tipo: 'gasto', metodo: 'TARJETA', estado: 'Pagado', icono: 'utensils', colorCategoria: 'salidas' },
      { id: 'mock-6', nombre: 'Pago de luz', detalle: 'Enel recibo mensual', categoria: 'Servicios', monto: -65.90, fecha: yesterdayStr, tipo: 'gasto', metodo: 'TRANSFERENCIA', estado: 'Pagado', icono: 'file-invoice-dollar', colorCategoria: 'servicios' },
      { id: 'mock-7', nombre: 'Venta de libro usado', detalle: 'Libro de cálculo', categoria: 'Venta', monto: 45.00, fecha: twoDaysAgoStr, tipo: 'ingreso', metodo: 'EFECTIVO', estado: 'Recibido', icono: 'tag', colorCategoria: 'venta' },
      { id: 'mock-8', nombre: 'Netflix', detalle: 'Plan mensual familiar', categoria: 'Entretenimiento', monto: -44.90, fecha: threeDaysAgoStr, tipo: 'gasto', metodo: 'TARJETA', estado: 'Pagado', icono: 'film', colorCategoria: 'entretenimiento' }
    ]);

    this.gastosState.cargarDatos();
    this.ingresosState.cargarDatos();

    this.txSub = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      this.gastosState.invalidarCache();
      this.ingresosState.invalidarCache();
    });
  }

  ngOnDestroy(): void {
    if (this.txSub) {
      this.txSub.unsubscribe();
    }
  }

  // Métodos de operaciones
  abrirEditarMovimiento(item: any): void {
    this.editandoMovimientoId.set(item.id);
    this.editandoMovimientoTipo.set(item.tipo);
    this.nombreEdit.set(item.nombre);
    this.montoEdit.set(String(Math.abs(item.monto)));
    this.categoriaEdit.set(item.categoria);
    this.fechaEdit.set(item.fecha);
    this.metodoPagoEdit.set(item.metodo);
    this.descripcionEdit.set(item.detalle || '');
    this.modalEdicionAbierto.set(true);
  }

  cerrarModalEdicion(): void {
    this.modalEdicionAbierto.set(false);
    this.editandoMovimientoId.set(null);
  }

  guardarEdicion(): void {
    const id = this.editandoMovimientoId();
    const tipo = this.editandoMovimientoTipo();
    if (!id) return;

    if (id.startsWith('mock-') || this.usarMockVisual()) {
      this.mockHistorial.update((items) =>
        items.map((i) =>
          i.id !== id
            ? i
            : {
                ...i,
                nombre: this.nombreEdit().trim(),
                detalle: this.descripcionEdit().trim(),
                monto: tipo === 'gasto' ? -Number(this.montoEdit()) : Number(this.montoEdit()),
                metodo: this.metodoPagoEdit(),
                fecha: this.fechaEdit(),
                categoria: this.categoriaEdit(),
                icono: tipo === 'gasto' ? this.iconoCategoriaGasto(this.categoriaEdit()) : this.iconoCategoriaIngreso(this.categoriaEdit()),
                colorCategoria: tipo === 'gasto' ? this.colorCategoriaGasto(this.categoriaEdit()) : this.colorCategoriaIngreso(this.categoriaEdit())
              }
        )
      );
      this.cerrarModalEdicion();
      this.notificacionService.mostrarDatosGuardados('Mock de transacción actualizado.');
      return;
    }

    let catId = '';
    const nameTrim = this.categoriaEdit().trim().toLowerCase();
    const stateCats = tipo === 'gasto' ? this.gastosState.categorias() : this.ingresosState.categorias();
    const match = stateCats.find(c => c.nombre.toLowerCase() === nameTrim);
    catId = match ? match.id : this.categoriaEdit();

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

    const request: any = {
      usuarioId: this.authService.usuario()?.id || 'default-user',
      nombreCliente: this.nombreEdit().trim(),
      monto: Number(this.montoEdit()),
      tipo: tipo === 'gasto' ? 'GASTO' : 'INGRESO',
      categoriaId: catId,
      fechaTransaccion: getLocalIsoString(this.fechaEdit()),
      metodoPago: this.metodoPagoEdit(),
      descripcion: this.descripcionEdit().trim(),
      etiquetas: ''
    };

    this.transaccionesService.actualizar(id, request).subscribe({
      next: () => {
        this.cerrarModalEdicion();
        this.gastosState.invalidarCache();
        this.ingresosState.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        this.notificacionService.mostrarDatosGuardados('Transacción actualizada con éxito.');
      },
      error: () => {
        this.notificacionService.mostrar('Error', 'No se pudo actualizar la transacción.', 'login', 'triangle-exclamation');
      }
    });
  }

  solicitarEliminarMovimiento(item: any): void {
    this.movimientoAEliminar.set({ id: item.id, nombre: item.nombre, tipo: item.tipo });
  }

  cancelarEliminar(): void {
    this.movimientoAEliminar.set(null);
  }

  confirmarEliminar(): void {
    const target = this.movimientoAEliminar();
    if (!target) return;

    if (target.id.startsWith('mock-') || this.usarMockVisual()) {
      this.mockHistorial.update(items => items.filter(m => m.id !== target.id));
      this.movimientoAEliminar.set(null);
      this.notificacionService.mostrarDatosGuardados('Movimiento mock eliminado.');
      return;
    }

    this.transaccionesService.eliminar(target.id).subscribe({
      next: () => {
        this.movimientoAEliminar.set(null);
        this.gastosState.invalidarCache();
        this.ingresosState.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        this.notificacionService.mostrarDatosGuardados('El movimiento ha sido eliminado.');
      },
      error: () => {
        this.movimientoAEliminar.set(null);
        this.notificacionService.mostrar('Error', 'No se pudo eliminar el movimiento.', 'login', 'triangle-exclamation');
      }
    });
  }

  // Helpers de categorias
  private nombreCategoriaIngreso(categoria?: string | null, categoriaId?: string | null): string {
    const nombre = categoria?.trim();
    if (nombre && !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(nombre)) {
      return nombre;
    }
    const id = categoriaId || nombre;
    const match = this.ingresosState.categorias().find(cat => cat.id === id);
    return match?.nombre ?? 'Otros';
  }

  private iconoCategoriaGasto(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('comida') || key.includes('restaurante')) return 'utensils';
    if (key.includes('hogar')) return 'house';
    if (key.includes('transport')) return 'bus';
    if (key.includes('servicio')) return 'wifi';
    if (key.includes('entreten')) return 'film';
    if (key.includes('salud')) return 'briefcase-medical';
    return 'receipt';
  }

  private colorCategoriaGasto(categoria: string): 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud' {
    const key = categoria.toLowerCase();
    if (key.includes('comida') || key.includes('restaurante')) return 'comida';
    if (key.includes('hogar')) return 'hogar';
    if (key.includes('transport')) return 'transporte';
    if (key.includes('servicio')) return 'servicios';
    if (key.includes('entreten')) return 'entretenimiento';
    return 'salud';
  }

  private iconoCategoriaIngreso(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('salario') || key.includes('sueldo')) return 'briefcase';
    if (key.includes('freelance')) return 'code';
    if (key.includes('invers')) return 'trending-up';
    if (key.includes('venta')) return 'tag';
    return 'coins';
  }

  private colorCategoriaIngreso(categoria: string): 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud' {
    const key = categoria.toLowerCase();
    if (key.includes('salario') || key.includes('sueldo')) return 'servicios';
    if (key.includes('freelance')) return 'transporte';
    if (key.includes('invers')) return 'hogar';
    return 'comida';
  }

  obtenerDiferenciaDias(fechaStr: string): number {
    if (!fechaStr) return -1;
    const parts = fechaStr.split('-');
    if (parts.length !== 3) return -1;
    const dateTx = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
    const today = new Date();
    const dateToday = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const diffMs = dateToday.getTime() - dateTx.getTime();
    return Math.floor(diffMs / (1000 * 60 * 60 * 24));
  }

  formatearMetodo(metodo: string): string {
    if (!metodo) return '';
    const map: Record<string, string> = {
      TARJETA: 'Tarjeta',
      DIGITAL: 'Digital',
      TRANSFERENCIA: 'Transferencia',
      EFECTIVO: 'Efectivo'
    };
    return map[metodo.toUpperCase()] ?? (metodo.charAt(0).toUpperCase() + metodo.slice(1).toLowerCase());
  }

  limpiarFiltros(): void {
    this.busqueda.set('');
    this.filtroEstado.set('TODOS');
    this.filtroMes.set(new Date().getMonth() + 1);
    this.filtroCategoria.set('TODOS');
    this.orden.set('fecha_desc');
    this.periodoSeleccionado.set('7_DIAS');
  }
}