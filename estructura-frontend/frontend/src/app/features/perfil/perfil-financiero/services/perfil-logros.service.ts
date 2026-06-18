import { Injectable, inject, computed } from '@angular/core';
import { PerfilFinancieroService } from './perfil-financiero.service';

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

@Injectable({
  providedIn: 'root'
})
export class PerfilLogrosService {
  private financieroService = inject(PerfilFinancieroService);

  // ── Logros Computados ─────────────────────────────────────────
  logrosFinancieros = computed<LogroFinanciero[]>(() => {
    const r = this.financieroService.resumenActual();
    const metas = this.financieroService.metasCompletadas();
    if (!r) return this.logrosMock();

    const tempLogros: LogroFinanciero[] = [
      {
        id: 'primer-ingreso',
        titulo: 'Primer paso financiero',
        descripcion: 'Registra tu primer ingreso.',
        icono: 'fa-solid fa-circle-dollar-to-slot',
        iconoColor: 'success',
        desbloqueado: r.cantidadIngresos >= 1,
        progreso: Math.min(r.cantidadIngresos, 1),
        meta: 1,
        categoria: 'ingresos',
      },
      {
        id: 'ahorro-inicial',
        titulo: 'Ahorro inicial',
        descripcion: 'Acumula S/ 2,000 en tus ingresos o balance positivo.',
        icono: 'fa-solid fa-sack-dollar',
        iconoColor: 'warning',
        desbloqueado: r.totalIngresos >= 2000,
        progreso: Math.min(r.totalIngresos, 2000),
        meta: 2000,
        categoria: 'ahorro',
      },
      {
        id: 'constancia-diaria',
        titulo: 'Constancia diaria',
        descripcion: 'Mantén una racha de 30 días registrando movimientos.',
        icono: 'fa-solid fa-fire',
        iconoColor: 'danger',
        desbloqueado: r.totalTransacciones >= 30,
        progreso: Math.min(r.totalTransacciones, 30),
        meta: 30,
        categoria: 'movimientos',
      },
      {
        id: 'objetivo-cumplido',
        titulo: 'Objetivo cumplido',
        descripcion: 'Completa tu primera meta financiera.',
        icono: 'fa-solid fa-bullseye',
        iconoColor: 'primary',
        desbloqueado: metas >= 1,
        progreso: Math.min(metas, 1),
        meta: 1,
        categoria: 'metas',
      },
      {
        id: 'registrador-experto',
        titulo: 'Registrador experto',
        descripcion: 'Registra 100 movimientos financieros.',
        icono: 'fa-solid fa-list-check',
        iconoColor: 'info',
        desbloqueado: r.totalTransacciones >= 100,
        progreso: Math.min(r.totalTransacciones, 100),
        meta: 100,
        categoria: 'movimientos',
      },
      {
        id: 'gran-ahorrador',
        titulo: 'Gran ahorrador',
        descripcion: 'Ahorra S/ 10,000.',
        icono: 'fa-solid fa-piggy-bank',
        iconoColor: 'success',
        desbloqueado: r.totalIngresos >= 10000,
        progreso: Math.min(r.totalIngresos, 10000),
        meta: 10000,
        categoria: 'ahorro',
      },
      {
        id: 'salud-estable',
        titulo: 'Salud estable',
        descripcion: 'Mantén una salud financiera saludable durante 3 meses.',
        icono: 'fa-solid fa-heart-pulse',
        iconoColor: 'danger',
        desbloqueado: (this.financieroService.indicesSalud()?.score ?? 0) >= 50,
        progreso: (this.financieroService.indicesSalud()?.score ?? 0) >= 50 ? 3 : 1,
        meta: 3,
        categoria: 'salud',
      },
      {
        id: 'cumplidor-metas',
        titulo: 'Cumplidor de metas',
        descripcion: 'Completa 5 metas financieras.',
        icono: 'fa-solid fa-trophy',
        iconoColor: 'warning',
        desbloqueado: metas >= 5,
        progreso: Math.min(metas, 5),
        meta: 5,
        categoria: 'metas',
      },
      {
        id: 'control-financiero',
        titulo: 'Control financiero',
        descripcion: 'Mantente dentro de tu presupuesto durante 30 días.',
        icono: 'fa-solid fa-shield-halved',
        iconoColor: 'primary',
        desbloqueado: r.totalTransacciones >= 10,
        progreso: r.totalTransacciones >= 10 ? 30 : 12,
        meta: 30,
        categoria: 'presupuesto',
      },
      {
        id: 'ahorrador-constante',
        titulo: 'Ahorrador constante',
        descripcion: 'Ahorra durante 3 meses seguidos.',
        icono: 'fa-solid fa-calendar-check',
        iconoColor: 'success',
        desbloqueado: r.totalIngresos > r.totalGastos,
        progreso: r.totalIngresos > r.totalGastos ? 3 : 1,
        meta: 3,
        categoria: 'ahorro',
      },
      {
        id: 'patrimonio-positivo',
        titulo: 'Patrimonio positivo',
        descripcion: 'Alcanza un patrimonio operativo neto de S/ 5,000.',
        icono: 'fa-solid fa-chart-line',
        iconoColor: 'info',
        desbloqueado: (r.totalIngresos - r.totalGastos) >= 5000,
        progreso: Math.min(Math.max(0, r.totalIngresos - r.totalGastos), 5000),
        meta: 5000,
        categoria: 'balance',
      },
      {
        id: 'categorizador-experto',
        titulo: 'Categorizador experto',
        descripcion: 'Utiliza todas las categorías principales en tus movimientos.',
        icono: 'fa-solid fa-tags',
        iconoColor: 'warning',
        desbloqueado: r.totalTransacciones >= 15,
        progreso: r.totalTransacciones >= 15 ? 5 : 3,
        meta: 5,
        categoria: 'movimientos',
      },
      {
        id: 'cero-excesos',
        titulo: 'Cero excesos',
        descripcion: 'No superes tus límites de presupuesto durante 2 meses.',
        icono: 'fa-solid fa-circle-xmark',
        iconoColor: 'danger',
        desbloqueado: r.totalTransacciones > 0,
        progreso: r.totalTransacciones > 0 ? 2 : 1,
        meta: 2,
        categoria: 'presupuesto',
      },
      {
        id: 'racha-imparable',
        titulo: 'Racha imparable',
        descripcion: 'Mantén actividad financiera durante 100 días.',
        icono: 'fa-solid fa-bolt',
        iconoColor: 'primary',
        desbloqueado: r.totalTransacciones >= 100,
        progreso: Math.min(r.totalTransacciones, 100),
        meta: 100,
        categoria: 'movimientos',
      },
      {
        id: 'maestro-ahorro',
        titulo: 'Maestro del ahorro',
        descripcion: 'Acumula S/ 25,000 ahorrados.',
        icono: 'fa-solid fa-crown',
        iconoColor: 'warning',
        desbloqueado: r.totalIngresos >= 25000,
        progreso: Math.min(r.totalIngresos, 25000),
        meta: 25000,
        categoria: 'ahorro',
      },
    ];

    const desbloqueados = tempLogros.filter(l => l.desbloqueado).length;

    tempLogros.push({
      id: 'leyenda-luka',
      titulo: 'Leyenda Luka',
      descripcion: 'Desbloquea todos los logros del sistema.',
      icono: 'fa-solid fa-star',
      iconoColor: 'success',
      desbloqueado: desbloqueados >= 15,
      progreso: desbloqueados,
      meta: 15,
      categoria: 'general',
    });

    return tempLogros;
  });

  logrosVisibles = computed(() => {
    const todos = this.logrosFinancieros();
    const desbloqueados = todos.filter(l => l.desbloqueado);
    if (desbloqueados.length > 0) {
      return desbloqueados.slice(0, 3);
    }
    return todos.slice(0, 3);
  });

  progresoLogros = computed(() => {
    const total = this.logrosFinancieros().length;
    const desbloqueados = this.logrosFinancieros().filter(l => l.desbloqueado).length;
    return { desbloqueados, total };
  });

  private logrosMock(): LogroFinanciero[] {
    const mockList: LogroFinanciero[] = [
      { id: 'primer-ingreso', titulo: 'Primer paso financiero', descripcion: 'Registra tu primer ingreso.', icono: 'fa-solid fa-circle-dollar-to-slot', iconoColor: 'success', desbloqueado: true, progreso: 1, meta: 1, categoria: 'ingresos' },
      { id: 'ahorro-inicial', titulo: 'Ahorro inicial', descripcion: 'Acumula S/ 2,000 en tus ingresos o balance positivo.', icono: 'fa-solid fa-sack-dollar', iconoColor: 'warning', desbloqueado: true, progreso: 2000, meta: 2000, categoria: 'ahorro' },
      { id: 'constancia-diaria', titulo: 'Constancia diaria', descripcion: 'Mantén una racha de 30 días registrando movimientos.', icono: 'fa-solid fa-fire', iconoColor: 'danger', desbloqueado: true, progreso: 30, meta: 30, categoria: 'movimientos' },
      { id: 'objetivo-cumplido', titulo: 'Objetivo cumplido', descripcion: 'Completa tu primera meta financiera.', icono: 'fa-solid fa-bullseye', iconoColor: 'primary', desbloqueado: true, progreso: 1, meta: 1, categoria: 'metas' },
      { id: 'registrador-experto', titulo: 'Registrador experto', descripcion: 'Registra 100 movimientos financieros.', icono: 'fa-solid fa-list-check', iconoColor: 'info', desbloqueado: false, progreso: 75, meta: 100, categoria: 'movimientos' },
      { id: 'gran-ahorrador', titulo: 'Gran ahorrador', descripcion: 'Ahorra S/ 10,000.', icono: 'fa-solid fa-piggy-bank', iconoColor: 'success', desbloqueado: false, progreso: 6200, meta: 10000, categoria: 'ahorro' },
      { id: 'salud-estable', titulo: 'Salud estable', descripcion: 'Mantén una salud financiera saludable durante 3 meses.', icono: 'fa-solid fa-heart-pulse', iconoColor: 'danger', desbloqueado: false, progreso: 2, meta: 3, categoria: 'salud' },
      { id: 'cumplidor-metas', titulo: 'Cumplidor de metas', descripcion: 'Completa 5 metas financieras.', icono: 'fa-solid fa-trophy', iconoColor: 'warning', desbloqueado: false, progreso: 3, meta: 5, categoria: 'metas' },
      { id: 'control-financiero', titulo: 'Control financiero', descripcion: 'Mantente dentro de tu presupuesto durante 30 días.', icono: 'fa-solid fa-shield-halved', iconoColor: 'primary', desbloqueado: true, progreso: 30, meta: 30, categoria: 'presupuesto' },
      { id: 'ahorrador-constante', titulo: 'Ahorrador constante', descripcion: 'Ahorra durante 3 meses seguidos.', icono: 'fa-solid fa-calendar-check', iconoColor: 'success', desbloqueado: false, progreso: 1, meta: 3, categoria: 'ahorro' },
      { id: 'patrimonio-positivo', titulo: 'Patrimonio positivo', descripcion: 'Alcanza un patrimonio operativo neto de S/ 5,000.', icono: 'fa-solid fa-chart-line', iconoColor: 'info', desbloqueado: false, progreso: 1200, meta: 5000, categoria: 'balance' },
      { id: 'categorizador-experto', titulo: 'Categorizador experto', descripcion: 'Utiliza todas las categorías principales en tus movimientos.', icono: 'fa-solid fa-tags', iconoColor: 'warning', desbloqueado: true, progreso: 5, meta: 5, categoria: 'movimientos' },
      { id: 'cero-excesos', titulo: 'Cero excesos', descripcion: 'No superes tus límites de presupuesto durante 2 meses.', icono: 'fa-solid fa-circle-xmark', iconoColor: 'danger', desbloqueado: true, progreso: 2, meta: 2, categoria: 'presupuesto' },
      { id: 'racha-imparable', titulo: 'Racha imparable', descripcion: 'Mantén actividad financiera durante 100 días.', icono: 'fa-solid fa-bolt', iconoColor: 'primary', desbloqueado: false, progreso: 45, meta: 100, categoria: 'movimientos' },
      { id: 'maestro-ahorro', titulo: 'Maestro del ahorro', descripcion: 'Acumula S/ 25,000 ahorrados.', icono: 'fa-solid fa-crown', iconoColor: 'warning', desbloqueado: false, progreso: 6200, meta: 25000, categoria: 'ahorro' }
    ];

    const desbloqueados = mockList.filter(l => l.desbloqueado).length;

    mockList.push({
      id: 'leyenda-luka',
      titulo: 'Leyenda Luka',
      descripcion: 'Desbloquea todos los logros del sistema.',
      icono: 'fa-solid fa-star',
      iconoColor: 'success',
      desbloqueado: desbloqueados >= 15,
      progreso: desbloqueados,
      meta: 15,
      categoria: 'general'
    });

    return mockList;
  }
}
