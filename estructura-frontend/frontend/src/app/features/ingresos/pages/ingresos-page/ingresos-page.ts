import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { IngresosStateService } from '../../../../core/services/ingresos-state.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import { OnboardingTour, TourStep } from '../../../../shared/components/onboarding-tour/onboarding-tour';
import {
  DistribucionCategoria,
  IngresoKpi,
  IngresoReciente,
  IngresoTendenciaPunto,
} from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingresos-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    OnboardingTour
  ],
  templateUrl: './ingresos-page.html',
  styleUrl: './ingresos-page.scss',
})
export class IngresosPage implements OnInit {
  private readonly stateService = inject(IngresosStateService);
  private readonly eventBus = inject(AppEventBus);


  private readonly ingresosMock = [
    {
      id: 'mock-i1',
      fechaTransaccion: new Date(Date.now() - 86400000).toISOString(),
      monto: 3500.00,
      categoria: 'Salario',
      categoriaId: 'salario',
      metodoPago: 'TRANSFERENCIA',
      descripcion: 'Sueldo mensual LUKA Corp',
      notas: ''
    },
    {
      id: 'mock-i2',
      fechaTransaccion: new Date().toISOString(),
      monto: 450.00,
      categoria: 'Freelance',
      categoriaId: 'freelance',
      metodoPago: 'DIGITAL',
      descripcion: 'Desarrollo Landing Page cliente',
      notas: ''
    },
    {
      id: 'mock-i3',
      fechaTransaccion: new Date(Date.now() - 86400000 * 3).toISOString(),
      monto: 150.00,
      categoria: 'Otros',
      categoriaId: 'otros',
      metodoPago: 'EFECTIVO',
      descripcion: 'Venta de audífonos antiguos',
      notas: ''
    }
  ];

  // —— Signals computados para transformar el estado a la interfaz de Ingresos ——

  // Signals de filtro específicos por gráfico
  readonly filtroFechaDistribucion = signal<string>('todos');
  readonly filtroFechaEvolucion = signal<string>('todos');
  readonly filtroFechaProporcion = signal<string>('todos');

  // ── Signals computados para transformar el estado a la interfaz de Ingresos ──

  readonly kpisSignal = computed<IngresoKpi[]>(() => {
    const transacciones = this.stateService.ingresos();
    const resumen = this.stateService.resumenActual();

    const total = resumen !== null ? (resumen.totalIngresos ?? 0) : 4100.00;
    const cantidad = resumen !== null ? (resumen.cantidadIngresos ?? 0) : 3;
    const cats = this.distribucionSignal();
    const primaryCatName = cats[0]?.categoria ?? 'Ninguna';
    const primaryCatPorc = cats[0]?.porcentaje ? `${cats[0].porcentaje.toFixed(0)}% del total` : '0% del total';

    // Lógica dinámica para calcular los nuevos KPIs agregados
    const varIngresos = this.variacionIngresos;
    const absVar = this.absVariacionIngresos.toFixed(1);
    const signo = varIngresos >= 0 ? '+' : '-';

    // Cálculo de ingreso promedio aproximado
    const promedio = cantidad > 0 ? total / cantidad : 0;

    return [
      { titulo: 'Ingresos registrados', valor: String(cantidad), subtitulo: 'Este mes', color: 'violet' },
      { titulo: 'Categoria principal', valor: primaryCatName, subtitulo: primaryCatPorc, color: 'amber' },
      // ── NUEVAS 3 TARJETAS COMPLETAMENTE ACOPLADAS ──
      {
        titulo: 'Comparación mes anterior',
        valor: `${signo}${absVar}%`,
        subtitulo: `Respecto a ${this.nombreMesAnterior}`,
        color: 'emerald'
      },
      {
        titulo: 'Ingreso promedio',
        valor: `S/ ${promedio.toFixed(2)}`,
        subtitulo: 'Por transacción',
        color: 'sky'
      },
      {
        titulo: 'Progreso meta',
        valor: total > 0 ? `${Math.min(Math.round((total / 8000) * 100), 100)}%` : '0%',
        subtitulo: 'Meta de S/ 8,000.00',
        color: 'sky'
      }
    ];
  });

  readonly distribucionSignal = computed<DistribucionCategoria[]>(() => {

    const rawTransacciones = this.stateService.ingresos();
    const transacciones = this.filtrarTransacciones(rawTransacciones, this.filtroFechaDistribucion());
    if (!transacciones.length) return [];


    const map = new Map<string, number>();
    let total = 0;
    for (const t of transacciones) {
      const cat = this.nombreCategoria(t.categoria, t.categoriaId);
      const m = t.monto || 0;
      map.set(cat, (map.get(cat) ?? 0) + m);
      total += m;
    }

    const colores = ['#22c55e', '#7c3aed', '#f59e0b', '#06b6d4', '#ec4899', '#64748b'];

    return Array.from(map.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([categoria, monto], idx) => ({
        categoria,
        monto,
        porcentaje: total > 0 ? (monto / total) * 100 : 0,
        color: colores[idx % colores.length]
      }));
  });


  readonly tendenciaSignal = computed<IngresoTendenciaPunto[]>(() => {
    let transacciones = this.stateService.ingresos();
    if (this.stateService.resumenActual() === null && !transacciones.length) {
      transacciones = this.ingresosMock as any[];
    }
    const meses = new Map<string, { periodo: string; monto: number }>();

    for (const t of transacciones) {
      const fecha = new Date(t.fechaTransaccion);
      const key = `${fecha.getFullYear()}-${fecha.getMonth()}`;
      const periodo = fecha.toLocaleDateString('es-PE', { month: 'short' });
      const m = t.monto || 0;

      const prev = meses.get(key);
      meses.set(key, { periodo, monto: (prev?.monto ?? 0) + m });
    }

    const result = Array.from(meses.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .map(([, v]) => v);

    return result.length > 0 ? result : [
      { periodo: 'Ene', monto: 0 },
      { periodo: 'Feb', monto: 0 },
      { periodo: 'Mar', monto: 0 },
      { periodo: 'Abr', monto: 0 },
      { periodo: 'May', monto: 0 }
    ];
  });

  readonly evolucionMensualSignal = computed<DistribucionCategoria[]>(() => {
    const rawTransacciones = this.stateService.ingresos();
    const transacciones = this.filtrarTransacciones(rawTransacciones, this.filtroFechaEvolucion());
    const meses = new Map<string, { key: string; mesNombre: string; monto: number }>();

    for (const t of transacciones) {
      const fecha = new Date(t.fechaTransaccion);
      const key = `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}`;
      const mesNombre = fecha.toLocaleDateString('es-PE', { month: 'short' });
      const m = t.monto || 0;
      const prev = meses.get(key);
      meses.set(key, { key, mesNombre, monto: (prev?.monto ?? 0) + m });
    }

    const ultimos6 = Array.from(meses.values())
      .sort((a, b) => a.key.localeCompare(b.key))
      .slice(-6);

    const total = ultimos6.reduce((acc, v) => acc + v.monto, 0);
    const colores = ['#06b6d4', '#3b82f6', '#6366f1', '#8b5cf6', '#a855f7', '#d946ef'];

    return ultimos6.map((item, idx) => ({
      categoria: item.mesNombre,
      monto: item.monto,
      porcentaje: total > 0 ? Math.round((item.monto / total) * 100) : 0,
      color: colores[idx % colores.length]
    }));
  });

  readonly proporcionIngresoSignal = computed<DistribucionCategoria[]>(() => {
    const rawTransacciones = this.stateService.ingresos();
    const transacciones = this.filtrarTransacciones(rawTransacciones, this.filtroFechaProporcion());
    let fijo = 0;
    let variable = 0;
    for (const t of transacciones) {
      const etiq = (t.etiquetas || '').toLowerCase();
      if (etiq.includes('fijo')) {
        fijo += t.monto || 0;
      } else {
        variable += t.monto || 0;
      }
    }
    const total = fijo + variable;
    return [
      {
        categoria: 'Fijo',
        monto: fijo,
        porcentaje: total > 0 ? Math.round((fijo / total) * 100) : 0,
        color: '#22c55e'
      },
      {
        categoria: 'Variable',
        monto: variable,
        porcentaje: total > 0 ? Math.round((variable / total) * 100) : 0,
        color: '#7c3aed'
      }
    ];
  });

  readonly recientesSignal = computed<IngresoReciente[]>(() => {
    let transacciones = this.stateService.ingresos();
    if (this.stateService.resumenActual() === null && !transacciones.length) {
      transacciones = this.ingresosMock as any[];
    }
    return transacciones.slice(0, 5).map(t => {
      const fecha = new Date(t.fechaTransaccion);
      return {
        categoria: this.nombreCategoria(t.categoria, t.categoriaId),
        descripcion: t.descripcion || t.notas || 'Ingreso registrado',
        monto: t.monto || 0,
        fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
      };
    });
  });

  // Getters para compatibilidad de enlace directo en plantilla HTML sin alterar bindings básicos
  get kpis(): IngresoKpi[] { return this.kpisSignal(); }
  get distribucion(): DistribucionCategoria[] { return this.distribucionSignal(); }
  get tendencia(): IngresoTendenciaPunto[] { return this.tendenciaSignal(); }
  get recientes(): IngresoReciente[] { return this.recientesSignal(); }
  get evolucionMensual(): DistribucionCategoria[] { return this.evolucionMensualSignal(); }
  get proporcionIngreso(): DistribucionCategoria[] { return this.proporcionIngresoSignal(); }

  getDonutStyle(data: DistribucionCategoria[]): string {
    if (!data || !data.length) {
      return 'background: conic-gradient(#E8EAF2 0% 100%);';
    }
    const stops: string[] = [];
    let prev = 0;
    const isMultiple = data.length > 1;
    for (const item of data) {
      const end = prev + item.porcentaje;
      if (isMultiple) {
        // Dejar un 1% de gap para que se vea la línea divisoria (el fondo de la tarjeta)
        const gapEnd = Math.max(prev, end - 1.5);
        stops.push(`${item.color} ${prev}% ${gapEnd}%`);
        stops.push(`transparent ${gapEnd}% ${end}%`);
      } else {
        stops.push(`${item.color} ${prev}% ${end}%`);
      }
      prev = end;
    }
    return `background: conic-gradient(${stops.join(', ')});`;
  }

  get lineChartPoints(): { x: number, y: number }[] {
    const data = this.evolucionMensual;
    if (!data.length) return [];
    const maxVal = Math.max(...data.map(d => d.monto), 1000);
    return data.map((d, idx) => ({
      x: 20 + idx * 52,
      y: 130 - (d.monto / maxVal) * 110
    }));
  }

  get lineChartPath(): string {
    const points = this.lineChartPoints;
    if (!points.length) return '';
    return `M ${points.map(p => `${p.x},${p.y}`).join(' L ')}`;
  }

  get lineChartAreaPath(): string {
    const points = this.lineChartPoints;
    if (!points.length) return '';
    const lastX = points[points.length - 1].x;
    return `M 20,130 L ${points.map(p => `${p.x},${p.y}`).join(' L ')} L ${lastX},130 Z`;
  }

  get metaPercentage(): number {
    const total = this.totalIngresosActual;
    return total > 0 ? Math.min(total / 8000, 1.0) : 0;
  }

  get metaDashOffset(): number {
    return 220 * (1 - this.metaPercentage);
  }

  get needleX(): number {
    const angle = Math.PI * this.metaPercentage;
    return 90 + 70 * Math.cos(Math.PI - angle);
  }

  get needleY(): number {
    const angle = Math.PI * this.metaPercentage;
    return 100 - 70 * Math.sin(Math.PI - angle);
  }

  getRecentIconBg(categoria: string): string {
    const c = categoria.toLowerCase();
    if (c.includes('salario')) return 'var(--purple-light)';
    if (c.includes('freelance')) return 'var(--teal-light)';
    if (c.includes('invers')) return 'var(--amber-light)';
    return 'var(--coral-light)';
  }

  getRecentIconClass(categoria: string): string {
    const c = categoria.toLowerCase();
    if (c.includes('salario')) return 'fa-solid fa-briefcase icon-purple';
    if (c.includes('freelance')) return 'fa-regular fa-user icon-teal';
    if (c.includes('invers')) return 'fa-solid fa-arrow-trend-up icon-amber';
    return 'fa-solid fa-coins icon-coral';
  }

  // Resumen del panel superior
  get nombreMesActual(): string {
    const meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    return meses[new Date().getMonth()];
  }

  get nombreMesAnterior(): string {
    const meses = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    const idx = new Date().getMonth() - 1;
    return meses[idx < 0 ? 11 : idx];
  }

  get totalIngresosActual(): number {
    if (this.stateService.resumenActual() === null) {
      return 4100.00;
    }
    return this.stateService.resumenActual()?.totalIngresos ?? 0;
  }

  get totalIngresosAnterior(): number {
    if (this.stateService.resumenAnterior() === null) {
      return 3800.00;
    }
    return this.stateService.resumenAnterior()?.totalIngresos ?? 0;
  }

  get variacionIngresos(): number {
    const actual = this.totalIngresosActual;
    const anterior = this.totalIngresosAnterior;
    if (!anterior) return 0;
    return ((actual - anterior) / anterior) * 100;
  }

  get absVariacionIngresos(): number {
    return Math.abs(this.variacionIngresos);
  }

  readonly mostrarTour = signal(false);
  readonly stepsTour: TourStep[] = [
    {
      targetSelector: 'a[routerLink="/ingresos/nuevo"]',
      title: 'Registrar Nuevo Ingreso',
      description: 'Haz clic aquí para agregar un nuevo flujo de ingresos, asignando categorías, montos y métodos de pago de forma rápida.',
      position: 'bottom'
    },
    {
      targetSelector: 'article.bg-gradient-to-r',
      title: 'Resumen de Ingresos',
      description: 'Esta sección te muestra el dinero total acumulado que ha ingresado este mes y la comparación porcentual con el mes anterior.',
      position: 'bottom'
    },
    {
      targetSelector: '.kpi-grid',
      title: 'Indicadores Clave (KPIs)',
      description: 'Examina estadísticas rápidas sobre la cantidad de ingresos del mes, la categoría principal, tu promedio por transacción y el progreso hacia tu meta de ahorro.',
      position: 'top'
    },
    {
      targetSelector: '.charts-grid',
      title: 'Gráficos de Distribución',
      description: 'Aquí puedes analizar de forma visual en qué categorías se concentran tus ingresos (Salario, Freelance, etc.) y visualizar tu progreso acumulado.',
      position: 'top'
    }
  ];

  ngOnInit(): void {
    const tourVisto = localStorage.getItem('luka_tour_ingresos_visto');
    if (!tourVisto) {
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
  }

  completarTour(): void {
    localStorage.setItem('luka_tour_ingresos_visto', 'true');
    this.mostrarTour.set(false);
  }

  constructor() {
    this.stateService.cargarDatos();
  }

  private filtrarTransacciones(transacciones: any[], filtro: string): any[] {
    if (filtro === 'todos') return transacciones;

    const ahora = new Date();
    const unDiaMs = 24 * 60 * 60 * 1000;

    return transacciones.filter(t => {
      const fecha = new Date(t.fechaTransaccion);
      const diffMs = ahora.getTime() - fecha.getTime();
      const diffDias = diffMs / unDiaMs;

      if (filtro === 'semana') {
        return diffDias >= 0 && diffDias <= 7;
      }
      if (filtro === 'quincena') {
        return diffDias >= 0 && diffDias <= 15;
      }
      if (filtro === 'mes') {
        return diffDias >= 0 && diffDias <= 30;
      }
      return true;
    });
  }

  private nombreCategoria(categoria?: string | null, categoriaId?: string | null): string {
    const nombre = categoria?.trim();
    if (nombre && !this.esUid(nombre)) return nombre;

    const id = categoriaId || nombre;
    const match = this.stateService.categorias().find(cat => cat.id === id);
    return match?.nombre ?? 'Otros';
  }

  private esUid(valor: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(valor);
  }
}