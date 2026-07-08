import { Injectable, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Transacciones } from './transacciones';
import { FinancieroService } from './Financiero.service';
import { TransaccionDTO } from '../models/financiero/transaccion.model';
import { ResumenFinancieroDTO, RachaDTO } from '../models/financiero/resumen.model';
import { CategoriaDTO } from '../models/financiero/categoria.model';

@Injectable({
  providedIn: 'root'
})
export class GastosStateService {
  // ── Angular Signals for State ──
  readonly gastos = signal<TransaccionDTO[]>([]);
  readonly resumenActual = signal<ResumenFinancieroDTO | null>(null);
  readonly resumenAnterior = signal<ResumenFinancieroDTO | null>(null);
  readonly categorias = signal<CategoriaDTO[]>([]);
  readonly rachaActual = signal<RachaDTO | null>(null);

  // ── Loading state ──
  readonly cargando = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Cache timestamps (ms)
  private ultimoRefrescoTransacciones = 0;
  private readonly CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes for transaction/current month summary

  constructor(
    private transaccionesService: Transacciones,
    private financieroService: FinancieroService
  ) {}

  /**
   * Loads all required data for the Gastos section.
   * Leverages caching for static datasets (previous month and categories) and time-based TTL for volatile data.
   */
  cargarDatos(forzar: boolean = false): void {
    const ahora = Date.now();
    const necesitaRefrescoVolatil = forzar || !this.ultimoRefrescoTransacciones || (ahora - this.ultimoRefrescoTransacciones > this.CACHE_TTL_MS);

    const necesitaCategorias = this.categorias().length === 0;
    const necesitaResumenAnterior = this.resumenAnterior() === null;

    if (!necesitaRefrescoVolatil && !necesitaCategorias && !necesitaResumenAnterior) {
      // Everything is already cached
      return;
    }

    this.cargando.set(true);
    this.error.set(null);

    const hoy = new Date();
    const mesActual = hoy.getMonth() + 1;
    const anioActual = hoy.getFullYear();
    const anterior = new Date(anioActual, hoy.getMonth() - 1, 1);
    const mesAnterior = anterior.getMonth() + 1;
    const anioAnterior = anterior.getFullYear();

    // Build calls conditionally
    const llamadas: Record<string, any> = {};

    if (necesitaRefrescoVolatil) {
      llamadas['historial'] = this.transaccionesService.listarHistorial({ tipo: 'GASTO', pagina: 0, tamanio: 50 }).pipe(
        catchError(() => of({ content: [] }))
      );
      llamadas['resumenActual'] = this.financieroService.getResumen(mesActual, anioActual).pipe(
        catchError(() => of(null))
      );
    } else {
      llamadas['historial'] = of(null);
      llamadas['resumenActual'] = of(null);
    }

    if (necesitaResumenAnterior) {
      llamadas['resumenAnterior'] = this.financieroService.getResumen(mesAnterior, anioAnterior).pipe(
        catchError(() => of(null))
      );
    } else {
      llamadas['resumenAnterior'] = of(null);
    }

    if (necesitaCategorias) {
      llamadas['categorias'] = this.financieroService.getCategorias('GASTO').pipe(
        catchError(() => of([]))
      );
    } else {
      llamadas['categorias'] = of(null);
    }

    if (necesitaRefrescoVolatil) {
      llamadas['racha'] = this.financieroService.getRacha().pipe(
        catchError(() => of(null))
      );
    } else {
      llamadas['racha'] = of(null);
    }

    forkJoin(llamadas).subscribe({
      next: (res: any) => {
        if (res.historial !== null) {
          this.gastos.set(res.historial.content || []);
          this.ultimoRefrescoTransacciones = Date.now();
        }
        if (res.resumenActual !== null) {
          this.resumenActual.set(res.resumenActual);
        }
        if (res.resumenAnterior !== null) {
          this.resumenAnterior.set(res.resumenAnterior);
        }
        if (res.categorias !== null) {
          this.categorias.set(res.categorias || []);
        }
        if (res.racha !== null) {
          this.rachaActual.set(res.racha);
        }
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('[GastosStateService] Error loading page data:', err);
        this.error.set('Error al sincronizar datos financieros.');
        this.cargando.set(false);
      }
    });
  }

  /**
   * Invalidates caches and forces a clean refetch.
   */
  invalidarCache(): void {
    this.ultimoRefrescoTransacciones = 0;
    this.cargarDatos(true);
  }
}
