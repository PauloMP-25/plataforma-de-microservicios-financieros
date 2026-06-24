import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricasSignosVitales } from '../../ia-zona-entrenamiento';

@Component({
  selector: 'app-ia-zona-vitales',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="vitales-grid">
      <!-- Frecuencia Cardiaca -->
      <div class="vital-card glass-panel" [ngClass]="getCardStatusClass(metricas.frecuenciaCardiaca.estado)">
        <div class="vital-card-header">
          <span class="vital-label">Frec. Cardíaca</span>
          <i class="fa-solid fa-heart-pulse" [ngClass]="getIconColorClass(metricas.frecuenciaCardiaca.estado)"></i>
        </div>
        <div class="vital-value">{{ metricas.frecuenciaCardiaca.valor }} <span class="vital-unit">tx/día</span></div>
        
        <!-- SVG EKG -->
        <div class="vital-ekg-svg">
          <svg preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,15 20,15 25,5 30,25 35,15 100,15" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.frecuenciaCardiaca.estado)"
                      class="ekg-line" [class.fast-pulse]="metricas.frecuenciaCardiaca.estado === 'critico'"></polyline>
          </svg>
        </div>
      </div>

      <!-- Presion Arterial -->
      <div class="vital-card glass-panel" [ngClass]="getCardStatusClass(metricas.presionArterial.estado)">
        <div class="vital-card-header">
          <span class="vital-label">Presión Ppto</span>
          <i class="fa-solid fa-gauge-high" [ngClass]="getIconColorClass(metricas.presionArterial.estado)"></i>
        </div>
        <div class="vital-value">{{ metricas.presionArterial.valor }}<span class="vital-unit">%</span></div>
        
        <div class="vital-ekg-svg">
          <svg preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,15 40,15 45,2 50,28 55,15 100,15" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.presionArterial.estado)"
                      class="ekg-line" [class.fast-pulse]="metricas.presionArterial.estado === 'critico'"></polyline>
          </svg>
        </div>
      </div>

      <!-- Temperatura de Ahorro -->
      <div class="vital-card glass-panel" [ngClass]="getCardStatusClass(metricas.temperaturaAhorro.estado)">
        <div class="vital-card-header">
          <span class="vital-label">Temp. Ahorro</span>
          <i class="fa-solid fa-temperature-half" [ngClass]="getIconColorClass(metricas.temperaturaAhorro.estado)"></i>
        </div>
        <div class="vital-value">{{ metricas.temperaturaAhorro.valor }}<span class="vital-unit">%</span></div>
        
        <div class="vital-ekg-svg">
          <svg preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,25 20,25 30,10 50,25 100,25" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.temperaturaAhorro.estado)"
                      class="ekg-line" [class.fast-pulse]="metricas.temperaturaAhorro.estado === 'critico'"></polyline>
          </svg>
        </div>
      </div>

      <!-- Saturacion Categorias -->
      <div class="vital-card glass-panel" [ngClass]="getCardStatusClass(metricas.saturacionCategorias.estado)">
        <div class="vital-card-header">
          <span class="vital-label">Saturación</span>
          <i class="fa-solid fa-droplet" [ngClass]="getIconColorClass(metricas.saturacionCategorias.estado)"></i>
        </div>
        <div class="vital-value">{{ metricas.saturacionCategorias.valor }}<span class="vital-unit">%</span></div>
        
        <div class="vital-ekg-svg">
          <svg preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,15 10,15 15,0 20,30 25,15 100,15" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.saturacionCategorias.estado)"
                      class="ekg-line fast-pulse"></polyline>
          </svg>
        </div>
      </div>
    </div>
  `
})
export class IaZonaVitalesComponent {
  @Input() metricas!: MetricasSignosVitales;

  getCardStatusClass(estado: string) {
    if (estado === 'critico') return 'border-red-500/50 shadow-[0_0_15px_rgba(239,68,68,0.15)] bg-red-950/20';
    if (estado === 'alerta') return 'border-amber-500/50 shadow-[0_0_15px_rgba(245,158,11,0.15)] bg-amber-950/20';
    return 'border-emerald-500/30 bg-slate-900/50';
  }

  getIconColorClass(estado: string) {
    if (estado === 'critico') return 'text-red-500 animate-pulse';
    if (estado === 'alerta') return 'text-amber-500';
    return 'text-emerald-500';
  }

  getStrokeColorClass(estado: string) {
    if (estado === 'critico') return 'stroke-red-500';
    if (estado === 'alerta') return 'stroke-amber-500';
    return 'stroke-emerald-500';
  }
}
