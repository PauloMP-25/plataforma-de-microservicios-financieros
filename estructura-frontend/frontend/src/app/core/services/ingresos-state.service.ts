import { Injectable, signal } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Transacciones } from './transacciones';
import { FinancieroService } from './Financiero.service';
import { TransaccionDTO } from '../models/financiero/transaccion.model';
import { ResumenFinancieroDTO } from '../models/financiero/resumen.model';
import { CategoriaDTO } from '../models/financiero/categoria.model';

export interface IngresosFiltrosVista {
  mes?: number;
  anio?: number;
  categoriaId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class IngresosStateService {
  // â”€â”€ Angular Signals for State â”€â”€
  readonly ingresos = signal<TransaccionDTO[]>([]);
  readonly resumenActual = signal<ResumenFinancieroDTO | null>(null);
  readonly resumenAnterior = signal<ResumenFinancieroDTO | null>(null);
  readonly categorias = signal<CategoriaDTO[]>([]);

  // â”€â”€ Loading state â”€â”€
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
   * Loads all required data for the Ingresos section.
   * Leverages caching for static datasets (previous month and categories) and time-based TTL for volatile data.
   */
  cargarDatos(forzar: boolean = false, filtros: IngresosFiltrosVista = {}): void {
    const ahora = Date.now();
    const hayFiltrosVista = filtros.mes != null || filtros.anio != null || !!filtros.categoriaId;
    const necesitaRefrescoVolatil = hayFiltrosVista || forzar || !this.ultimoRefrescoTransacciones || (ahora - this.ultimoRefrescoTransacciones > this.CACHE_TTL_MS);

    const necesitaCategorias = this.categorias().length === 0;
    const necesitaResumenAnterior = this.resumenAnterior() === null;

    if (!necesitaRefrescoVolatil && !necesitaCategorias && !necesitaResumenAnterior) {
      // Everything is already cached
      return;
    }

    this.cargando.set(true);
    this.error.set(null);

    const hoy = new Date();
    const mesActual = filtros.mes ?? hoy.getMonth() + 1;
    const anioActual = filtros.anio ?? hoy.getFullYear();
    const anterior = new Date(anioActual, hoy.getMonth() - 1, 1);
    const mesAnterior = anterior.getMonth() + 1;
    const anioAnterior = anterior.getFullYear();

    // Build calls conditionally
    const llamadas: Record<string, any> = {};

    if (necesitaRefrescoVolatil) {
      llamadas['historial'] = this.transaccionesService.listarHistorial({
        tipo: 'INGRESO',
        categoriaId: filtros.categoriaId,
        mes: mesActual,
        anio: anioActual,
        pagina: 0,
        tamanio: 50
      }).pipe(
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
      llamadas['categorias'] = this.financieroService.getCategorias('INGRESO').pipe(
        catchError(() => of([]))
      );
    } else {
      llamadas['categorias'] = of(null);
    }

    forkJoin(llamadas).subscribe({
      next: (res: any) => {
        if (res.historial !== null) {
          this.ingresos.set(res.historial.content || []);
          if (!hayFiltrosVista) {
            this.ultimoRefrescoTransacciones = Date.now();
          }
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
        this.cargando.set(false);
      },
      error: (err) => {
        console.error('[IngresosStateService] Error loading page data:', err);
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
