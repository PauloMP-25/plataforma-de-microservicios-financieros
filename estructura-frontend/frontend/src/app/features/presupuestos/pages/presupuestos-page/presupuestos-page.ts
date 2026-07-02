import { Component, OnInit, signal, computed, inject, effect, ElementRef, ViewChild, AfterViewInit, OnDestroy, Injector } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { PresupuestoService } from '../../../../core/services/presupuesto.service';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { PresupuestoDTO } from '../../../../core/models/financiero/presupuesto.model';

import { NotificacionService } from '../../../../core/services/notificacion.service';


import { GastosStateService } from '../../../../core/services/gastos-state.service';
import { AuthService } from '../../../../core/services/auth.service';
import Chart from 'chart.js/auto';


import { OnboardingTour, TourStep } from '../../../../shared/components/onboarding-tour/onboarding-tour';


@Component({
  selector: 'app-presupuestos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, OnboardingTour],
  providers: [DatePipe],
  templateUrl: './presupuestos-page.html',
  styleUrls: ['./presupuestos-page.scss']
})
export class PresupuestosPage implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('evolucionCanvas') evolucionCanvasRef?: ElementRef<HTMLCanvasElement>;

  private fb = inject(FormBuilder);
  private presupuestoService = inject(PresupuestoService);
  private financieroService = inject(FinancieroService);
  private gastosState = inject(GastosStateService);
  private datePipe = inject(DatePipe);

  private notificacionService = inject(NotificacionService);

  authService = inject(AuthService);

  // Chart instance
  private evolucionChartInstance: Chart | null = null;
  private injector = inject(Injector);

  constructor() {
    effect(() => {
      const hist = this.historialPresupuestos();
      const activo = this.presupuestoActivo();
      if (this.esPremiumOPro() && this.evolucionChartInstance && hist.length) {
        this._actualizarChart(this.evolucionChartInstance, hist, activo?.montoLimite ?? 1000, activo?.porcentajeAlerta ?? 80);
      }
    });
  }



  // --- SIGNALS DE ESTADO ---
  formulario!: FormGroup;
  cargando = signal<boolean>(false);
  temaOscuro = signal<boolean>(false);

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: '.lk-sidebar',
      title: 'Configuración de Límite',
      description: 'Establece el monto límite mensual, define alertas en base a porcentajes y especifica las fechas de inicio/fin del período de evaluación.',
      position: 'right'
    },
    {
      targetSelector: '.lk-gauge-card',
      title: 'Gauge de Consumo',
      description: 'Visualiza de forma gráfica el porcentaje consumido de tu límite de presupuesto, la cantidad gastada y el monto de dinero disponible.',
      position: 'bottom'
    },
    {
      targetSelector: '.lk-evolucion-card, .lk-premium-card',
      title: 'Evolución Histórica',
      description: 'Revisa de forma comparativa tus presupuestos y consumo real a lo largo de periodos anteriores (Exclusivo en Luka Premium).',
      position: 'bottom'
    },
    {
      targetSelector: '.lk-bottom-grid',
      title: 'Desglose y Registro de Historial',
      description: 'Consulta un resumen detallado del dinero gastado por cada categoría y la lista histórica de límites inactivos del sistema.',
      position: 'top'
    }
  ];

  completarTour(): void {
    localStorage.setItem('luka_tour_presupuestos_visto', 'true');
    this.mostrarTour.set(false);
  }

  // Datos del Negocio
  presupuestoActivo = signal<PresupuestoDTO | null>(null);
  gastoTotalMes = signal<number>(0);
  historialPresupuestos = signal<any[]>([]);

  // UI States
  mostrarConfirmEliminar = signal<boolean>(false);
  mostrarTodoHistorial = signal<boolean>(false);
  exitoMensaje = signal<string | null>(null);
  errorMensaje = signal<string | null>(null);

  paginaHistorial = signal<number>(1);

  // Categorías filtradas y top 5
  categorias = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo) return [];

    const gastos = this.gastosState.gastos();
    const fIni = new Date(activo.fechaInicio).getTime();
    const fFin = new Date(activo.fechaFin).getTime() + 86400000 - 1;

    const filtrados = gastos.filter(g => {
      const t = new Date(g.fechaTransaccion).getTime();
      return t >= fIni && t <= fFin;
    });

    const mapa = new Map<string, number>();
    let total = 0;
    for (const g of filtrados) {
      const cat = g.categoria || 'Otros';
      const m = Number(g.monto) || 0;
      mapa.set(cat, (mapa.get(cat) || 0) + m);
      total += m;
    }

    const arr = Array.from(mapa.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5);

    const bgColors = ['rgba(91, 106, 240, 0.1)', 'rgba(6, 182, 212, 0.1)', 'rgba(34, 197, 94, 0.1)', 'rgba(245, 158, 11, 0.1)', 'rgba(239, 68, 68, 0.1)'];
    const colors = ['#5B6AF0', '#06B6D4', '#22C55E', '#F59E0B', '#EF4444'];
    const icons = ['fa-tag', 'fa-tag', 'fa-tag', 'fa-tag', 'fa-tag'];

    return arr.map(([nombre, monto], i) => ({
      nombre,
      monto,
      porcentaje: total > 0 ? Math.round((monto / total) * 100) : 0,
      icono: icons[i % icons.length],
      bg: bgColors[i % bgColors.length],
      color: colors[i % colors.length]
    }));
  });

  // --- STATS DE EVOLUCIÓN ---
  mesMasAlto = computed(() => {
    const hist = this.historialPresupuestos();
    if (!hist.length) return null;
    return hist.reduce((prev: any, curr: any) => curr.montoConsumido > prev.montoConsumido ? curr : prev);
  });

  mesMasBajo = computed(() => {
    const hist = this.historialPresupuestos();
    if (!hist.length) return null;
    return hist.reduce((prev: any, curr: any) => curr.montoConsumido < prev.montoConsumido ? curr : prev);
  });

  conteoExcedidos = computed(() => {
    return this.historialPresupuestos().filter((h: any) =>
      h.montoLimite > 0 && h.montoConsumido > h.montoLimite
    ).length;
  });

  esPremiumOPro = computed(() => this.authService.esPremium() || this.authService.esPro());

  // --- SIGNALS COMPUTADOS ---
  porcentajeConsumo = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo || activo.montoLimite <= 0) return 0;
    const pct = (this.gastoTotalMes() / activo.montoLimite) * 100;
    return Math.min(Math.round(pct), 999);
  });

  margenDisponible = computed(() => {
    const activo = this.presupuestoActivo();
    if (!activo) return 0;
    const margen = activo.montoLimite - this.gastoTotalMes();
    return margen > 0 ? margen : 0;
  });

  estadoAlerta = computed<'seguro' | 'alerta' | 'superado'>(() => {
    const pct = this.porcentajeConsumo();
    const activo = this.presupuestoActivo();
    const limiteAlerta = activo ? activo.porcentajeAlerta : 80;

    if (pct >= 100) return 'superado';
    if (pct >= limiteAlerta) return 'alerta';
    return 'seguro';
  });

  diasRestantes = computed<number | null>(() => {
    const activo = this.presupuestoActivo();
    if (!activo) return null;
    const hoy = new Date();
    const fin = new Date(activo.fechaFin);
    const diferenciaMs = fin.getTime() - hoy.getTime();
    if (diferenciaMs < 0) return 0;
    return Math.ceil(diferenciaMs / (1000 * 60 * 60 * 24));
  });

  labelBotonGuardar = computed(() => {
    return this.presupuestoActivo() ? 'Actualizar límite' : 'Establecer límite';
  });

  gaugeOffset = computed(() => {
    const pct = Math.min(this.porcentajeConsumo(), 100);
    return (314.16 - (314.16 * pct) / 100).toString();
  });

  gaugeColor = computed(() => {
    const estado = this.estadoAlerta();
    if (estado === 'superado') return '#EF4444';
    if (estado === 'alerta') return '#F59E0B';
    return '#22C55E';
  });

  historialVisible = computed(() => {
    const lista = this.historialPresupuestos();
    const pag = this.paginaHistorial();
    const start = (pag - 1) * 5;
    return lista.slice(start, start + 5);
  });

  totalPaginasHistorial = computed(() => {
    return Math.ceil(this.historialPresupuestos().length / 5);
  });

  cambiarPagina(delta: number): void {
    const nueva = this.paginaHistorial() + delta;
    if (nueva >= 1 && nueva <= this.totalPaginasHistorial()) {
      this.paginaHistorial.set(nueva);
    }
  }

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarDatosDashboard();
    this.gastosState.cargarDatos();
    this.detectarTemaActual();

    const tourVisto = localStorage.getItem('luka_tour_presupuestos_visto');
    if (!tourVisto) {
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => this._initChart(), 0);
  }

  ngOnDestroy(): void {
    this.evolucionChartInstance?.destroy();
    this.evolucionChartInstance = null;
  }

  private _pointColors(data: number[], limit: number): string[] {
    return data.map(v =>
      v > limit ? '#EF4444' : v > limit * 0.8 ? '#F59E0B' : '#818CF8'
    );
  }

  private _pointSizes(data: number[], limit: number): number[] {
    return data.map(v =>
      v > limit ? 8 : v > limit * 0.8 ? 6 : 5
    );
  }

  private _buildChartConfig(hist: any[], limit: number, alertPct: number): any {
    const labels = hist.map(h => h.periodo || h.nombre || '—');
    const consumed = hist.map(h => Number(h.montoConsumido) || 0);
    const alertaLimite = limit * (alertPct / 100);
    const isDark = document.body.classList.contains('theme-dark');
    const gridColor = isDark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)';
    const textColor = isDark ? '#A1AABF' : '#6B7280';

    return {
      type: 'line' as const,
      data: {
        labels,
        datasets: [
          {
            label: 'Consumido',
            data: consumed,
            borderColor: '#818CF8',
            borderWidth: 2.5,
            pointBackgroundColor: this._pointColors(consumed, limit),
            pointBorderColor: isDark ? '#161B27' : '#ffffff',
            pointBorderWidth: 2,
            pointRadius: this._pointSizes(consumed, limit),
            pointHoverRadius: 10,
            fill: true,
            backgroundColor: isDark ? 'rgba(129,140,248,0.08)' : 'rgba(129,140,248,0.06)',
            tension: 0.4,
            segment: {
              borderColor: (ctx: any) => {
                const v = consumed[ctx.p1DataIndex];
                if (v > limit) return '#EF4444';
                if (v > limit * 0.8) return '#F59E0B';
                return '#818CF8';
              }
            }
          },
          {
            label: 'Límite',
            data: Array(labels.length).fill(limit),
            borderColor: '#10B981',
            borderDash: [7, 4],
            borderWidth: 1.5,
            pointRadius: 0,
            fill: false,
            tension: 0
          },
          {
            label: `Alerta ${alertPct}%`,
            data: Array(labels.length).fill(alertaLimite),
            borderColor: '#F59E0B',
            borderDash: [3, 5],
            borderWidth: 1,
            pointRadius: 0,
            fill: false,
            tension: 0
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index' as const, intersect: false },
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              afterBody: (items: any[]) => {
                const idx = items[0]?.dataIndex;
                if (idx === undefined) return '';
                const v = consumed[idx];
                const pct = limit > 0 ? Math.round((v / limit) * 100) : 0;
                const estado = v > limit ? '🔴 Excedido' : v > limit * 0.8 ? '🟡 Alerta' : '🟢 Normal';
                return [`${pct}% del límite`, estado];
              }
            }
          }
        },
        scales: {
          x: {
            ticks: { color: textColor, font: { size: 11 } },
            grid: { color: gridColor }
          },
          y: {
            beginAtZero: true,
            ticks: { color: textColor, font: { size: 11 }, callback: (v: any) => `S/ ${v}` },
            grid: { color: gridColor }
          }
        }
      }
    };
  }

  private _initChart(): void {
    if (!this.esPremiumOPro() || !this.evolucionCanvasRef) return;
    const hist = this.historialPresupuestos();
    if (!hist.length) return;
    const activo = this.presupuestoActivo();
    const limit = activo?.montoLimite ?? 1000;
    const alertPct = activo?.porcentajeAlerta ?? 80;
    const ctx = this.evolucionCanvasRef.nativeElement.getContext('2d')!;
    this.evolucionChartInstance?.destroy();
    this.evolucionChartInstance = new Chart(ctx, this._buildChartConfig(hist, limit, alertPct));
  }

  private _actualizarChart(chart: Chart, hist: any[], limit: number, alertPct: number): void {
    const config = this._buildChartConfig(hist, limit, alertPct);
    chart.data = config.data;
    chart.update('active');
  }

  initChartDeferred(): void {
    setTimeout(() => this._initChart(), 50);
  }

  private inicializarFormulario(): void {
    const hoyStr = new Date().toISOString().substring(0, 10);
    const finMes = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0);
    const finStr = finMes.toISOString().substring(0, 10);

    this.formulario = this.fb.group({
      nombre: ['Presupuesto Mensual', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      montoLimite: [1000, [Validators.required, Validators.min(1)]],
      porcentajeAlerta: [80, [Validators.required, Validators.min(50), Validators.max(100)]],
      fechaInicio: [hoyStr, [Validators.required]],
      fechaFin: [finStr, [Validators.required]]
    }, { validators: this.validarFechas });
  }

  private validarFechas(control: AbstractControl): { [key: string]: boolean } | null {
    const inicio = control.get('fechaInicio')?.value;
    const fin = control.get('fechaFin')?.value;
    if (inicio && fin && new Date(fin) < new Date(inicio)) {
      return { 'fechasInvalidas': true };
    }
    return null;
  }

  campoInvalido(campo: string): boolean {
    const control = this.formulario.get(campo);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  private cargarDatosDashboard(): void {
    this.cargando.set(true);

    this.financieroService.getResumen().subscribe({
      next: (resumen) => this.gastoTotalMes.set(resumen.totalGastos || 0),
      error: () => this.mostrarToast('Error al recuperar balance de gastos.', 'danger')
    });

    this.presupuestoService.obtenerActivo().subscribe({
      next: (presupuesto) => {
        if (presupuesto) {
          this.presupuestoActivo.set(presupuesto);
          this.formulario.patchValue({
            nombre: presupuesto.nombre || 'Presupuesto Mensual',
            montoLimite: presupuesto.montoLimite,
            porcentajeAlerta: presupuesto.porcentajeAlerta,
            fechaInicio: this.formatearFechaInput(presupuesto.fechaInicio),
            fechaFin: this.formatearFechaInput(presupuesto.fechaFin)
          });
          this.formulario.get('nombre')?.disable();
        } else {
          this.presupuestoActivo.set(null);
          this.formulario.get('nombre')?.enable();
        }
        this.cargando.set(false);
      },
      error: () => {
        this.mostrarToast('Error al cargar la configuración de presupuestos.', 'danger');
        this.cargando.set(false);
      }
    });

    this.presupuestoService.listarHistorial().subscribe({
      next: (historial) => {
        const hMapeado = (historial || []).map(h => ({
          ...h,
          montoLimite: Number(h.montoLimite) || 0,
          montoConsumido: Number((h as any).montoConsumido) || 0,
          porcentajeAlerta: Number(h.porcentajeAlerta) || 80
        }));
        this.historialPresupuestos.set(hMapeado);
      },
      error: () => console.error('Error silencioso al listar historial.')
    });
  }

  guardarPresupuesto(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.cargando.set(true);
    const payload = this.formulario.getRawValue();
    const activoActual = this.presupuestoActivo();

    // Si hay un presupuesto activo, le metemos su ID al payload para enviar un solo argumento
    const request$ = activoActual
      ? this.presupuestoService.actualizar({ ...payload, id: activoActual.id })
      : this.presupuestoService.crear(payload);

    request$.subscribe({
      next: () => {
        this.mostrarToast('Límite actualizado correctamente.', 'success');
        this.notificacionService.mostrarPresupuestoCreado(payload.nombre || 'Presupuesto');
        this.cargarDatosDashboard();
      },
      error: () => {
        this.mostrarToast('Hubo un error al guardar la configuración.', 'danger');
        this.cargando.set(false);
      }
    });
  }
  pedirConfirmacionEliminar(): void {
    this.mostrarConfirmEliminar.set(true);
  }

  cancelarEliminar(): void {
    this.mostrarConfirmEliminar.set(false);
  }

  eliminarPresupuesto(): void {
    this.mostrarConfirmEliminar.set(false);
    this.cargando.set(true);

    this.presupuestoService.eliminarActivo().subscribe({
      next: () => {
        this.mostrarToast('Límite desactivado del período actual.', 'success');
        this.presupuestoActivo.set(null);
        this.limpiarFormulario();
        this.cargarDatosDashboard();
      },
      error: () => {
        this.mostrarToast('No se pudo desactivar el límite.', 'danger');
        this.cargando.set(false);
      }
    });
  }

  limpiarFormulario(): void {
    this.formulario.patchValue({
      nombre: 'Presupuesto Mensual',
      montoLimite: 1000,
      porcentajeAlerta: 80
    });
    this.formulario.get('nombre')?.enable();
  }

  montoAlertaCalculado(): number {
    const limite = this.formulario?.get('montoLimite')?.value || 0;
    const pct = this.formulario?.get('porcentajeAlerta')?.value || 80;
    return (limite * pct) / 100;
  }

  pctHistorial(h: any): number {
    if (h.montoLimite <= 0) return 0;
    return Math.round((h.montoConsumido / h.montoLimite) * 100);
  }

  estadoConsumoHistorial(h: any): 'seguro' | 'alerta' | 'superado' {
    const pct = this.pctHistorial(h);
    if (pct >= 100) return 'superado';
    if (pct >= h.porcentajeAlerta) return 'alerta';
    return 'seguro';
  }

  toggleHistorial(): void {
    this.mostrarTodoHistorial.update(v => !v);
  }

  toggleTema(): void {
    const body = document.body;
    if (body.classList.contains('theme-dark')) {
      body.classList.remove('theme-dark');
      this.temaOscuro.set(false);
    } else {
      body.classList.add('theme-dark');
      this.temaOscuro.set(true);
    }
  }

  private detectarTemaActual(): void {
    this.temaOscuro.set(document.body.classList.contains('theme-dark'));
  }

  formatearMonto(valor: number): string {
    return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(valor);
  }

  private formatearFechaInput(fechaIso: string): string {
    return fechaIso.substring(0, 10);
  }

  private mostrarToast(mensaje: string, tipo: 'success' | 'danger'): void {
    if (tipo === 'success') {
      this.exitoMensaje.set(mensaje);
      setTimeout(() => this.exitoMensaje.set(null), 4000);
    } else {
      this.errorMensaje.set(mensaje);
      setTimeout(() => this.errorMensaje.set(null), 4000);
    }
  }
}