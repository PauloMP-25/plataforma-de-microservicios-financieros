import { Component, computed, inject, signal, effect, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Transacciones } from '../../../../core/services/transacciones';
import { MetodoPago, TransaccionDTO, TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { AuthService } from '../../../../core/services/auth.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { forkJoin } from 'rxjs';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { IaService } from '../../../../core/services/ia.service';
import { CategoriaSugerida } from '../../../../core/models/ia_coach/ia-base.model';
import { NotificacionService } from '../../../../core/services/notificacion.service';

@Component({
  selector: 'app-gastos-page',
  standalone:true,
  imports: [CommonModule, RouterLink],
  templateUrl: './gastos-page.html',
  styleUrl: './gastos-page.scss',
})
export class GastosPage implements OnDestroy {
  private readonly transaccionesService = inject(Transacciones);
  private readonly authService = inject(AuthService);
  private readonly financieroService = inject(FinancieroService);
  private readonly eventBus = inject(AppEventBus);
  private readonly stateService = inject(GastosStateService);
  private readonly iaService = inject(IaService);
  private readonly notificacionService = inject(NotificacionService);
  readonly iaSugeridaConfirmada = signal<boolean>(false);

  readonly sugerenciasIa = signal<CategoriaSugerida[]>([]);
  readonly sugerenciaSeleccionada = signal<CategoriaSugerida | null>(null);
  readonly categoriaIAPendiente = signal<{ nombre: string; icono: string } | null>(null);
  readonly clasificandoIa = signal(false);
  readonly intentosIaRestantes = computed(() => this.iaService.clasificacionesRestantes());
  readonly intentosIaMaximos = computed(() => this.iaService.clasificacionesMaximas());
  readonly puedeSugerirCategoriaIa = computed(() =>
    this.descripcion().trim().length >= 4 && !this.clasificandoIa() && this.intentosIaRestantes() > 0 && this.sugerenciasIa().length === 0
  );
  readonly cargando = computed(() => this.stateService.cargando());
  readonly terminoBusqueda = signal('');
  readonly tabActiva = signal<'todos' | 'pagados' | 'pendientes' | 'recurrentes'>('todos');
  readonly gastos = computed(() => this.stateService.gastos());
  readonly modalAbierto = signal(false);
  readonly guardandoGasto = signal(false);
  readonly mensajeFormulario = signal('');
  readonly gastoEditandoId = signal<string | null>(null);
  readonly gastoPendienteEliminar = signal<{ id: string; nombre: string } | null>(null);

  readonly categoria = signal('');
  readonly monto = signal('');
  readonly nombreGasto = signal('');
  readonly descripcion = signal('');
  readonly fecha = signal('');
  readonly metodoPago = signal<MetodoPago>('DIGITAL');
  readonly etiquetas = signal<string[]>([]);
  readonly nuevaEtiqueta = signal('');
  readonly filtroTendencia = signal<'7d' | '30d' | '90d'>('30d');
  readonly errores = signal<Record<string, string>>({});
  readonly eliminadosIds = signal<string[]>([]);

  readonly saldoActual = computed(() => Number(this.stateService.resumenActual()?.balance ?? 0));
  readonly totalGastadoActual = computed(() => Number(this.stateService.resumenActual()?.totalGastos ?? 0));
  readonly totalGastosAnterior = computed(() => Number(this.stateService.resumenAnterior()?.totalGastos ?? 0));
  readonly saldoAnterior = computed(() => Number(this.stateService.resumenAnterior()?.balance ?? 0));

  readonly variacionGastado = computed(() => this.calcularVariacion(this.totalGastadoActual(), this.totalGastosAnterior()));
  readonly variacionSaldo = computed(() => this.calcularVariacion(this.saldoActual(), this.saldoAnterior()));
  readonly variacionPendiente = signal(0);
  readonly bannerIntegracion = signal(
    'Integración en curso: historial de gastos (OK). Pendientes/Recurrentes dependen de Suscripciones (falta implementar backend).'
  );
  // TODO(backend): Implementar endpoint de Suscripciones para poblar Pendientes/Recurrentes.
  // TODO(backend): Implementar estado de pago de suscripción para habilitar “Marcar pagado”.

  readonly pendientesMock = signal<Array<{
    id: string;
    nombre: string;
    frecuencia: 'MENSUAL' | 'SEMANAL' | 'QUINCENAL';
    fechaVencimiento: string;
    monto: number;
    vencePronto: boolean;
    metodoPago: 'TARJETA' | 'DIGITAL' | 'TRANSFERENCIA';
    categoriaIcono: string;
  }>>([]);

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
  }>>([]);

  readonly usarMockVisualPagados = signal(true);

  get categoriasDisponibles(): any[] {
    return this.stateService.categorias().length > 0
      ? this.stateService.categorias()
      : [
          { id: 'alimentos', nombre: 'Alimentos' },
          { id: 'transporte', nombre: 'Transporte' },
          { id: 'servicios', nombre: 'Servicios' },
          { id: 'hogar', nombre: 'Hogar' },
          { id: 'otros', nombre: 'Otros' },
        ];
  }

  get categoriasConCrear(): any[] {
    const base = [...this.categoriasDisponibles];
    const pending = this.categoriaIAPendiente();
    if (pending) {
      if (!base.some(c => c.nombre.toLowerCase() === pending.nombre.toLowerCase())) {
        base.push({ id: 'PENDIENTE_IA', nombre: pending.nombre });
      }
    }
    return [
      ...base,
      { id: 'CREAR_NUEVA', nombre: '＋ Crear nueva categoría...' }
    ];
  }

  readonly metodosPagoDisponibles: Array<{ id: MetodoPago; nombre: string }> = [
    { id: 'DIGITAL', nombre: 'Digital (Yape/Plin)' },
    { id: 'TARJETA', nombre: 'Tarjeta' },
    { id: 'TRANSFERENCIA', nombre: 'Transferencia' },
    { id: 'EFECTIVO', nombre: 'Efectivo' },
  ];

  readonly totalGastado = computed(() =>
    this.filasPagadas().reduce((acc, gasto) => acc + Number(gasto.monto || 0), 0)
  );

  readonly totalPendiente = computed(() =>
    this.pendientesMock().reduce((acc, p) => acc + Number(p.monto || 0), 0)
  );
  readonly totalPagado = computed(() =>
    this.filasPagadas().filter((g) => g.estado === 'Pagado').reduce((acc, g) => acc + g.monto, 0)
  );
  readonly proximoVencimiento = computed(() => this.pendientesMock().find(() => true) ?? null);

  readonly gastosPorCategoria = computed(() => {
    const grupos = new Map<string, { categoria: string; total: number }>();
    for (const g of this.filasPagadas()) {
      const key = g.categoria || 'Otros';
      const prev = grupos.get(key);
      grupos.set(key, { categoria: key, total: (prev?.total ?? 0) + Number(g.monto || 0) });
    }

    const total = Array.from(grupos.values()).reduce((acc, item) => acc + item.total, 0);
    return Array.from(grupos.values())
      .sort((a, b) => b.total - a.total)
      .map((item) => ({
        ...item,
        porcentaje: total > 0 ? (item.total / total) * 100 : 0,
      }));
  });

  readonly tendenciaMensual = computed(() => {
    const meses = new Map<string, { etiqueta: string; total: number }>();
    for (const g of this.filasPagadas()) {
      const raw = g.fecha === 'Hoy' ? new Date() : new Date(`${g.fecha} ${new Date().getFullYear()}`);
      const fecha = Number.isNaN(raw.getTime()) ? new Date() : raw;
      const key = `${fecha.getFullYear()}-${fecha.getMonth()}`;
      const etiqueta = fecha.toLocaleDateString('es-PE', { month: 'short' });
      const prev = meses.get(key);
      meses.set(key, { etiqueta, total: (prev?.total ?? 0) + Number(g.monto || 0) });
    }

    const arr = Array.from(meses.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([, v]) => v);

    const max = Math.max(...arr.map((x) => x.total), 1);
    return arr.map((x) => ({ ...x, porcentaje: (x.total / max) * 100 }));
  });

  readonly donutCategorias = computed(() => {
    const colores = ['#f59e0b', '#10b981', '#6366f1', '#ec4899', '#94a3b8', '#14b8a6'];
    let offset = 100;
    return this.gastosPorCategoria().map((item, idx) => {
      const porcentaje = Math.max(0, Math.min(100, Number(item.porcentaje || 0)));
      const segmento = {
        ...item,
        color: colores[idx % colores.length],
        dasharray: `${porcentaje} ${Math.max(0, 100 - porcentaje)}`,
        dashoffset: offset,
      };
      offset -= porcentaje;
      return segmento;
    });
  });

  readonly totalDonutCategorias = computed(() =>
    this.gastosPorCategoria().reduce((acc, item) => acc + Number(item.total || 0), 0)
  );

  readonly tendenciaLineal = computed(() => {
    const data = this.tendenciaMensualFiltrada();
    if (!data.length) {
      return { puntos: '', etiquetas: [] as string[] };
    }
    const max = Math.max(...data.map((d) => d.total), 1);
    const n = data.length;
    const puntos = data
      .map((item, idx) => {
        const x = n === 1 ? 10 : 10 + (idx * 80) / (n - 1);
        const y = 90 - ((item.total / max) * 80);
        return `${x},${y}`;
      })
      .join(' ');

    return {
      puntos,
      etiquetas: data.map((d) => d.etiqueta),
    };
  });

  readonly tendenciaMensualFiltrada = computed(() => {
    const dias = this.filtroTendencia() === '7d' ? 7 : this.filtroTendencia() === '30d' ? 30 : 90;
    const hoy = new Date();
    const desde = new Date(hoy);
    desde.setDate(hoy.getDate() - dias);

    const totalesDia = new Map<string, number>();
    for (const g of this.filasPagadas()) {
      const fecha = this.parseFechaFila(g.fecha);
      if (fecha < desde || fecha > hoy) continue;
      const key = `${fecha.getFullYear()}-${fecha.getMonth()}-${fecha.getDate()}`;
      totalesDia.set(key, (totalesDia.get(key) ?? 0) + Number(g.monto || 0));
    }

    const step = dias === 90 ? 7 : 1;
    const salida: Array<{ fecha: Date; etiqueta: string; total: number }> = [];
    const cursor = new Date(desde);
    while (cursor <= hoy) {
      const key = `${cursor.getFullYear()}-${cursor.getMonth()}-${cursor.getDate()}`;
      salida.push({
        fecha: new Date(cursor),
        etiqueta: cursor.toLocaleDateString('es-PE', { day: '2-digit', month: 'short' }),
        total: totalesDia.get(key) ?? 0,
      });
      cursor.setDate(cursor.getDate() + step);
    }

    return salida;
  });

  readonly puntosTendencia = computed(() => {
    const data = this.tendenciaMensualFiltrada();
    if (!data.length) return [] as Array<{ x: number; y: number; etiqueta: string }>;
    const max = Math.max(...data.map((d) => d.total), 1);
    const n = data.length;
    return data.map((d, idx) => {
      const x = n === 1 ? 50 : 10 + (idx * 80) / (n - 1);
      const y = 82 - ((d.total / max) * 54);
      return { x, y, etiqueta: d.etiqueta };
    });
  });

  readonly topDiasGasto = computed(() => {
    const data = this.tendenciaMensualFiltrada()
      .filter((d) => d.total > 0)
      .sort((a, b) => b.total - a.total)
      .slice(0, 5);

    const max = Math.max(...data.map((d) => d.total), 1);
    return data.map((d) => ({
      ...d,
      porcentaje: (d.total / max) * 100,
    }));
  });

  readonly gastoPromedioMensual = computed(() => {
    const data = this.tendenciaMensual();
    if (!data.length) return 0;
    const total = data.reduce((acc, item) => acc + Number(item.total || 0), 0);
    return total / data.length;
  });

  readonly variacionPromedioMensual = computed(() => {
    const data = this.tendenciaMensual();
    if (data.length < 2) return 0;
    const actual = data[data.length - 1]?.total ?? 0;
    const previo = data[data.length - 2]?.total ?? 0;
    if (!previo) return 0;
    return ((actual - previo) / previo) * 100;
  });

  readonly gastosPendientes = computed(() => this.pendientesMock());
  readonly gastosPagados = computed(() => this.gastos());

  readonly filasPagadas = computed(() => {
    const eliminados = new Set(this.eliminadosIds());
    const base = this.usarMockVisualPagados()
      ? this.pagadosMock()
      : (() => {
          const data = this.gastosPagados();
          if (!data.length) {
            return [];
          }

          return data.map((g) => {
            const fecha = new Date(g.fechaTransaccion);
            const categoria = g.categoria || 'Otros';
            const { nombre, detalle } = this.parseNotas(g.notas, categoria);

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
        })();

    return base.filter((g) => !eliminados.has(g.id));
  });

  readonly gastosFiltradosPagados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    const tab = this.tabActiva();
    return this.filasPagadas().filter((gasto) => {
      const coincideBusqueda = !q || gasto.nombre.toLowerCase().includes(q) || gasto.categoria.toLowerCase().includes(q) || gasto.metodo.toLowerCase().includes(q);

      const coincideTab = tab === 'todos' || tab === 'pagados';
      return coincideBusqueda && coincideTab;
    });
  });

  readonly pendientesFiltrados = computed(() => {
    const q = this.terminoBusqueda().trim().toLowerCase();
    const tab = this.tabActiva();
    return this.gastosPendientes().filter((p) => {
      const coincideBusqueda =
        !q || p.nombre.toLowerCase().includes(q) || p.frecuencia.toLowerCase().includes(q);
      const coincideTab = tab === 'todos' || tab === 'pendientes' || tab === 'recurrentes';
      if (tab === 'recurrentes') {
        return coincideBusqueda && p.frecuencia !== 'SEMANAL';
      }
      return coincideBusqueda && coincideTab;
    });
  });

  private txSub?: any;

  constructor() {
    this.stateService.cargarDatos();
    effect(() => {
      if (this.stateService.gastos().length > 0) {
        this.usarMockVisualPagados.set(false);
      } else {
        this.usarMockVisualPagados.set(true);
      }
    });

    this.txSub = this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => {
      this.stateService.invalidarCache();
    });
  }

  ngOnDestroy(): void {
    if (this.txSub) {
      this.txSub.unsubscribe();
    }
  }

  seleccionarTab(tab: 'todos' | 'pagados' | 'pendientes' | 'recurrentes'): void {
    this.tabActiva.set(tab);
  }

  actualizarBusqueda(valor: string): void {
    this.terminoBusqueda.set(valor);
  }

  marcarPendienteComoPagado(id: string): void {
    const pendiente = this.pendientesMock().find((p) => p.id === id);
    if (!pendiente) {
      return;
    }

    this.pendientesMock.set(this.pendientesMock().filter((p) => p.id !== id));
    this.pagadosMock.update((items) => [
      {
        id: `mock-${pendiente.id}`,
        nombre: pendiente.nombre,
        detalle: 'Suscripción recurrente',
        categoria: 'Servicios',
        fecha: 'Hoy',
        hora: 'Ahora',
        monto: pendiente.monto,
        metodo: pendiente.metodoPago,
        estado: 'Pagado',
        icono: 'circle-check',
        colorCategoria: 'servicios',
      },
      ...items,
    ]);
    this.usarMockVisualPagados.set(true);
  }

  abrirModal(): void {
    this.resetFormulario();
    this.modalAbierto.set(true);
  }

  editarGasto(id: string): void {
    const gasto = this.filasPagadas().find((g) => g.id === id);
    if (!gasto) return;

    this.gastoEditandoId.set(id);
    this.nombreGasto.set(gasto.nombre);
    this.descripcion.set(gasto.detalle);
    this.monto.set(String(gasto.monto));
    this.fecha.set(this.fechaIsoDesdeTexto(gasto.fecha));
    this.metodoPago.set(this.normalizarMetodoPago(gasto.metodo));

    // Encontrar ID de categoría a partir de filas o por nombre como fallback
    let catId = (gasto as any).categoriaId || '';
    if (!catId) {
      const match = this.categoriasDisponibles.find(
        (c) => c.nombre.toLowerCase() === gasto.categoria.toLowerCase()
      );
      catId = match ? match.id : '';
    }
    this.categoria.set(catId);

    this.modalAbierto.set(true);
  }

  eliminarGasto(id: string): void {
    const gasto = this.filasPagadas().find((g) => g.id === id);
    this.gastoPendienteEliminar.set({ id, nombre: gasto?.nombre ?? 'este gasto' });
  }

  confirmarEliminarGasto(): void {
    const pendiente = this.gastoPendienteEliminar();
    if (!pendiente) return;
    const id = pendiente.id;

    if (id.startsWith('g') || id.startsWith('mock-')) {
      this.eliminadosIds.update((ids) => Array.from(new Set([...ids, id])));
      this.pagadosMock.update((items) => items.filter((i) => i.id !== id));
      this.usarMockVisualPagados.set(true);
      this.gastoPendienteEliminar.set(null);
      return;
    }

    this.transaccionesService.eliminar(id).subscribe({
      next: () => {
        this.gastoPendienteEliminar.set(null);
        this.stateService.invalidarCache();
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
      },
      error: () => this.mensajeFormulario.set('No se pudo eliminar el gasto.'),
    });
  }

  cancelarEliminarGasto(): void {
    this.gastoPendienteEliminar.set(null);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  private nombreCategoriaPorId(id: string): string {
    const match = this.categoriasDisponibles.find(c => c.id === id);
    return match ? match.nombre : 'Gastos';
  }

  guardarGasto(): void {
    const errores = this.validarFormulario();
    this.errores.set(errores);
    this.mensajeFormulario.set('');

    if (Object.keys(errores).length > 0) {
      return;
    }

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

    const usuarioId = this.authService.usuario()?.id;
    if (!usuarioId) {
      this.mensajeFormulario.set('No se encontró sesión activa.');
      return;
    }

    const registrarTransaccionFinal = (catId: string) => {
      this.guardandoGasto.set(true);
      const editId = this.gastoEditandoId();
      if (editId) {
        if (editId.startsWith('g') || editId.startsWith('mock-')) {
          this.pagadosMock.update((items) =>
            items.map((i) =>
              i.id !== editId
                ? i
                : {
                    ...i,
                    nombre: this.nombreGasto().trim(),
                    detalle: this.descripcion().trim(),
                    monto: Number(this.monto()),
                    metodo: this.metodoPago(),
                    fecha: this.fecha()
                      ? new Date(this.fecha()).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
                      : i.fecha,
                  }
            )
          );
          this.usarMockVisualPagados.set(true);
          this.modalAbierto.set(false);
          this.resetFormulario();
          this.guardandoGasto.set(false);
          this.notificacionService.mostrarGastoRegistrado(Number(this.monto()), this.nombreCategoriaPorId(catId));
          return;
        }

        const requestEdit: TransaccionRequestDTO = {
          usuarioId,
          nombreCliente: this.authService.usuario()?.nombreUsuario ?? 'Cliente',
          monto: Number(this.monto()),
          tipo: 'GASTO',
          categoriaId: catId || 'otros',
          fechaTransaccion: getLocalIsoString(this.fecha()),
          metodoPago: this.metodoPago(),
          notas: `${this.nombreGasto().trim()}|${this.descripcion().trim()}`,
          descripcion: this.descripcion().trim(),
          etiquetas: this.etiquetas().join(','),
        };

        this.transaccionesService.actualizar(editId, requestEdit).subscribe({
          next: () => {
            this.guardandoGasto.set(false);
            this.modalAbierto.set(false);
            this.resetFormulario();
            this.stateService.invalidarCache();
            this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
            this.notificacionService.mostrarGastoRegistrado(Number(this.monto()), this.nombreCategoriaPorId(catId));
          },
          error: () => {
            this.guardandoGasto.set(false);
            this.mensajeFormulario.set('No se pudo actualizar el gasto.');
          },
        });
      } else {
        const request: TransaccionRequestDTO = {
          usuarioId,
          nombreCliente: this.authService.usuario()?.nombreUsuario ?? 'Cliente',
          monto: Number(this.monto()),
          tipo: 'GASTO',
          categoriaId: catId,
          fechaTransaccion: getLocalIsoString(this.fecha()),
          metodoPago: this.metodoPago(),
          notas: `${this.nombreGasto().trim()}|${this.descripcion().trim()}`,
          descripcion: this.descripcion().trim(),
          etiquetas: this.etiquetas().join(','),
        };

        this.transaccionesService.registrar(request).subscribe({
          next: () => {
            this.guardandoGasto.set(false);
            this.modalAbierto.set(false);
            this.resetFormulario();
            this.stateService.invalidarCache();
            this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
            this.notificacionService.mostrarGastoRegistrado(Number(this.monto()), this.nombreCategoriaPorId(catId));
          },
          error: () => {
            this.guardandoGasto.set(false);
            this.mensajeFormulario.set('No se pudo registrar el gasto.');
          },
        });
      }
    };

    const pendingCat = this.categoriaIAPendiente();
    if (this.categoria() === 'PENDIENTE_IA' && pendingCat) {
      this.guardandoGasto.set(true);
      this.financieroService.crearCategoria({
        nombre: pendingCat.nombre,
        descripcion: 'Categoría personalizada de gastos recomendada por IA',
        icono: pendingCat.icono,
        tipo: 'GASTO'
      }).subscribe({
        next: (cat) => {
          this.stateService.categorias.update(cats => [...cats, cat]);
          this.categoria.set(cat.id);
          registrarTransaccionFinal(cat.id);
        },
        error: (err) => {
          this.guardandoGasto.set(false);
          this.mensajeFormulario.set('No se pudo crear la categoría recomendada por IA.');
          console.error(err);
        }
      });
    } else {
      registrarTransaccionFinal(this.categoria());
    }
  }


  clasificarConIa(): void {
    const d = this.descripcion().trim();
    if (!d || d.length < 4) {
      this.sugerenciasIa.set([]);
      return;
    }
    if (this.clasificandoIa() || this.intentosIaRestantes() <= 0) return;
    this.clasificandoIa.set(true);

    this.iaService.getClasificarTransaccion({
      id_temporal: 'nuevo-gasto',
      tipo_movimiento: 'GASTO',
      descripcion: d,
      etiquetas: this.etiquetas().join(',')
    }).subscribe({
      next: (res) => {
        this.clasificandoIa.set(false);
        if (res.datos) {
          const sugerencias = res.datos.sugerencias;
          if (sugerencias) {
            this.sugerenciasIa.set(sugerencias);
          }
        }
      },
      error: () => {
        this.clasificandoIa.set(false);
        const matched = ['Alimentos', 'Transporte', 'Servicios', 'Hogar', 'Salud', 'Educación', 'Entretenimiento'].filter(c =>
          c.toLowerCase().includes(d.toLowerCase())
        );
        const fallbackList: CategoriaSugerida[] = (matched.length > 0 ? matched : ['Alimentos', 'Transporte', 'Servicios', 'Hogar', 'Otros']).slice(0, 5).map(c => ({
          categoria: c,
          icono: this.iconoCategoria(c)
        }));
        this.sugerenciasIa.set(fallbackList);
      }
    });
  }

  agregarEtiqueta(): void {
    const raw = this.nuevaEtiqueta().trim();
    if (!raw) return;
    const tag = raw.split(' ')[0];
    if (!this.etiquetas().includes(tag)) {
      this.etiquetas.update(tags => [...tags, tag]);
    }
    this.nuevaEtiqueta.set('');
  }

  eliminarEtiqueta(tag: string): void {
    this.etiquetas.update(tags => tags.filter(t => t !== tag));
  }

  confirmarCrearCategoriaGasto(nombre: string, icono?: string): void {
    const nameTrim = nombre.trim();
    if (!nameTrim) return;

    const match = this.categoriasDisponibles.find(
      c => c.nombre.toLowerCase() === nameTrim.toLowerCase()
    );
    if (match) {
      this.categoria.set(match.id);
      return;
    }

    this.financieroService.crearCategoria({
      nombre: nameTrim,
      descripcion: 'Categoría personalizada de gastos',
      icono: icono || this.iconoCategoria(nameTrim),
      tipo: 'GASTO'
    }).subscribe({
      next: (cat) => {
        this.stateService.categorias.update(cats => [...cats, cat]);
        this.categoria.set(cat.id);
      },
      error: (err) => {
        console.error('Error al crear categoría de gasto:', err);
      }
    });
  }

  seleccionarSugerenciaGasto(sug: CategoriaSugerida): void {
    this.sugerenciaSeleccionada.set(sug);
  }

  confirmarSugerenciaGasto(): void {
    const sug = this.sugerenciaSeleccionada();
    if (!sug) return;

    const match = this.categoriasDisponibles.find(
      c => c.nombre.toLowerCase() === sug.categoria.toLowerCase()
    );
    if (match) {
      this.categoria.set(match.id);
      this.categoriaIAPendiente.set(null);
    } else {
      this.categoriaIAPendiente.set({ nombre: sug.categoria, icono: sug.icono });
      this.categoria.set('PENDIENTE_IA');
    }
    this.sugerenciaSeleccionada.set(null);
  }


  private validarFormulario(): Record<string, string> {
    const out: Record<string, string> = {};

    if (!this.categoria().trim()) {
      out['categoria'] = 'Selecciona una categoría.';
    }
    if (!this.monto().trim() || Number(this.monto()) <= 0) {
      out['monto'] = 'Ingresa un monto válido mayor a 0.';
    } else if (this.saldoActual() - Number(this.monto()) < 0) {
      out['monto'] = 'El gasto supera tu balance actual. Registra un ingreso primero.';
    }
    if (!this.nombreGasto().trim()) {
      out['nombreGasto'] = 'Ingresa el nombre del gasto.';
    }
    if (!this.descripcion().trim()) {
      out['descripcion'] = 'Ingresa una descripción del gasto.';
    }
    if (!this.fecha().trim()) {
      out['fecha'] = 'Selecciona una fecha.';
    }

    return out;
  }

  private resetFormulario(): void {
    this.gastoEditandoId.set(null);
    this.categoria.set('');
    this.monto.set('');
    this.nombreGasto.set('');
    this.descripcion.set('');
    this.fecha.set('');
    this.metodoPago.set('DIGITAL');
    this.etiquetas.set([]);
    this.nuevaEtiqueta.set('');
    this.errores.set({});
    this.mensajeFormulario.set('');
    this.sugerenciasIa.set([]);
  }

  private parseNotas(notas: string | null, fallbackCategoria: string): { nombre: string; detalle: string } {
    if (!notas) {
      return { nombre: fallbackCategoria, detalle: 'Transacción registrada' };
    }

    const [nombreRaw, detalleRaw] = notas.split('|');
    const nombre = nombreRaw?.trim() || fallbackCategoria;
    const detalle = detalleRaw?.trim() || 'Transacción registrada';
    return { nombre, detalle };
  }

  private parseFechaFila(fechaTexto: string): Date {
    if (fechaTexto === 'Hoy') return new Date();
    const normalizada = fechaTexto
      .toLowerCase()
      .replace('.', '')
      .replace('ene', 'jan')
      .replace('abr', 'apr')
      .replace('ago', 'aug')
      .replace('dic', 'dec');
    const dt = new Date(normalizada);
    if (!Number.isNaN(dt.getTime())) return dt;
    return new Date();
  }

  private fechaIsoDesdeTexto(fechaTexto: string): string {
    const dt = this.parseFechaFila(fechaTexto);
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private normalizarMetodoPago(valor: string): MetodoPago {
    const v = valor.toLowerCase();
    if (v.includes('efectivo')) return 'EFECTIVO';
    if (v.includes('transfer')) return 'TRANSFERENCIA';
    if (v.includes('tarjeta') || v.includes('crédito') || v.includes('débito')) return 'TARJETA';
    return 'DIGITAL';
  }

  private cargarGastos(): void {
    this.stateService.cargarDatos();
  }

  private calcularVariacion(actual: number, previo: number): number {
    if (!previo) return 0;
    return ((actual - previo) / Math.abs(previo)) * 100;
  }

  private iconoCategoria(categoria: string): string {
    const key = categoria.toLowerCase();
    if (key.includes('comida')) return 'utensils';
    if (key.includes('hogar')) return 'house';
    if (key.includes('transport')) return 'bus';
    if (key.includes('servicio')) return 'wifi';
    if (key.includes('entreten')) return 'film';
    if (key.includes('salud')) return 'briefcase-medical';
    return 'receipt';
  }

  private colorCategoria(categoria: string): 'comida' | 'hogar' | 'transporte' | 'servicios' | 'entretenimiento' | 'salud' {
    const key = categoria.toLowerCase();
    if (key.includes('comida')) return 'comida';
    if (key.includes('hogar')) return 'hogar';
    if (key.includes('transport')) return 'transporte';
    if (key.includes('servicio')) return 'servicios';
    if (key.includes('entreten')) return 'entretenimiento';
    return 'salud';
  }

}
