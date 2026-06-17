import { Injectable, inject, signal, computed } from '@angular/core';
import { FinancieroService } from '../../../../core/services/Financiero.service';
import { ClienteMetasLimitesService } from '../../../../core/services/cliente-metas-limites.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SuscripcionService } from '../../../../core/services/suscripcion.service';
import { ResumenFinancieroDTO } from '../../../../core/models/financiero/resumen.model';
import { RespuestaMetaAhorro } from '../../../../core/models/cliente/meta-limite.model';
import { forkJoin } from 'rxjs';

export interface PuntoTendencia {
  mes: string;
  anio: number;
  ingresos: number;
  gastos: number;
  ahorro: number;
}

@Injectable({
  providedIn: 'root'
})
export class PerfilFinancieroService {
  public auth = inject(AuthService);
  private financieroService = inject(FinancieroService);
  private metasService = inject(ClienteMetasLimitesService);
  private suscripcionService = inject(SuscripcionService);

  // ── Estado ──────────────────────────────────────────────────
  cargando = signal<boolean>(false);
  resumenActual = signal<ResumenFinancieroDTO | null>(null);
  resumenAnterior = signal<ResumenFinancieroDTO | null>(null);
  metasCompletadas = signal<number>(0);
  metasTotal = signal<number>(0);
  filtroTendencia = signal<3 | 6 | 12>(6);
  tendencia = signal<PuntoTendencia[]>([]);
  mostrarTodosLogros = signal<boolean>(false);
  modalPlanesAbierto = signal<boolean>(false);
  comprandoPlan = signal<boolean>(false);

  mesSeleccionado = signal<number>(5);
  anioSeleccionado = signal<number>(2025);

  mesesDisponibles = [
    { valor: 1, label: 'Enero' },
    { valor: 2, label: 'Febrero' },
    { valor: 3, label: 'Marzo' },
    { valor: 4, label: 'Abril' },
    { valor: 5, label: 'Mayo' },
    { valor: 6, label: 'Junio' },
    { valor: 7, label: 'Julio' },
    { valor: 8, label: 'Agosto' },
    { valor: 9, label: 'Septiembre' },
    { valor: 10, label: 'Octubre' },
    { valor: 11, label: 'Noviembre' },
    { valor: 12, label: 'Diciembre' }
  ];

  aniosDisponibles = [2024, 2025, 2026];

  // ── KPIs Computados ─────────────────────────────────────────
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

  variacionAhorro = computed(() => {
    const r = this.resumenActual();
    const ant = this.resumenAnterior();
    if (!r || !ant || r.totalIngresos === 0 || ant.totalIngresos === 0) return null;
    const capacidadActual = ((r.totalIngresos - r.totalGastos) / r.totalIngresos) * 100;
    const capacidadAnt = ((ant.totalIngresos - ant.totalGastos) / ant.totalIngresos) * 100;
    return capacidadActual - capacidadAnt;
  });

  variacionSalud = computed(() => {
    const actual = this.indicesSalud();
    const ant = this.resumenAnterior();
    if (!actual || !ant || ant.totalIngresos === 0) return null;
    const ratioAnt = ((ant.totalIngresos - ant.totalGastos) / ant.totalIngresos) * 100;
    const scoreAnt = Math.min(100, Math.max(0, ratioAnt));
    return actual.score - Math.round(scoreAnt);
  });

  nombreMesAnterior = computed(() => {
    const nombresMeses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    const mes = this.mesSeleccionado();
    const anio = this.anioSeleccionado();
    const mesAnterior = mes === 1 ? 12 : mes - 1;
    const anioAnterior = mes === 1 ? anio - 1 : anio;
    return `${nombresMeses[mesAnterior - 1]} ${anioAnterior}`;
  });

  // ── Tendencia SVG ────────────────────────────────────────────
  tendenciaNormalizada = computed(() => {
    const puntos = this.tendencia();
    const h = 160;
    const w = 500;
    const mapY = (v: number) => {
      const maxVal = puntos.length > 0 ? Math.max(...puntos.flatMap(p => [p.ingresos, p.gastos]), 1) : 1;
      return h - (v / maxVal) * (h - 20);
    };
    const mapX = (i: number) => {
      const denom = puntos.length > 1 ? puntos.length - 1 : 1;
      return 10 + (i / denom) * (w - 20);
    };

    if (puntos.length === 0) {
      return {
        ingresos: '',
        gastos: '',
        puntos: [],
        maxVal: 1,
        mapY,
        mapX,
        h,
        w,
      };
    }

    const toPath = (valores: number[]) =>
      valores.map((v, i) => `${i === 0 ? 'M' : 'L'} ${mapX(i).toFixed(1)},${mapY(v).toFixed(1)}`).join(' ');

    return {
      ingresos: toPath(puntos.map(p => p.ingresos)),
      gastos: toPath(puntos.map(p => p.gastos)),
      puntos,
      maxVal: Math.max(...puntos.flatMap(p => [p.ingresos, p.gastos]), 1),
      mapY,
      mapX,
      h,
      w,
    };
  });

  readonly meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

  cargarDatos(): void {
    this.cargando.set(true);
    const mes = this.mesSeleccionado();
    const anio = this.anioSeleccionado();
    const mesAnterior = mes === 1 ? 12 : mes - 1;
    const anioAnterior = mes === 1 ? anio - 1 : anio;

    forkJoin({
      actual: this.financieroService.getResumen(mes, anio),
      anterior: this.financieroService.getResumen(mesAnterior, anioAnterior),
      metas: this.metasService.listarMetas(),
    }).subscribe({
      next: ({ actual, anterior, metas }) => {
        this.resumenActual.set(actual);
        this.resumenAnterior.set(anterior);
        const completadas = metas.content.filter((m: RespuestaMetaAhorro) => m.completada).length;
        this.metasCompletadas.set(completadas);
        this.metasTotal.set(metas.content.length);
        this.generarTendencia(this.filtroTendencia());
        this.cargando.set(false);
      },
      error: () => {
        const factorMes = mes * 230;
        const factorAnio = (anio % 10) * 120;
        const totalIngresos = 3800 + (factorMes % 1500) + factorAnio;
        const totalGastos = 2200 + (factorMes % 900) + (factorAnio % 400);
        const balance = totalIngresos - totalGastos;
        const cantIng = 2 + (mes % 3);
        const cantGas = 10 + (mes % 8);

        const factorMesAnt = mesAnterior * 230;
        const factorAnioAnt = (anioAnterior % 10) * 120;
        const totalIngresosAnt = 3800 + (factorMesAnt % 1500) + factorAnioAnt;
        const totalGastosAnt = 2200 + (factorMesAnt % 900) + (factorAnioAnt % 400);
        const balanceAnt = totalIngresosAnt - totalGastosAnt;
        const cantIngAnt = 2 + (mesAnterior % 3);
        const cantGasAnt = 10 + (mesAnterior % 8);

        this.resumenActual.set({
          desde: new Date(anio, mes - 1, 1).toISOString(),
          hasta: new Date(anio, mes, 0).toISOString(),
          totalIngresos,
          totalGastos,
          balance,
          cantidadIngresos: cantIng,
          cantidadGastos: cantGas,
          totalTransacciones: cantIng + cantGas,
          promedioIngreso: Math.round(totalIngresos / cantIng),
          promedioGasto: Math.round(totalGastos / cantGas),
        });
        this.resumenAnterior.set({
          desde: new Date(anioAnterior, mesAnterior - 1, 1).toISOString(),
          hasta: new Date(anioAnterior, mesAnterior, 0).toISOString(),
          totalIngresos: totalIngresosAnt,
          totalGastos: totalGastosAnt,
          balance: balanceAnt,
          cantidadIngresos: cantIngAnt,
          cantidadGastos: cantGasAnt,
          totalTransacciones: cantIngAnt + cantGasAnt,
          promedioIngreso: Math.round(totalIngresosAnt / cantIngAnt),
          promedioGasto: Math.round(totalGastosAnt / cantGasAnt),
        });
        this.metasCompletadas.set(3);
        this.metasTotal.set(8);
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
        anio: fecha.getFullYear(),
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

  colorIndice(score: number): string {
    if (score >= 75) return 'success';
    if (score >= 50) return 'info';
    if (score >= 25) return 'warning';
    return 'danger';
  }

  abrirModalPlanes(): void {
    this.modalPlanesAbierto.set(true);
  }

  cerrarModalPlanes(): void {
    this.modalPlanesAbierto.set(false);
  }

  comprarPlan(plan: 'PRO' | 'PREMIUM'): void {
    if (this.comprandoPlan()) return;
    this.comprandoPlan.set(true);

    this.suscripcionService.crearSesionCheckout(plan).subscribe({
      next: (sesion) => {
        this.comprandoPlan.set(false);
        if (sesion?.urlCheckout) {
          window.location.href = sesion.urlCheckout;
        }
      },
      error: () => {
        this.comprandoPlan.set(false);
      }
    });
  }

  formatMoneda(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatMonedaSinDecimales(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  }
}
