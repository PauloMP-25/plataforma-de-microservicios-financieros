import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FinancieroService } from '../../../core/services/Financiero.service';
import { Transacciones } from '../../../core/services/transacciones';
import { ClienteMetasLimitesService } from '../../../core/services/cliente-metas-limites.service';
import { AppEventBus } from '../../../core/services/app-event-bus.service';
import { ResumenFinancieroDTO } from '../../../core/models/financiero/resumen.model';
import { forkJoin } from 'rxjs';

export interface LogroFinanciero {
  id: string;
  titulo: string;
  descripcion: string;
  icono: string;
  iconoColor: string;
  desbloqueado: boolean;
  progreso: number;
  meta: number;
  categoria: string;
}

export interface PuntoTendencia {
  mes: string;
  ingresos: number;
  gastos: number;
  ahorro: number;
}

export interface CategoriaComposicion {
  nombre: string;
  porcentaje: number;
  monto: number;
  color: string;
}

@Component({
  selector: 'app-perfil-financiero',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './perfil-financiero.html',
  styleUrl: './perfil-financiero.scss',
})
export class PerfilFinanciero implements OnInit {
  private financieroService = inject(FinancieroService);
  private transaccionesService = inject(Transacciones);
  private metasService = inject(ClienteMetasLimitesService);
  private eventBus = inject(AppEventBus);

  // ── Estado ──────────────────────────────────────────────────
  cargando = signal<boolean>(false);
  resumenActual = signal<ResumenFinancieroDTO | null>(null);
  resumenAnterior = signal<ResumenFinancieroDTO | null>(null);
  metasCompletadas = signal<number>(0);
  metasTotal = signal<number>(0);
  filtroTendencia = signal<3 | 6 | 12>(6);
  filtroComposicionMes = signal<number>(new Date().getMonth() + 1);
  filtroComposicionAnio = signal<number>(new Date().getFullYear());
  tendencia = signal<PuntoTendencia[]>([]);
  mostrarTodosLogros = signal<boolean>(false);
  vistaDona = signal<'ingresos' | 'gastos'>('ingresos');

  // ── KPIs Computados ─────────────────────────────────────────
  balanceNeto = computed(() => {
    const r = this.resumenActual();
    if (!r) return null;
    return r.totalIngresos - r.totalGastos;
  });

  indicesSalud = computed(() => {
    const r = this.resumenActual();
    if (!r || r.totalIngresos === 0) return null;
    const ratio = ((r.totalIngresos - r.totalGastos) / r.totalIngresos) * 100;
    const score = Math.min(100, Math.max(0, ratio));
    let etiqueta = 'Crítico';
    if (score >= 75) etiqueta = 'Excelente';
    else if (score >= 50) etiqueta = 'Saludable';
    else if (score >= 25) etiqueta = 'Regular';
    return { score: Math.round(score), etiqueta };
  });

  capacidadAhorro = computed(() => {
    const r = this.resumenActual();
    if (!r || r.totalIngresos === 0) return null;
    return ((r.totalIngresos - r.totalGastos) / r.totalIngresos) * 100;
  });

  progresoLogros = computed(() => {
    const total = this.logrosFinancieros().length;
    const desbloqueados = this.logrosFinancieros().filter(l => l.desbloqueado).length;
    return { desbloqueados, total };
  });

  variacionBalance = computed(() => {
    const actual = this.balanceNeto();
    const ant = this.resumenAnterior();
    if (actual === null || !ant) return null;
    const balanceAnt = ant.totalIngresos - ant.totalGastos;
    if (balanceAnt === 0) return null;
    return ((actual - balanceAnt) / Math.abs(balanceAnt)) * 100;
  });

  variacionAhorro = computed(() => {
    const r = this.resumenActual();
    const ant = this.resumenAnterior();
    if (!r || !ant || r.totalIngresos === 0 || ant.totalIngresos === 0) return null;
    const capacidadActual = ((r.totalIngresos - r.totalGastos) / r.totalIngresos) * 100;
    const capacidadAnt = ((ant.totalIngresos - ant.totalGastos) / ant.totalIngresos) * 100;
    return capacidadActual - capacidadAnt;
  });

  // ── Logros ──────────────────────────────────────────────────
  logrosFinancieros = computed<LogroFinanciero[]>(() => {
    const r = this.resumenActual();
    const metas = this.metasCompletadas();
    const totalMetas = this.metasTotal();
    if (!r) return this.logrosMock();
    return [
      {
        id: 'primer-ingreso',
        titulo: 'Primer ingreso',
        descripcion: 'Registra tu primer ingreso',
        icono: 'fa-solid fa-circle-dollar-to-slot',
        iconoColor: 'success',
        desbloqueado: r.cantidadIngresos >= 1,
        progreso: Math.min(r.cantidadIngresos, 1),
        meta: 1,
        categoria: 'ingresos',
      },
      {
        id: 'acumulado-2000',
        titulo: 'Has acumulado S/ 2,000',
        descripcion: 'Acumula S/ 2,000 en ingresos',
        icono: 'fa-solid fa-sack-dollar',
        iconoColor: 'warning',
        desbloqueado: r.totalIngresos >= 2000,
        progreso: Math.min(r.totalIngresos, 2000),
        meta: 2000,
        categoria: 'ingresos',
      },
      {
        id: 'racha-movimientos',
        titulo: 'Racha de 30 días',
        descripcion: 'Registra movimientos 30 días seguidos',
        icono: 'fa-solid fa-fire',
        iconoColor: 'danger',
        desbloqueado: r.totalTransacciones >= 30,
        progreso: Math.min(r.totalTransacciones, 30),
        meta: 30,
        categoria: 'movimientos',
      },
      {
        id: 'primera-meta',
        titulo: 'Primera meta completada',
        descripcion: 'Completa tu primera meta de ahorro',
        icono: 'fa-solid fa-bullseye',
        iconoColor: 'primary',
        desbloqueado: metas >= 1,
        progreso: Math.min(metas, 1),
        meta: 1,
        categoria: 'metas',
      },
      {
        id: 'cien-movimientos',
        titulo: 'Registrar 100 movimientos',
        descripcion: 'Lleva un control consistente de tus finanzas',
        icono: 'fa-solid fa-list-check',
        iconoColor: 'info',
        desbloqueado: r.totalTransacciones >= 100,
        progreso: Math.min(r.totalTransacciones, 100),
        meta: 100,
        categoria: 'movimientos',
      },
      {
        id: 'ahorro-10000',
        titulo: 'Ahorrar S/ 10,000',
        descripcion: 'Supera los S/ 10,000 acumulados en ingresos',
        icono: 'fa-solid fa-piggy-bank',
        iconoColor: 'success',
        desbloqueado: r.totalIngresos >= 10000,
        progreso: Math.min(r.totalIngresos, 10000),
        meta: 10000,
        categoria: 'ahorro',
      },
      {
        id: 'salud-financiera',
        titulo: 'Mantener salud financiera saludable',
        descripcion: 'Mantén un índice de salud mayor a 50 durante 3 meses',
        icono: 'fa-solid fa-heart-pulse',
        iconoColor: 'danger',
        desbloqueado: (this.indicesSalud()?.score ?? 0) >= 50,
        progreso: Math.min((this.indicesSalud()?.score ?? 0) / 50, 1) * 3,
        meta: 3,
        categoria: 'salud',
      },
      {
        id: 'completar-5-metas',
        titulo: 'Completar 5 metas',
        descripcion: 'Alcanza 5 metas de ahorro',
        icono: 'fa-solid fa-trophy',
        iconoColor: 'warning',
        desbloqueado: metas >= 5,
        progreso: Math.min(metas, 5),
        meta: 5,
        categoria: 'metas',
      },
    ];
  });

  logrosVisibles = computed(() => {
    const todos = this.logrosFinancieros();
    return this.mostrarTodosLogros() ? todos : todos.slice(0, 4);
  });

  // ── Composición por categorías (mock avanzado) ───────────────
  composicionIngresos = computed<CategoriaComposicion[]>(() => {
    const r = this.resumenActual();
    if (!r || r.totalIngresos === 0) return [];
    const total = r.totalIngresos;
    return [
      { nombre: 'Salario', porcentaje: 68, monto: total * 0.68, color: '#22c55e' },
      { nombre: 'Freelance', porcentaje: 18, monto: total * 0.18, color: '#5b6af0' },
      { nombre: 'Inversiones', porcentaje: 9, monto: total * 0.09, color: '#f59e0b' },
      { nombre: 'Otros', porcentaje: 5, monto: total * 0.05, color: '#8290af' },
    ];
  });

  composicionGastos = computed<CategoriaComposicion[]>(() => {
    const r = this.resumenActual();
    if (!r || r.totalGastos === 0) return [];
    const total = r.totalGastos;
    return [
      { nombre: 'Vivienda', porcentaje: 40, monto: total * 0.40, color: '#ef4444' },
      { nombre: 'Alimentación', porcentaje: 25, monto: total * 0.25, color: '#f59e0b' },
      { nombre: 'Transporte', porcentaje: 15, monto: total * 0.15, color: '#5b6af0' },
      { nombre: 'Otros', porcentaje: 20, monto: total * 0.20, color: '#8290af' },
    ];
  });

  composicionActual = computed(() =>
    this.vistaDona() === 'ingresos' ? this.composicionIngresos() : this.composicionGastos()
  );

  totalComposicion = computed(() =>
    this.vistaDona() === 'ingresos'
      ? this.resumenActual()?.totalIngresos ?? 0
      : this.resumenActual()?.totalGastos ?? 0
  );

  // ── Tendencia SVG ────────────────────────────────────────────
  tendenciaNormalizada = computed(() => {
    const puntos = this.tendencia();
    if (puntos.length === 0) return { ingresos: [], gastos: [], ahorro: [] };
    const todos = puntos.flatMap(p => [p.ingresos, p.gastos, p.ahorro]);
    const maxVal = Math.max(...todos, 1);
    const h = 160;
    const w = 480;
    const mapY = (v: number) => h - (v / maxVal) * (h - 20);
    const mapX = (i: number) => (i / (puntos.length - 1)) * w;
    const toPath = (valores: number[]) =>
      valores.map((v, i) => `${i === 0 ? 'M' : 'L'} ${mapX(i).toFixed(1)},${mapY(v).toFixed(1)}`).join(' ');
    return {
      ingresos: toPath(puntos.map(p => p.ingresos)),
      gastos: toPath(puntos.map(p => p.gastos)),
      ahorro: toPath(puntos.map(p => p.ahorro)),
      puntos,
      maxVal,
      mapY,
      mapX,
      h,
      w,
    };
  });

  // ── Dona SVG ─────────────────────────────────────────────────
  donaSegmentos = computed(() => {
    const cats = this.composicionActual();
    const radio = 60;
    const cx = 75;
    const cy = 75;
    let acumulado = 0;
    return cats.map(cat => {
      const inicio = acumulado;
      acumulado += cat.porcentaje;
      const fin = acumulado;
      const anguloInicio = (inicio / 100) * 2 * Math.PI - Math.PI / 2;
      const anguloFin = (fin / 100) * 2 * Math.PI - Math.PI / 2;
      const x1 = cx + radio * Math.cos(anguloInicio);
      const y1 = cy + radio * Math.sin(anguloInicio);
      const x2 = cx + radio * Math.cos(anguloFin);
      const y2 = cy + radio * Math.sin(anguloFin);
      const largeArc = fin - inicio > 50 ? 1 : 0;
      const d = `M ${cx},${cy} L ${x1.toFixed(2)},${y1.toFixed(2)} A ${radio},${radio} 0 ${largeArc},1 ${x2.toFixed(2)},${y2.toFixed(2)} Z`;
      return { ...cat, d };
    });
  });

  readonly meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

  ngOnInit(): void {
    this.cargarDatos();
    this.eventBus.on('TRANSACTION_MODIFIED').subscribe(() => this.cargarDatos());
  }

  cargarDatos(): void {
    this.cargando.set(true);
    const hoy = new Date();
    const mesAnterior = hoy.getMonth() === 0 ? 12 : hoy.getMonth();
    const anioAnterior = hoy.getMonth() === 0 ? hoy.getFullYear() - 1 : hoy.getFullYear();

    forkJoin({
      actual: this.financieroService.getResumen(),
      anterior: this.financieroService.getResumen(mesAnterior, anioAnterior),
      metas: this.metasService.listarMetas(),
    }).subscribe({
      next: ({ actual, anterior, metas }) => {
        this.resumenActual.set(actual);
        this.resumenAnterior.set(anterior);
        const completadas = metas.filter(m => m.completada).length;
        this.metasCompletadas.set(completadas);
        this.metasTotal.set(metas.length);
        this.generarTendencia(this.filtroTendencia());
        this.cargando.set(false);
      },
      error: () => {
        this.resumenActual.set({
          desde: new Date().toISOString(),
          hasta: new Date().toISOString(),
          totalIngresos: 6800,
          totalGastos: 2950,
          balance: 3850,
          cantidadIngresos: 5,
          cantidadGastos: 22,
          totalTransacciones: 27,
          promedioIngreso: 1360,
          promedioGasto: 134.09,
        });
        this.resumenAnterior.set({
          desde: new Date().toISOString(),
          hasta: new Date().toISOString(),
          totalIngresos: 6050,
          totalGastos: 2800,
          balance: 3250,
          cantidadIngresos: 4,
          cantidadGastos: 18,
          totalTransacciones: 22,
          promedioIngreso: 1512.5,
          promedioGasto: 155.56,
        });
        this.metasCompletadas.set(1);
        this.metasTotal.set(6);
        this.generarTendencia(this.filtroTendencia());
        this.cargando.set(false);
      },
    });
  }

  generarTendencia(meses: 3 | 6 | 12): void {
    const puntos: PuntoTendencia[] = [];
    const hoy = new Date();
    for (let i = meses - 1; i >= 0; i--) {
      const fecha = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      const ing = 4500 + Math.random() * 3500;
      const gas = 2200 + Math.random() * 1500;
      puntos.push({
        mes: this.meses[fecha.getMonth()],
        ingresos: Math.round(ing),
        gastos: Math.round(gas),
        ahorro: Math.round(Math.max(0, ing - gas)),
      });
    }
    this.tendencia.set(puntos);
  }

  cambiarTendencia(meses: 3 | 6 | 12): void {
    this.filtroTendencia.set(meses);
    this.generarTendencia(meses);
  }

  toggleLogros(): void {
    this.mostrarTodosLogros.update(v => !v);
  }

  cambiarDona(vista: 'ingresos' | 'gastos'): void {
    this.vistaDona.set(vista);
  }

  etiquetaIndice(score: number): string {
    if (score >= 75) return 'Excelente';
    if (score >= 50) return 'Saludable';
    if (score >= 25) return 'Regular';
    return 'Crítico';
  }

  colorIndice(score: number): string {
    if (score >= 75) return 'success';
    if (score >= 50) return 'info';
    if (score >= 25) return 'warning';
    return 'danger';
  }

  formatMoneda(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  private logrosMock(): LogroFinanciero[] {
    return [
      { id: 'primer-ingreso', titulo: 'Primer ingreso', descripcion: 'Registra tu primer ingreso', icono: 'fa-solid fa-circle-dollar-to-slot', iconoColor: 'success', desbloqueado: true, progreso: 1, meta: 1, categoria: 'ingresos' },
      { id: 'acumulado-2000', titulo: 'Has acumulado S/ 2,000', descripcion: 'Acumula S/ 2,000 en ingresos', icono: 'fa-solid fa-sack-dollar', iconoColor: 'warning', desbloqueado: true, progreso: 2000, meta: 2000, categoria: 'ingresos' },
      { id: 'racha-movimientos', titulo: 'Racha de 30 días', descripcion: 'Registra movimientos 30 días seguidos', icono: 'fa-solid fa-fire', iconoColor: 'danger', desbloqueado: true, progreso: 30, meta: 30, categoria: 'movimientos' },
      { id: 'primera-meta', titulo: 'Primera meta completada', descripcion: 'Completa tu primera meta de ahorro', icono: 'fa-solid fa-bullseye', iconoColor: 'primary', desbloqueado: true, progreso: 1, meta: 1, categoria: 'metas' },
      { id: 'cien-movimientos', titulo: 'Registrar 100 movimientos', descripcion: 'Lleva un control consistente', icono: 'fa-solid fa-list-check', iconoColor: 'info', desbloqueado: false, progreso: 75, meta: 100, categoria: 'movimientos' },
      { id: 'ahorro-10000', titulo: 'Ahorrar S/ 10,000', descripcion: 'Supera los S/ 10,000 acumulados', icono: 'fa-solid fa-piggy-bank', iconoColor: 'success', desbloqueado: false, progreso: 6200, meta: 10000, categoria: 'ahorro' },
      { id: 'salud-financiera', titulo: 'Mantener salud saludable', descripcion: 'Mantén índice > 50 durante 3 meses', icono: 'fa-solid fa-heart-pulse', iconoColor: 'danger', desbloqueado: false, progreso: 2, meta: 3, categoria: 'salud' },
      { id: 'completar-5-metas', titulo: 'Completar 5 metas', descripcion: 'Alcanza 5 metas de ahorro', icono: 'fa-solid fa-trophy', iconoColor: 'warning', desbloqueado: false, progreso: 3, meta: 5, categoria: 'metas' },
    ];
  }
}
