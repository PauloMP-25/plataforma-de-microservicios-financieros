import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RespuestaMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';
import { AuthService } from '../../../../core/services/auth.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { NotificacionService } from '../../../../core/services/notificacion.service';
import { TransaccionRequestDTO } from '../../../../core/models/financiero/transaccion.model';
import { MetaKpiComponent } from '../../components/meta-kpi/meta-kpi.component';
import { MetaFiltersComponent } from '../../components/meta-filters/meta-filters.component';
import { MetaCardComponent } from '../../components/meta-card/meta-card.component';
import { MetaDetailsSidebarComponent } from '../../components/meta-details-sidebar/meta-details-sidebar.component';
import { MetaConfirmModalComponent } from '../../components/meta-confirm-modal/meta-confirm-modal.component';
import { MetasUtilityService } from '../../services/metas-utility.service';
import { MetasDataService } from '../../services/metas-data.service';
import { DashboardStateService } from '../../../../core/services/dashboard-state.service';

import { OnboardingTour, TourStep } from '../../../../shared/components/onboarding-tour/onboarding-tour';

@Component({
  selector: 'app-metas-page',
  standalone: true,
  imports: [
    CommonModule,
    MetaKpiComponent,
    MetaFiltersComponent,
    MetaCardComponent,
    MetaDetailsSidebarComponent,
    MetaConfirmModalComponent,
    OnboardingTour
  ],
  templateUrl: './metas-page.html',
  styleUrl: './metas-page.scss',
})
export class MetasPage implements OnInit {
  private router = inject(Router);
  private metasDataService = inject(MetasDataService);
  private dashboardState = inject(DashboardStateService);
  private authService = inject(AuthService);
  private eventBus = inject(AppEventBus);
  private metasUtility = inject(MetasUtilityService);
  private notificacionService = inject(NotificacionService);

  // Estado
  metas = signal<RespuestaMetaAhorro[]>([]);
  cargando = signal<boolean>(false);
  errorMensaje = signal<string>('');
  exitoMensaje = signal<string>('');

  // Paginación reactiva (client-side)
  paginaActual = signal<number>(1);
  readonly pageSize = 6;

  // Modales y Paneles
  metaSeleccionada = signal<any | null>(null);
  modalConfirmarCompletar = signal<any | null>(null);

  // Filtros reactivos
  filtroEstado = signal<string>('Todas');
  filtroMes = signal<string>('Todos');
  filtroAnio = signal<string>('Todos');
  filtroMontoMin = signal<number | null>(null);
  filtroMontoMax = signal<number | null>(null);

  // Lista de años dinámicos basados en metas y el año actual
  aniosDisponibles = computed(() => {
    const yearsSet = new Set<number>();
    const currentYear = new Date().getFullYear();
    yearsSet.add(currentYear);
    yearsSet.add(currentYear + 1);
    yearsSet.add(currentYear + 2);

    this.metas().forEach(m => {
      const fechaAnio = m.fechaCreacion || m.fechaObjetivo;
      if (fechaAnio) {
        const parts = fechaAnio.substring(0, 10).split('-');
        if (parts.length === 3) {
          const y = parseInt(parts[0], 10);
          if (!isNaN(y)) yearsSet.add(y);
        }
      }
    });

    return Array.from(yearsSet).sort((a, b) => a - b);
  });

  // Exponer Math para la plantilla HTML
  protected readonly Math = Math;

  // Lista de categorías con sus íconos correspondientes
  categorias = this.metasUtility.categorias;

  // Ahorro disponible cargado desde DashboardStateService (balance YTD)
  ahorroDisponible = computed(() => {
    const resumen = this.dashboardState.resumenYTD();
    return (resumen && resumen.balance != null) ? resumen.balance : 0;
  });

  // Cálculo secuencial/híbrido del avance de las metas activas usando el saldo restante
  metasCalculadas = computed(() => {
    const listado = this.metas();
    const disponibleGlobal = this.ahorroDisponible();

    const completadas = listado.filter(m => m.completada);
    const activas = listado.filter(m => !m.completada);

    // Ordenar activas por fecha límite más cercana (prioridad de llenado)
    const activasOrdenadas = [...activas].sort((a, b) => {
      if (!a.fechaObjetivo) return 1;
      if (!b.fechaObjetivo) return -1;
      return new Date(a.fechaObjetivo).getTime() - new Date(b.fechaObjetivo).getTime();
    });

    const activasCalculadas = activasOrdenadas.map(meta => {
      const datosVisuales = this.metasUtility.obtenerCategoriaYNombre(meta.nombre);
      const nombreVisual = datosVisuales.nombre;
      const categoriaVisual = meta.proposito || datosVisuales.categoria || 'Otros';
      const iconoVisual = this.metasUtility.obtenerIconoCategoria(categoriaVisual);

      const montoAplicado = Math.min(meta.montoObjetivo, disponibleGlobal);

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
      const datosVisuales = this.metasUtility.obtenerCategoriaYNombre(meta.nombre);
      const nombreVisual = datosVisuales.nombre;
      const categoriaVisual = meta.proposito || datosVisuales.categoria || 'Otros';
      const iconoVisual = this.metasUtility.obtenerIconoCategoria(categoriaVisual);

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
        if (!m.fechaObjetivo) return false;
        const limitStr = m.fechaObjetivo.substring(0, 10);
        const parts = limitStr.split('-');
        if (parts.length !== 3) return false;
        const month = parseInt(parts[1], 10) - 1; // 0-indexed en JS
        return month === mesNum;
      });
    }

    // Filtro por Año (Basado en la fecha de creación)
    const anio = this.filtroAnio();
    if (anio !== 'Todos') {
      const anioNum = parseInt(anio, 10);
      listado = listado.filter(m => {
        const fechaAnio = m.fechaCreacion || m.fechaObjetivo;
        if (!fechaAnio) return false;
        const startStr = fechaAnio.substring(0, 10);
        const parts = startStr.split('-');
        if (parts.length !== 3) return false;
        const year = parseInt(parts[0], 10);
        return year === anioNum;
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

  // Metas paginadas para mostrar 6 por página
  totalPaginas = computed(() => {
    return Math.ceil(this.metasFiltradas().length / this.pageSize) || 1;
  });

  paginasArray = computed(() => {
    const total = this.totalPaginas();
    const arr = [];
    for (let i = 1; i <= total; i++) {
      arr.push(i);
    }
    return arr;
  });

  metasPaginadas = computed(() => {
    const listado = this.metasFiltradas();
    const page = this.paginaActual();
    const total = this.totalPaginas();
    const cappedPage = page > total ? total : page;
    const inicio = (cappedPage - 1) * this.pageSize;
    const fin = inicio + this.pageSize;
    return listado.slice(inicio, fin);
  });

  // KPIs
  metasActivasCount = computed(() => this.metasCalculadas().filter(m => !m.completada).length);
  metasCumplidasCount = computed(() => this.metasCalculadas().filter(m => m.completada).length);
  
  metaMasCercana = computed(() => {
    const activas = this.metasCalculadas().filter(m => !m.completada);
    if (activas.length === 0) return null;
    return [...activas].sort((a, b) => {
      if (!a.fechaObjetivo) return 1;
      if (!b.fechaObjetivo) return -1;
      return new Date(a.fechaObjetivo).getTime() - new Date(b.fechaObjetivo).getTime();
    })[0];
  });

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: '.metas-page__btn-nueva',
      title: 'Crear Nueva Meta',
      description: 'Define tus objetivos de ahorro especificando el nombre, monto total necesario y la fecha límite para cumplirlos.',
      position: 'bottom'
    },
    {
      targetSelector: '.metas-page__resumen-dashboard',
      title: 'Resumen de Metas',
      description: 'Lleva el seguimiento del total de metas activas, cuántas has logrado completar y el saldo acumulado disponible para repartir.',
      position: 'bottom'
    },
    {
      targetSelector: 'app-meta-filters',
      title: 'Barra de Filtros',
      description: 'Filtra y ordena tus metas por estado, periodo mensual y rango de montos para focalizar tus prioridades de ahorro.',
      position: 'bottom'
    },
    {
      targetSelector: '.metas-page__grid',
      title: 'Tus Objetivos de Ahorro',
      description: 'Visualiza el avance detallado de cada meta. Puedes seleccionarla para ver su desglose, editarla o completarla.',
      position: 'top'
    }
  ];

  completarTour(): void {
    localStorage.setItem('luka_tour_metas_visto', 'true');
    this.mostrarTour.set(false);
  }

  ngOnInit(): void {
    this.dashboardState.cargarAnalitica();
    this.cargarMetas();

    const tourVisto = localStorage.getItem('luka_tour_metas_visto');
    if (!tourVisto) {
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
  }

  cargarMetas(): void {
    this.cargando.set(true);
    this.errorMensaje.set('');

    this.metasDataService.listarMetas(0, 100).subscribe({
      next: (pagina) => {
        if (pagina && pagina.content) {
          this.metas.set(pagina.content);
        } else {
          this.metas.set([]);
        }
        this.cargando.set(false);

        // Actualizar detalle
        const seleccionada = this.metaSeleccionada();
        if (seleccionada) {
          const fresca = this.metasCalculadas().find(m => m.id === seleccionada.id);
          if (fresca) this.metaSeleccionada.set(fresca);
        }
      },
      error: () => {
        this.errorMensaje.set('Hubo un error al cargar tus metas. Por favor, intenta de nuevo.');
        this.metas.set([]);
        this.cargando.set(false);
      }
    });
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas()) {
      this.paginaActual.set(pagina);
    }
  }

  siguientePagina(): void {
    if (this.paginaActual() < this.totalPaginas()) {
      this.paginaActual.update(p => p + 1);
    }
  }

  anteriorPagina(): void {
    if (this.paginaActual() > 1) {
      this.paginaActual.update(p => p - 1);
    }
  }

  // Descomponer prefijo del nombre
  obtenerCategoriaYNombre(metaNombre: string): { categoria: string; nombre: string; icono: string } {
    return this.metasUtility.obtenerCategoriaYNombre(metaNombre);
  }

  obtenerIconoCategoria(catId: string): string {
    return this.metasUtility.obtenerIconoCategoria(catId);
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
    if (!meta.fechaObjetivo) return false;
    const limite = new Date(meta.fechaObjetivo + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return limite < hoy;
  }

  calcularDiasRestantes(fechaObjetivoStr: string): number {
    if (!fechaObjetivoStr) return 0;
    const limite = new Date(fechaObjetivoStr + 'T00:00:00');
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
    if (!meta.fechaInicio) {
      return 'N/A';
    }
    const fin = meta.fechaCompletada || meta.fechaActualizacion || new Date().toISOString();
    return this.calcularTiempoEmpleado(meta.fechaInicio, fin);
  }

  obtenerFechaActualizacionOCreacion(meta: RespuestaMetaAhorro): string {
    return meta.fechaInicio || meta.fechaCreacion || '';
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

    this.metasDataService.eliminarMeta(metaId).subscribe({
      next: () => {
        this.notificacionService.mostrar(
          'Meta Eliminada',
          'La meta de ahorro fue eliminada con éxito.',
          'meta',
          'trash-can'
        );
        if (this.metaSeleccionada()?.id === metaId) {
          this.metaSeleccionada.set(null);
        }
        this.cargarMetas();
      },
      error: () => {
        this.notificacionService.mostrar(
          'Error',
          'Hubo un error al intentar eliminar la meta de ahorro.',
          'meta',
          'triangle-exclamation'
        );
        this.cargando.set(false);
      }
    });
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
    this.metasDataService.completarMeta(meta).subscribe({
      next: (res) => {
        const mensajeExito = res.isMock
          ? `Has completado tu meta "${meta.nombreVisual}" (Modo Pruebas). Completa el registro del gasto.`
          : `Has completado tu meta "${meta.nombreVisual}". Completa el registro del gasto.`;
        
        this.notificacionService.mostrar('¡Felicidades!', mensajeExito, 'meta', 'award', 5000);
        this.modalConfirmarCompletar.set(null);
        this.metaSeleccionada.set(null);
        
        this.dashboardState.invalidarCache();
        
        // Redirigir al formulario de gastos
        this.router.navigate(['/gastos/nuevo'], {
          queryParams: {
            metaId: meta.id,
            monto: meta.montoObjetivo,
            nombre: meta.nombreVisual,
            descripcion: `Gasto por meta: ${meta.nombreVisual}`
          }
        });
      },
      error: () => {
        this.notificacionService.mostrar(
          'Error',
          'Hubo un error al completar la meta de ahorro.',
          'meta',
          'triangle-exclamation'
        );
        this.cargando.set(false);
      }
    });
  }

  limpiarFiltros(): void {
    this.filtroEstado.set('Todas');
    this.filtroMes.set('Todos');
    this.filtroAnio.set('Todos');
    this.filtroMontoMin.set(null);
    this.filtroMontoMax.set(null);
    this.paginaActual.set(1);
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
