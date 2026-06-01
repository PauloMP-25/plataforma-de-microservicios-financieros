import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-prediccion-indicadores',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-prediccion-indicadores.html',
  styleUrl: './ia-prediccion-indicadores.scss'
})
export class IaPrediccionIndicadoresComponent {
  @Input() ingresoEsperado: number = 0;
  @Input() proyeccionProximoMes: number = 0;
  @Input() porcentajeVariacion: number = 0;
  @Input() deficitEstimado: number = 0;
  @Input() promedioHistorico: number = 0;
  @Input() pendiente: 'SUBE' | 'BAJA' | 'ESTABLE' = 'ESTABLE';

  getEscudoClase(): string {
    return this.deficitEstimado > 0 ? 'escudo-peligro' : 'escudo-exito';
  }

  getEscudoIcono(): string {
    return this.deficitEstimado > 0 ? 'fa-solid fa-shield-xmark' : 'fa-solid fa-shield-halved';
  }

  getTendenciaIcono(): string {
    const p = this.pendiente;
    if (p === 'SUBE') return 'fa-solid fa-arrow-trend-up';
    if (p === 'BAJA') return 'fa-solid fa-arrow-trend-down';
    return 'fa-solid fa-arrows-left-right';
  }

  getTendenciaClase(): string {
    const p = this.pendiente;
    if (p === 'SUBE') return 'tendencia-sube';
    if (p === 'BAJA') return 'tendencia-baja';
    return 'tendencia-estable';
  }

  getAbsDeficit(): number {
    return Math.abs(this.deficitEstimado);
  }

  get maxThermometerValue(): number {
    const maxVal = Math.max(this.ingresoEsperado, this.proyeccionProximoMes);
    return maxVal > 0 ? maxVal * 1.2 : 1000;
  }
}
