import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

import { IaHabitosKpiComponent } from './components/ia-habitos-kpi/ia-habitos-kpi';
import { IaHabitosRadarComponent } from './components/ia-habitos-radar/ia-habitos-radar';
import { IaHabitosHeatmapComponent } from './components/ia-habitos-heatmap/ia-habitos-heatmap';
import { IaHabitosCoachComponent } from './components/ia-habitos-coach/ia-habitos-coach';

@Component({
  selector: 'app-ia-habitos-financieros',
  standalone: true,
  imports: [
    CommonModule,
    IaHabitosKpiComponent,
    IaHabitosRadarComponent,
    IaHabitosHeatmapComponent,
    IaHabitosCoachComponent
  ],
  templateUrl: './ia-habitos-financieros.html',
  styleUrl: './ia-habitos-financieros.scss',
})
export class IaHabitosFinancierosComponent {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Propiedades calculadas a partir del insight
  puntuacionHabito = computed(() => this.resultado?.insight?.puntuacion_habito ?? 72);
  esSaludable = computed(() => this.resultado?.insight?.es_saludable ?? false);
  diaMayorGasto = computed(() => this.resultado?.insight?.dia_mayor_gasto ?? 'Sábado');
  categoriaMasFrecuente = computed(() => this.resultado?.insight?.categoria_mas_frecuente ?? 'Restaurantes');
  totalTransacciones = computed(() => this.resultado?.insight?.total_transacciones_periodo ?? 18);
  
  dimensiones = computed(() => this.resultado?.insight?.dimensiones || {});
  heatmapDatos = computed(() => this.resultado?.insight?.heatmap_datos || []);
  consejo = computed(() => this.resultado?.consejo || '');
}
