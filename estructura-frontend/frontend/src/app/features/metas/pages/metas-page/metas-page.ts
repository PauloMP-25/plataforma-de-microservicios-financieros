import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ClienteMetasLimitesService } from '../../../../core/services/cliente-metas-limites.service';
import { RespuestaMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { Transacciones } from '../../../../core/services/transacciones';
import { AuthService } from '../../../../core/services/auth.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-metas-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './metas-page.html',
  styleUrl: './metas-page.scss',
})
export class MetasPage implements OnInit {
  private router = inject(Router);
  private metasService = inject(ClienteMetasLimitesService);
  private financieroService = inject(FinancieroService);
  private transaccionesService = inject(Transacciones);
  private authService = inject(AuthService);
  private eventBus = inject(AppEventBus);

  // Estado
  metas = signal<RespuestaMetaAhorro[]>([]);
  cargando = signal<boolean>(false);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');

  // Paginación
  paginaActual = signal<number>(0);
  totalPaginas = signal<number>(1);
  tamanioPagina = 10;

  // Modales y Paneles
  metaSeleccionada = signal<any | null>(null);
  modalConfirmarCompletar = signal<any | null>(null);

  // Filtros reactivos
  filtroEstado = signal<string>('Todas');
  filtroMes = signal<string>('Todos');
  filtroAnio = signal<string>('Todos');
  filtroMontoMin = signal<number | null>(null);
  filtroMontoMax = signal<number | null>(null);


  // Exponer Math para la plantilla HTML
  protected readonly Math = Math;

  // Lista de categorías con sus íconos correspondientes
  readonly categorias = [
    { id: 'Viaje', nombre: 'Viaje', icono: 'fa-solid fa-plane' },
    { id: 'Vivienda', nombre: 'Vivienda', icono: 'fa-solid fa-house' },
    { id: 'Auto', nombre: 'Auto', icono: 'fa-solid fa-car' },
    { id: 'Estudios', nombre: 'Estudios', icono: 'fa-solid fa-graduation-cap' },
    { id: 'Tecnología', nombre: 'Tecnología', icono: 'fa-solid fa-laptop' },
    { id: 'Emergencia', nombre: 'Emergencia', icono: 'fa-solid fa-piggy-bank' },
    { id: 'Otros', nombre: 'Otros', icono: 'fa-solid fa-bullseye' }
  ];

  // Ahorro disponible cargado desde FinancieroService (balance general)
  ahorroDisponible = computed(() => {
    const resumen = this.financieroService.resumen();
    return resumen ? resumen.balance : 0;
  });

  // Cálculo secuencial/híbrido del avance de las metas activas usando el saldo restante
  metasCalculadas = computed(() => {
    const listado = this.metas();
    const disponibleGlobal = this.ahorroDisponible();

    const completadas = listado.filter(m => m.completada);
    const activas = listado.filter(m => !m.completada);

    // Ordenar activas por fecha límite más cercana (prioridad de llenado)
    const activasOrdenadas = [...activas].sort((a, b) => {
      if (!a.fechaLimite) return 1;
      if (!b.fechaLimite) return -1;
      return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
    });

    let saldoRestante = disponibleGlobal;

    const activasCalculadas = activasOrdenadas.map(meta => {
      const datosVisuales = this.obtenerCategoriaYNombre(meta.nombre);
      const nombreVisual = meta.proposito ? meta.nombre : datosVisuales.nombre;
      const categoriaVisual = meta.proposito || datosVisuales.categoria || 'Otros';
      const iconoVisual = this.obtenerIconoCategoria(categoriaVisual);

      const faltante = meta.montoObjetivo;
      const adicionalAplicado = Math.min(faltante, saldoRestante);

      const montoAplicado = adicionalAplicado;
      saldoRestante = Math.max(0, saldoRestante - adicionalAplicado);

      const porcentaje = meta.montoObjetivo > 0 
        ? (montoAplicado / meta.montoObjetivo) * 100 
        : 0;

      return {
        ...meta,
        nombreVisual,
        categoriaVisual,
        iconoVisual,
        montoAplicado: montoAplicado,
        porcentajeProgreso: Math.min(100, porcentaje),
        puedeCompletar: porcentaje >= 100
      };
    });

    const completadasMapeadas = completadas.map(meta => {
      const datosVisuales = this.obtenerCategoriaYNombre(meta.nombre);
      const nombreVisual = meta.proposito ? meta.nombre : datosVisuales.nombre;
      const categoriaVisual = meta.proposito || datosVisuales.categoria || 'Otros';
      const iconoVisual = this.obtenerIconoCategoria(categoriaVisual);

      return {
        ...meta,
        nombreVisual,
        categoriaVisual,
        iconoVisual,
        montoAplicado: meta.montoObjetivo,
        porcentajeProgreso: 100,
        puedeCompletar: false
      };
    });

    return [...completadasMapeadas, ...activasCalculadas];
  });

  // Metas después de aplicar los filtros del frontend
  metasFiltradas = computed(() => {
    let listado = this.metasCalculadas();

    // Filtro por Estado
    const est = this.filtroEstado();
    if (est !== 'Todas') {
      if (est === 'Activas') {
        listado = listado.filter(m => !m.completada && !this.esVencida(m));
      } else if (est === 'Cumplidas') {
        listado = listado.filter(m => m.completada);
      } else if (est === 'Vencidas') {
        listado = listado.filter(m => !m.completada && this.esVencida(m));
      }
    }

    // Filtro por Mes
    const mes = this.filtroMes();
    if (mes !== 'Todos') {
      const mesNum = parseInt(mes, 10);
      listado = listado.filter(m => {
        if (!m.fechaLimite) return false;
        const date = new Date(m.fechaLimite + 'T00:00:00');
        return date.getMonth() === mesNum;
      });
    }

    // Filtro por Año
    const anio = this.filtroAnio();
    if (anio !== 'Todos') {
      listado = listado.filter(m => {
        if (!m.fechaLimite) return false;
        const date = new Date(m.fechaLimite + 'T00:00:00');
        return date.getFullYear() === parseInt(anio, 10);
      });
    }

    // Filtro por Rango Mínimo
    const min = this.filtroMontoMin();
    if (min !== null && min >= 0) {
      listado = listado.filter(m => m.montoObjetivo >= min);
    }

    // Filtro por Rango Máximo
    const max = this.filtroMontoMax();
    if (max !== null && max >= 0) {
      listado = listado.filter(m => m.montoObjetivo <= max);
    }

    return listado;
  });


  // KPIs
  metasActivasCount = computed(() => this.metasCalculadas().filter(m => !m.completada).length);
  metasCumplidasCount = computed(() => this.metasCalculadas().filter(m => m.completada).length);
  
  metaMasCercana = computed(() => {
    const activas = this.metasCalculadas().filter(m => !m.completada);
    if (activas.length === 0) return null;
    return [...activas].sort((a, b) => {
      if (!a.fechaLimite) return 1;
      if (!b.fechaLimite) return -1;
      return new Date(a.fechaLimite).getTime() - new Date(b.fechaLimite).getTime();
    })[0];
  });

  ngOnInit(): void {
    this.financieroService.cargarResumen();
    this.cargarMetas();
  }

  cargarMetas(): void {
    this.cargando.set(true);
    this.errorMensaje.set('');

    this.metasService.listarMetas(this.paginaActual(), this.tamanioPagina).subscribe({
      next: (pagina) => {
        if (pagina && pagina.content) {
          this.metas.set(pagina.content);
          this.totalPaginas.set(pagina.totalPages || 1);
        } else {
          this.metas.set([]);
          this.totalPaginas.set(1);
        }
        this.cargando.set(false);

        // Actualizar detalle
        const seleccionada = this.metaSeleccionada();
        if (seleccionada) {
          const fresca = this.metasCalculadas().find(m => m.id === seleccionada.id);
          if (fresca) this.metaSeleccionada.set(fresca);
        }
      },
      error: (err) => {
        console.error('Error al recuperar metas de la API:', err);
        this.errorMensaje.set('Hubo un error al cargar tus metas. Por favor, intenta de nuevo.');
        this.metas.set([]);
        this.totalPaginas.set(1);
        this.cargando.set(false);
      }
    });
  }

  paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.paginaActual.update(p => p - 1);
      this.cargarMetas();
    }
  }

  paginaSiguiente(): void {
    if (this.paginaActual() < this.totalPaginas() - 1) {
      this.paginaActual.update(p => p + 1);
      this.cargarMetas();
    }
  }

  // Descomponer prefijo del nombre
  obtenerCategoriaYNombre(metaNombre: string): { categoria: string; nombre: string; icono: string } {
    const match = metaNombre.match(/^\[(.*?)\] (.*)$/);
    if (match) {
      const cat = match[1];
      const nom = match[2];
      return {
        categoria: cat,
        nombre: nom,
        icono: this.obtenerIconoCategoria(cat)
      };
    }
    return {
      categoria: 'Otros',
      nombre: metaNombre,
      icono: this.obtenerIconoCategoria('Otros')
    };
  }

  obtenerIconoCategoria(catId: string): string {
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? cat.icono : 'fa-solid fa-bullseye';
  }

  obtenerColorEstado(meta: any): string {
    if (meta.completada) return 'success';
    if (this.esVencida(meta)) return 'danger';
    return 'active';
  }

  obtenerTextoEstado(meta: any): string {
    if (meta.completada) return 'Cumplida';
    if (this.esVencida(meta)) return 'Vencida';
    return 'Activa';
  }

  formatMoneda(valor: number): string {
    if (valor === null || valor === undefined) return '0.00';
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  obtenerColorProgreso(meta: any): string {
    if (meta.completada) return '#22c55e';
    if (this.esVencida(meta)) return '#ef4444';
    return '#5b6af0';
  }

  esVencida(meta: RespuestaMetaAhorro): boolean {
    if (!meta.fechaLimite) return false;
    const limite = new Date(meta.fechaLimite + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return limite < hoy;
  }

  calcularDiasRestantes(fechaLimiteStr: string): number {
    if (!fechaLimiteStr) return 0;
    const limite = new Date(fechaLimiteStr + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const dif = limite.getTime() - hoy.getTime();
    return Math.max(0, Math.ceil(dif / (1000 * 60 * 60 * 24)));
  }

  calcularTiempoEmpleado(creacion: string, actualizacion: string): string {
    const inicio = new Date(creacion);
    const fin = new Date(actualizacion);
    const difAnios = fin.getFullYear() - inicio.getFullYear();
    const difMeses = fin.getMonth() - inicio.getMonth() + (difAnios * 12);
    
    if (difMeses <= 0) {
      const difDias = Math.ceil((fin.getTime() - inicio.getTime()) / (1000 * 60 * 60 * 24));
      return `${difDias} días`;
    }
    return `${difMeses} meses`;
  }

  obtenerTiempoEmpleadoMeta(meta: RespuestaMetaAhorro): string {
    const fin = meta.fechaActualizacion || meta.fechaCreacion;
    return this.calcularTiempoEmpleado(meta.fechaCreacion, fin);
  }

  obtenerFechaActualizacionOCreacion(meta: RespuestaMetaAhorro): string {
    return meta.fechaActualizacion || meta.fechaCreacion;
  }

  abrirCrearMeta(): void {
    this.router.navigate(['/metas/nueva']);
  }

  abrirEditarMeta(meta: any): void {
    this.router.navigate(['/metas/editar', meta.id]);
  }

  eliminarMeta(metaId: string): void {
    if (!confirm('¿Estás seguro de que deseas eliminar esta meta de ahorro? Todo el progreso acumulado se perderá.')) {
      return;
    }

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    this.metasService.eliminarMeta(metaId).subscribe({
      next: () => {
        this.removerMockLocalmente(metaId);
        this.exitoMensaje.set('Meta de ahorro eliminada con éxito.');
        if (this.metaSeleccionada()?.id === metaId) {
          this.metaSeleccionada.set(null);
        }
        this.cargarMetas();
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      },
      error: () => {
        this.removerMockLocalmente(metaId);
        this.metas.update(items => items.filter(i => i.id !== metaId));
        if (this.metaSeleccionada()?.id === metaId) {
          this.metaSeleccionada.set(null);
        }
        this.exitoMensaje.set('Meta de ahorro eliminada con éxito (Modo Pruebas).');
        this.cargando.set(false);
        setTimeout(() => this.exitoMensaje.set(''), 4000);
      }
    });
  }

  private removerMockLocalmente(id: string): void {
    const localMetasStr = localStorage.getItem('luka_mock_metas');
    if (localMetasStr) {
      try {
        let lista = JSON.parse(localMetasStr);
        lista = lista.filter((m: any) => m.id !== id);
        localStorage.setItem('luka_mock_metas', JSON.stringify(lista));
      } catch (e) {
        console.error(e);
      }
    }
  }

  // Selección para panel de detalle
  seleccionarMeta(meta: any, event?: Event): void {
    if (event && window.innerWidth < 1024 && !(event.target as HTMLElement).closest('.card-options-btn')) {
      return;
    }
    this.metaSeleccionada.set(meta);
  }

  cerrarDetalle(): void {
    this.metaSeleccionada.set(null);
  }

  // Apertura del modal de confirmación de completado
  solicitarCompletarMeta(meta: any): void {
    this.modalConfirmarCompletar.set(meta);
  }

  cerrarModalCompletar(): void {
    this.modalConfirmarCompletar.set(null);
  }

  // Confirmar y completar meta (descuenta saldo disponible, marca completada y genera gasto)
  confirmarCompletarMeta(): void {
    const meta = this.modalConfirmarCompletar();
    if (!meta) return;

    this.cargando.set(true);
    this.errorMensaje.set('');
    this.exitoMensaje.set('');

    const usuario = this.authService.usuario();
    
    const transaccionPayload: TransaccionRequestDTO = {
      usuarioId: usuario?.id ?? '',
      nombreCliente: usuario?.nombreUsuario ?? 'Cliente',
      monto: meta.montoObjetivo,
      tipo: 'GASTO',
      categoriaId: 'otros',
      fechaTransaccion: new Date().toISOString(),
      metodoPago: 'DIGITAL',
      notas: `Meta alcanzada: ${meta.nombreVisual}|Gasto registrado automáticamente al cumplir el objetivo financiero|DIARIO`
    };

    forkJoin({
      gasto: this.transaccionesService.registrar(transaccionPayload),
      meta: this.metasService.actualizarProgresoMeta(meta.id, meta.montoObjetivo)
    }).subscribe({
      next: () => {
        this.exitoMensaje.set(`¡Felicidades! Has completado tu meta "${meta.nombreVisual}". Se registró un gasto de S/ ${meta.montoObjetivo.toFixed(2)}.`);
        this.modalConfirmarCompletar.set(null);
        this.metaSeleccionada.set(null);
        
        this.financieroService.cargarResumen();
        this.cargarMetas();
        
        this.eventBus.emit({ type: 'TRANSACTION_MODIFIED' });
        
        setTimeout(() => this.exitoMensaje.set(''), 5000);
      },
      error: () => {
        // Simulación offline si falla
        this.marcarMockComoCompletadoLocalmente(meta.id, meta.montoObjetivo);
        this.metas.update(items => items.map(i => {
          if (i.id === meta.id) {
            return {
              ...i,
              montoActual: meta.montoObjetivo,
              completada: true,
              fechaActualizacion: new Date().toISOString()
            };
          }
          return i;
        }));

        this.exitoMensaje.set(`¡Felicidades! Has completado tu meta "${meta.nombreVisual}" (Modo Pruebas).`);
        this.modalConfirmarCompletar.set(null);
        this.metaSeleccionada.set(null);
        this.cargando.set(false);
        setTimeout(() => this.exitoMensaje.set(''), 5000);
      }
    });
  }

  private marcarMockComoCompletadoLocalmente(id: string, montoObjetivo: number): void {
    const localMetasStr = localStorage.getItem('luka_mock_metas');
    if (localMetasStr) {
      try {
        let lista = JSON.parse(localMetasStr);
        lista = lista.map((m: any) => {
          if (m.id === id) {
            return {
              ...m,
              montoActual: montoObjetivo,
              completada: true,
              fechaActualizacion: new Date().toISOString()
            };
          }
          return m;
        });
        localStorage.setItem('luka_mock_metas', JSON.stringify(lista));
      } catch (e) {
        console.error(e);
      }
    }
  }

  limpiarFiltros(): void {
    this.filtroEstado.set('Todas');
    this.filtroMes.set('Todos');
    this.filtroAnio.set('Todos');
    this.filtroMontoMin.set(null);
    this.filtroMontoMax.set(null);
    this.paginaActual.set(0);
  }

  mostrarDashboard(): boolean {
    return !this.cargando() || this.metas().length > 0;
  }

  mostrarBotonCompletar(meta: any): boolean {
    return !!(meta && !meta.completada && meta.puedeCompletar);
  }

  mostrarBotonProgreso(meta: any): boolean {
    return !!(meta && !meta.completada && !meta.puedeCompletar);
  }

  mostrarBotonEditar(meta: any): boolean {
    return !!(meta && !meta.completada);
  }

  mostrarCajaExito(meta: any): boolean {
    return !!(meta && meta.completada);
  }

  mostrarCajaProgreso(meta: any): boolean {
    return !!(meta && !meta.completada);
  }

  saldoNegativoAlCompletar(meta: any): boolean {
    return !!(meta && meta.montoObjetivo > this.ahorroDisponible());
  }
}
