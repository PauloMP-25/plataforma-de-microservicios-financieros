import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IngresoKpisComponent } from '../../components/ingreso-kpis/ingreso-kpis';
import { IngresoChartComponent } from '../../components/ingreso-chart/ingreso-chart';
import { IngresoRecentListComponent } from '../../components/ingreso-recent-list/ingreso-recent-list';
import { IngresosTableComponent } from '../../components/ingresos-table/ingresos-table';
import { IngresosStateService } from '../../../../core/services/ingresos-state.service';
import { AppEventBus } from '../../../../core/services/app-event-bus.service';
import {
  DistribucionCategoria,
  IngresoKpi,
  IngresoReciente,
  IngresoRegistro,
  IngresoTendenciaPunto,
} from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingresos-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    IngresoKpisComponent,
    IngresoChartComponent,
    IngresoRecentListComponent,
    IngresosTableComponent
  ],
  templateUrl: './ingresos-page.html',
  styleUrl: './ingresos-page.scss',
})
export class IngresosPage {
  private readonly stateService = inject(IngresosStateService);
  private readonly eventBus = inject(AppEventBus);

  // ── Signals computados para transformar el estado a la interfaz de Ingresos ──
  readonly kpisSignal = computed<IngresoKpi[]>(() => {
    const resumen = this.stateService.resumenActual();
    
    const total = resumen?.totalIngresos ?? 0;
    const cantidad = resumen?.cantidadIngresos ?? 0;
    const cats = this.distribucionSignal();
    const primaryCatName = cats[0]?.categoria ?? 'Ninguna';
    const primaryCatPorc = cats[0]?.porcentaje ? `${cats[0].porcentaje.toFixed(0)}% del total` : '0% del total';

    return [
      { titulo: 'Total ingresos', valor: `S/ ${total.toFixed(2)}`, subtitulo: 'Este mes', color: 'emerald' },
      { titulo: 'Ingresos registrados', valor: String(cantidad), subtitulo: 'Este mes', color: 'violet' },
      { titulo: 'Racha de registros', valor: 'Activa', subtitulo: 'Sigue así', color: 'sky' },
      { titulo: 'Categoría principal', valor: primaryCatName, subtitulo: primaryCatPorc, color: 'amber' },
    ];
  });

  readonly distribucionSignal = computed<DistribucionCategoria[]>(() => {
    const transacciones = this.stateService.ingresos();
    if (!transacciones.length) return [];

    const map = new Map<string, number>();
    let total = 0;
    for (const t of transacciones) {
      const cat = t.categoriaNombre || 'Otros';
      const m = t.monto || 0;
      map.set(cat, (map.get(cat) ?? 0) + m);
      total += m;
    }

    const colores = ['#22c55e', '#7c3aed', '#f59e0b', '#06b6d4', '#ec4899', '#64748b'];
    return Array.from(map.entries())
      .sort((a, b) => b[1] - a[1])
      .map(([categoria, monto], idx) => ({
        categoria,
        monto,
        porcentaje: total > 0 ? (monto / total) * 100 : 0,
        color: colores[idx % colores.length]
      }));
  });

  readonly tendenciaSignal = computed<IngresoTendenciaPunto[]>(() => {
    const transacciones = this.stateService.ingresos();
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

  readonly recientesSignal = computed<IngresoReciente[]>(() => {
    const transacciones = this.stateService.ingresos();
    return transacciones.slice(0, 5).map(t => {
      const fecha = new Date(t.fechaTransaccion);
      return {
        categoria: t.categoriaNombre || 'Otros',
        descripcion: t.descripcion || t.notas || 'Ingreso registrado',
        monto: t.monto || 0,
        fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric' })
      };
    });
  });

  readonly tablaSignal = computed<IngresoRegistro[]>(() => {
    const transacciones = this.stateService.ingresos();
    return transacciones.map(t => {
      const fecha = new Date(t.fechaTransaccion);
      return {
        id: t.id,
        fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }),
        monto: t.monto || 0,
        categoria: t.categoriaNombre || 'Otros',
        metodoPago: t.metodoPago as any || 'DIGITAL',
        etiquetas: t.etiquetas ? t.etiquetas.split(',') : [],
        nota: t.descripcion || t.notas || 'Ingreso registrado'
      };
    });
  });

  // Getters para compatibilidad de enlace directo en plantilla HTML sin alterar bindings básicos
  get kpis(): IngresoKpi[] { return this.kpisSignal(); }
  get distribucion(): DistribucionCategoria[] { return this.distribucionSignal(); }
  get tendencia(): IngresoTendenciaPunto[] { return this.tendenciaSignal(); }
  get recientes(): IngresoReciente[] { return this.recientesSignal(); }
  get tabla(): IngresoRegistro[] { return this.tablaSignal(); }

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
    return this.stateService.resumenActual()?.totalIngresos ?? 0;
  }

  get totalIngresosAnterior(): number {
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

  constructor() {
    this.stateService.cargarDatos();
  }
}
