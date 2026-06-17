import { Component, OnInit, inject, signal, computed, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FinancieroService } from '../../../core/services/Financiero.service';
import { ClienteMetasLimitesService } from '../../../core/services/cliente-metas-limites.service';
import { ClientePerfilService } from '../../../core/services/cliente-perfil.service';
import { AuthService } from '../../../core/services/auth.service';
import { SuscripcionService } from '../../../core/services/suscripcion.service';
import { AppEventBus } from '../../../core/services/app-event-bus.service';
import { ResumenFinancieroDTO } from '../../../core/models/financiero/resumen.model';
import { SolicitudPerfilFinanciero } from '../../../core/models/cliente/perfil-cliente.model';
import { RespuestaMetaAhorro } from '../../../core/models/cliente/meta-limite.model';
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
  anio: number;
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
  imports: [CommonModule, FormsModule],
  templateUrl: './perfil-financiero.html',
  styleUrl: './perfil-financiero.scss',
})
export class PerfilFinanciero implements OnInit {
  public auth = inject(AuthService);
  private financieroService = inject(FinancieroService);
  private metasService = inject(ClienteMetasLimitesService);
  private perfilService = inject(ClientePerfilService);
  private authService = this.auth;
  private suscripcionService = inject(SuscripcionService);
  private eventBus = inject(AppEventBus);
  private destroyRef = inject(DestroyRef);

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

  // ── Modal Configuración ──────────────────────────────────────
  modalConfigAbierto = signal<boolean>(false);
  configurando = signal<boolean>(false);
  pasoActual = signal<number>(1);

  formConfig = signal<SolicitudPerfilFinanciero>({
    ocupacion: '',
    ingresoMensual: 0,
    estiloVida: 'MODERADO',
    tonoIA: 'AMIGABLE'
  });

  estiloVidaSliderVal = computed(() => {
    const estilo = this.formConfig().estiloVida;
    if (estilo === 'AHORRATIVO') return 1;
    if (estilo === 'MODERADO') return 2;
    if (estilo === 'GASTADOR') return 3;
    if (estilo === 'INVERSOR') return 4;
    return 2;
  });

  erroresConfig = signal<{ [key: string]: string }>({});
  mensajeConfig = signal<{ texto: string, tipo: 'success' | 'error' } | null>(null);

  // ── Handlers Modal ───────────────────────────────────────────
  abrirModalConfig(): void {
    const usuario = this.authService.usuario();
    if (!usuario) return;

    // Cargar perfil real del backend para el modal
    this.perfilService.consultarPerfilFinanciero(usuario.id).subscribe({
      next: (perfil) => {
        const totalIngresos = this.resumenActual()?.totalIngresos ?? 0;
        
        // Normalización estricta de valores cargados del backend
        const backendEstilo = (perfil.estiloVida || '').toUpperCase().trim();
        const backendTono = (perfil.tonoIA || '').toUpperCase().trim();
        
        let estiloVida = 'MODERADO';
        if (['AHORRATIVO', 'MODERADO', 'GASTADOR', 'INVERSOR'].includes(backendEstilo)) {
          estiloVida = backendEstilo;
        }

        let tonoIA = 'AMIGABLE';
        if (['FORMAL', 'AMIGABLE', 'MOTIVADOR', 'DIRECTO'].includes(backendTono)) {
          tonoIA = backendTono;
        }

        this.formConfig.set({
          ocupacion: perfil.ocupacion || '',
          ingresoMensual: totalIngresos,
          estiloVida: estiloVida,
          tonoIA: tonoIA
        });
        this.pasoActual.set(1);
        this.modalConfigAbierto.set(true);
      },
      error: () => {
        // Fallback si no existe, abre con los datos por defecto
        const totalIngresos = this.resumenActual()?.totalIngresos ?? 0;
        this.formConfig.set({
          ocupacion: '',
          ingresoMensual: totalIngresos,
          estiloVida: 'MODERADO',
          tonoIA: 'AMIGABLE'
        });
        this.pasoActual.set(1);
        this.modalConfigAbierto.set(true);
      }
    });
  }

  cerrarModalConfig(): void {
    this.modalConfigAbierto.set(false);
    this.erroresConfig.set({});
    this.mensajeConfig.set(null);
    this.pasoActual.set(1);
  }

  onEstiloVidaSliderChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const val = Number(target.value);
    let estilo = 'MODERADO';
    if (val === 1) estilo = 'AHORRATIVO';
    else if (val === 2) estilo = 'MODERADO';
    else if (val === 3) estilo = 'GASTADOR';
    else if (val === 4) estilo = 'INVERSOR';
    this.actualizarCampoConfig('estiloVida', estilo);
  }

  seleccionarTonoIA(tono: string): void {
    this.actualizarCampoConfig('tonoIA', tono);
  }

  avanzarPaso(): void {
    if (this.pasoActual() === 1) {
      // Validar datos básicos
      const data = this.formConfig();
      const errores = { ...this.erroresConfig() };
      let hasError = false;

      if (!data.ocupacion || data.ocupacion.trim().length < 3) {
        errores['ocupacion'] = 'La ocupación debe tener al menos 3 caracteres.';
        hasError = true;
      } else {
        delete errores['ocupacion'];
      }

      this.erroresConfig.set(errores);
      if (hasError) return;
    }

    if (this.pasoActual() < 3) {
      this.pasoActual.update(p => p + 1);
    }
  }

  retrocederPaso(): void {
    if (this.pasoActual() > 1) {
      this.pasoActual.update(p => p - 1);
    }
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

  actualizarCampoConfig(campo: keyof SolicitudPerfilFinanciero, valor: string | number): void {
    this.formConfig.update(f => ({ ...f, [campo]: valor }));
    // Limpiar error de ese campo
    const errs = { ...this.erroresConfig() };
    delete errs[campo];
    this.erroresConfig.set(errs);
  }

  validarFormConfig(): boolean {
    const data = this.formConfig();
    const errores: { [key: string]: string } = {};

    if (!data.ocupacion || data.ocupacion.trim().length < 3) {
      errores['ocupacion'] = 'La ocupación debe tener al menos 3 caracteres.';
    }
    if (data.ingresoMensual === null || data.ingresoMensual === undefined || data.ingresoMensual < 0) {
      errores['ingresoMensual'] = 'El ingreso mensual no puede ser negativo.';
    }
    if (!data.estiloVida) {
      errores['estiloVida'] = 'Selecciona un estilo de vida.';
    }
    if (!data.tonoIA) {
      errores['tonoIA'] = 'Selecciona un tono para la IA.';
    }

    this.erroresConfig.set(errores);
    return Object.keys(errores).length === 0;
  }

  guardarConfiguracion(): void {
    if (!this.validarFormConfig()) return;

    const usuario = this.authService.usuario();
    if (!usuario) return;

    this.configurando.set(true);
    this.mensajeConfig.set(null);

    this.perfilService.guardarPerfilFinanciero(usuario.id, this.formConfig()).subscribe({
      next: () => {
        this.configurando.set(false);
        this.mensajeConfig.set({ texto: 'Perfil financiero actualizado exitosamente.', tipo: 'success' });
        setTimeout(() => this.cerrarModalConfig(), 2000);
      },
      error: () => {
        this.configurando.set(false);
        this.mensajeConfig.set({ texto: 'Ocurrió un error al guardar. Intenta de nuevo.', tipo: 'error' });
      }
    });
  }

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

  progresoLogros = computed(() => {
    const total = this.logrosFinancieros().length;
    const desbloqueados = this.logrosFinancieros().filter(l => l.desbloqueado).length;
    return { desbloqueados, total };
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
    if (this.mostrarTodosLogros()) {
      return todos;
    }
    return todos.filter(l => l.desbloqueado).slice(0, 3);
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

  ngOnInit(): void {
    this.cargarDatos();
    this.eventBus.on('TRANSACTION_MODIFIED')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cargarDatos());
  }

  onMesChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.mesSeleccionado.set(Number(select.value));
    this.cargarDatos();
  }

  onAnioChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.anioSeleccionado.set(Number(select.value));
    this.cargarDatos();
  }

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

  exportarPdf(): void {
    const resumen = this.resumenActual();
    const nombresMeses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    const periodoLabel = `${nombresMeses[this.mesSeleccionado() - 1]} ${this.anioSeleccionado()}`;
    const salud = this.indicesSalud();
    const ahorro = this.capacidadAhorro();
    const logros = this.progresoLogros();
    const fechaGeneracion = new Date().toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' });
    const html = this.construirReportePdf(resumen, periodoLabel, salud, ahorro, logros, fechaGeneracion);
    const ventana = window.open('', '_blank', 'width=980,height=720');
    if (!ventana) return;

    ventana.document.open();
    ventana.document.write(html);
    ventana.document.close();
    ventana.focus();
    setTimeout(() => ventana.print(), 350);
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

  formatMoneda(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatMonedaSinDecimales(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  }

  private construirReportePdf(
    resumen: ResumenFinancieroDTO | null,
    periodo: string,
    salud: { score: number; etiqueta: string } | null,
    ahorro: number | null,
    logros: { desbloqueados: number; total: number },
    fechaGeneracion: string
  ): string {
    const ingresos = resumen?.totalIngresos ?? 0;
    const gastos = resumen?.totalGastos ?? 0;
    const saldo = ingresos - gastos;
    const rowsTendencia = this.tendencia().map(punto => `
      <tr><td>${punto.mes} ${punto.anio}</td><td>S/ ${this.formatMoneda(punto.ingresos)}</td><td>S/ ${this.formatMoneda(punto.gastos)}</td><td>S/ ${this.formatMoneda(punto.ahorro)}</td></tr>
    `).join('');

    return `<!doctype html>
      <html lang="es">
      <head>
        <meta charset="utf-8">
        <title>Perfil financiero - ${periodo}</title>
        <style>
          @page { size: A4; margin: 16mm; }
          body { margin: 0; font-family: Arial, sans-serif; color: #0f172a; background: #fff; }
          .hero { padding: 22px; border-radius: 18px; color: #fff; background: linear-gradient(135deg, #4f46e5, #7c3aed); }
          h1 { margin: 0 0 6px; font-size: 26px; }
          h2 { margin: 22px 0 10px; font-size: 17px; color: #312e81; }
          p { margin: 0; color: inherit; }
          .muted { color: #64748b; font-size: 12px; }
          .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 18px 0; }
          .card { border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px; background: #f8fafc; }
          .label { display: block; color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; }
          .value { display: block; margin-top: 6px; font-size: 21px; font-weight: 800; }
          table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 12px; }
          th, td { border-bottom: 1px solid #e2e8f0; padding: 8px; text-align: left; }
          th { background: #eef2ff; color: #312e81; }
          .footer { margin-top: 24px; color: #64748b; font-size: 11px; }
        </style>
      </head>
      <body>
        <section class="hero">
          <h1>Reporte de Perfil Financiero</h1>
          <p>Periodo: ${periodo}</p>
          <p>Generado: ${fechaGeneracion}</p>
        </section>
        <section class="grid">
          <article class="card"><span class="label">Ingresos</span><span class="value">S/ ${this.formatMoneda(ingresos)}</span></article>
          <article class="card"><span class="label">Gastos</span><span class="value">S/ ${this.formatMoneda(gastos)}</span></article>
          <article class="card"><span class="label">Saldo operativo</span><span class="value">S/ ${this.formatMoneda(saldo)}</span></article>
          <article class="card"><span class="label">Salud financiera</span><span class="value">${salud ? `${salud.score}/100` : 'Sin datos'}</span><span class="muted">${salud?.etiqueta ?? ''}</span></article>
          <article class="card"><span class="label">Tasa de ahorro</span><span class="value">${ahorro !== null ? `${ahorro.toFixed(1)}%` : 'Sin datos'}</span></article>
          <article class="card"><span class="label">Logros</span><span class="value">${logros.desbloqueados}/${logros.total}</span></article>
        </section>
        <section>
          <h2>Tendencia financiera</h2>
          <table><thead><tr><th>Mes</th><th>Ingresos</th><th>Gastos</th><th>Ahorro</th></tr></thead><tbody>${rowsTendencia}</tbody></table>
        </section>
        <p class="footer">Reporte generado desde Luka App. Para guardar como PDF, selecciona “Guardar como PDF” en el diálogo de impresión.</p>
      </body>
      </html>`;
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
