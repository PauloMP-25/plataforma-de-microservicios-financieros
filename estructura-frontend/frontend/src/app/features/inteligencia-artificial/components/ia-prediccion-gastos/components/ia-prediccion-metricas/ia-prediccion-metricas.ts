import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-prediccion-metricas',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-prediccion-metricas.html',
  styleUrl: './ia-prediccion-metricas.scss'
})
export class IaPrediccionMetricasComponent {
  @Input() promedioHistorico: number = 0;
  @Input() proyeccionProximoMes: number = 0;
}
