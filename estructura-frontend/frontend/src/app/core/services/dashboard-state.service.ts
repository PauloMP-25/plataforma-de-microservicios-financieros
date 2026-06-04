import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../enviroments/environment';
import { ResultadoApi } from '../models/auth/user.model';
import { 
  DashboardResumenDTO, 
  CashflowPointDTO, 
  CategoriaDistribucionDTO 
} from '../models/dashboard/dashboard.model';
import { TransaccionDTO } from '../models/financiero/transaccion.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardStateService {
  private base = `${environment.gatewayUrl}/api/v1/dashboard`;

  // ── Angular Signals de Estado ──
  readonly perfil = signal<any>(null);
  readonly resumen = signal<DashboardResumenDTO | null>(null);
  readonly recientes = signal<TransaccionDTO[]>([]);
  readonly flujoCaja = signal<CashflowPointDTO[]>([]);
  readonly distribucionGastos = signal<CategoriaDistribucionDTO[]>([]);

  // ── Estados Auxiliares ──
  readonly loadingResumen = signal<boolean>(false);
  readonly loadingGraficos = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  // Timestamp de la última actualización exitosa de los KPIs
  private ultimoRefrescoKPIs = 0;
  // Duración de caché local en ms (15 minutos)
  private readonly CACHE_DURATION_MS = 15 * 60 * 1000;

  constructor(private http: HttpClient) {}

  /**
   * Carga el perfil del usuario, los KPIs (resumen) y las transacciones recientes.
   * Utiliza caché en memoria de 15 minutos a menos que se fuerce el refresco.
   */
  cargarResumen(forzar: boolean = false): void {
    const ahora = Date.now();
    if (!forzar && this.resumen() && (ahora - this.ultimoRefrescoKPIs < this.CACHE_DURATION_MS)) {
      // Retener estado local (caché cliente activa)
      return;
    }

    this.loadingResumen.set(true);
    this.error.set(null);

    const url = forzar ? `${this.base}/resumen?refresh=true` : `${this.base}/resumen`;
    this.http.get<ResultadoApi<any>>(url).subscribe({
      next: (resp) => {
        if (resp.exito && resp.datos) {
          this.perfil.set(resp.datos.perfil);
          this.resumen.set(resp.datos.resumen);
          this.recientes.set(resp.datos.recientes || []);
          this.ultimoRefrescoKPIs = Date.now();
        } else {
          this.error.set(resp.mensaje || 'Error al cargar resumen');
        }
        this.loadingResumen.set(false);
      },
      error: (err) => {
        // Fallback a mock si falla el BFF
        console.warn('[DashboardStateService] Fallback a mock de resumen:', err);
        const hoy = new Date();
        const factorMes = (hoy.getMonth() + 1) * 230;
        const totalIngresos = 3800 + (factorMes % 1500);
        const totalGastos = 2200 + (factorMes % 900);
        const balance = totalIngresos - totalGastos;
        
        this.resumen.set({
          desde: new Date(hoy.getFullYear(), hoy.getMonth(), 1).toISOString(),
          hasta: new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0).toISOString(),
          totalIngresos,
          totalGastos,
          balance,
          cantidadIngresos: 4,
          cantidadGastos: 15,
          tasaAhorro: totalIngresos > 0 ? (balance / totalIngresos) * 100 : 0
        });
        this.recientes.set([]);
        this.error.set(null);
        this.loadingResumen.set(false);
      }
    });
  }

  /**
   * Carga los datos analíticos para renderizar los gráficos SVG.
   */
  cargarGraficos(forzar: boolean = false): void {
    this.loadingGraficos.set(true);

    const url = forzar ? `${this.base}/graficos?refresh=true` : `${this.base}/graficos`;
    this.http.get<ResultadoApi<any>>(url).subscribe({
      next: (resp) => {
        if (resp.exito && resp.datos) {
          this.flujoCaja.set(resp.datos.flujoCaja || []);
          this.distribucionGastos.set(resp.datos.distribucionGastos || []);
        }
        this.loadingGraficos.set(false);
      },
      error: (err) => {
        console.error('[DashboardStateService] Error cargando gráficos:', err);
        this.loadingGraficos.set(false);
      }
    });
  }

  /**
   * Invalida los datos y la fecha del último refresco local, forzando una recarga limpia.
   */
  invalidarCache(): void {
    this.ultimoRefrescoKPIs = 0;
    this.cargarResumen(true);
    this.cargarGraficos(true);
  }
}
