import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import { ResumenFinancieroDTO}  from '../models/financiero/resumen.model';
import { CategoriaDTO, TipoMovimiento } from '../models/financiero/categoria.model';
import { AuthService } from './auth.service';
 
@Injectable({ providedIn: 'root' })
export class FinancieroService {
 
  private baseTransacciones = `${environment.gatewayUrl}/api/v1/financiero/transacciones`;
  private baseCategorias    = `${environment.gatewayUrl}/api/v1/financiero/categorias`;
 
  // ── Estado reactivo ──
  resumen    = signal<ResumenFinancieroDTO | null>(null);
  categorias = signal<CategoriaDTO[]>([]);
  cargando   = signal<boolean>(false);
 
  constructor(private http: HttpClient, private auth: AuthService) {}
 
  // ── Resumen financiero del mes actual ──
  getResumen(mes?: number, anio?: number): Observable<ResumenFinancieroDTO> {
    const usuarioId = this.auth.usuario()?.id;
    let params = new HttpParams().set('usuarioId', usuarioId ?? '');
    if (mes)  params = params.set('mes',  mes);
    if (anio) params = params.set('anio', anio);
 
    return this.http.get<ResumenFinancieroDTO>(`${this.baseTransacciones}/resumen`, { params }).pipe(
      catchError(() => {
        // Fallback a mock si falla el backend
        return of({
          desde: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString(),
          hasta: new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString(),
          totalIngresos: 4500,
          totalGastos: 2800,
          balance: 1700,
          cantidadIngresos: 3,
          cantidadGastos: 15,
          totalTransacciones: 18,
          promedioIngreso: 1500,
          promedioGasto: 186.67
        } as ResumenFinancieroDTO);
      })
    );
  }
 
  // ── Categorías ──
  getCategorias(tipo?: TipoMovimiento): Observable<CategoriaDTO[]> {
    let params = new HttpParams();
    if (tipo) params = params.set('tipo', tipo);
    return this.http.get<CategoriaDTO[]>(this.baseCategorias, { params });
  }
 
  cargarResumen(): void {
    this.cargando.set(true);
    this.getResumen().subscribe({
      next:  r  => { this.resumen.set(r); this.cargando.set(false); },
      error: () => this.cargando.set(false)
    });
  }
 
  cargarCategorias(): void {
    this.getCategorias().subscribe({
      next: cats => this.categorias.set(cats)
    });
  }
}
