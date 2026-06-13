import {
  Component,
  Input,
  OnChanges,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';

import { IaPrediccionIndicadoresComponent } from './components/ia-prediccion-indicadores/ia-prediccion-indicadores';
import { IaPrediccionOraculoComponent } from './components/ia-prediccion-oraculo/ia-prediccion-oraculo';
import { IaPrediccionConsejoComponent } from './components/ia-prediccion-consejo/ia-prediccion-consejo';
import { IaPrediccionMetricasComponent } from './components/ia-prediccion-metricas/ia-prediccion-metricas';

@Component({
  selector: 'app-ia-prediccion-gastos',
  standalone: true,
  imports: [
    CommonModule,
    IaPrediccionIndicadoresComponent,
    IaPrediccionOraculoComponent,
    IaPrediccionConsejoComponent,
    IaPrediccionMetricasComponent
  ],
  templateUrl: './ia-prediccion-gastos.html',
  styleUrl: './ia-prediccion-gastos.scss',
})
export class IaPrediccionGastosComponent implements OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  promedioHistorico = signal<number>(0);
  proyeccionProximoMes = signal<number>(0);
  pendiente = signal<'SUBE' | 'BAJA' | 'ESTABLE'>('ESTABLE');
  porcentajeVariacion = signal<number>(0);
  deficitEstimado = signal<number>(0);
  historialMeses = signal<number[]>([]);
  ingresoEsperado = signal<number>(2000.00);
  consejo = signal<any>(null);
  oraculoHablando = signal<boolean>(false);

  setHablando(estado: boolean) {
    this.oraculoHablando.set(estado);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resultado'] && this.resultado) {
      this.procesarResultado();
    }
  }

  private procesarResultado(): void {
    const insight = this.resultado?.insight;
    if (!insight) return;

    this.promedioHistorico.set(insight.promedio_historico ?? 0);
    this.proyeccionProximoMes.set(insight.proyeccion_proximo_mes ?? 0);
    this.pendiente.set(insight.pendiente ?? 'ESTABLE');
    this.porcentajeVariacion.set(insight.porcentaje_variacion_mensual ?? 0);
    this.deficitEstimado.set(insight.deficit_estimado ?? 0);
    this.historialMeses.set(insight.historial_meses ?? []);
    this.ingresoEsperado.set(insight.ingreso_esperado ?? 2000.00);
    this.consejo.set(this.resultado?.consejo ?? null);
  }
}
