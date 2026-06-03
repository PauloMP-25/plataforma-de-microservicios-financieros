import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetricasSignosVitales } from '../../ia-zona-entrenamiento';

@Component({
  selector: 'app-ia-zona-vitales',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 w-full">
      <!-- Frecuencia Cardiaca -->
      <div class="glass-panel p-4 rounded-xl flex flex-col justify-between relative overflow-hidden group border"
           [ngClass]="getCardStatusClass(metricas.frecuenciaCardiaca.estado)">
        <div class="flex justify-between items-start mb-2 relative z-10">
          <span class="text-[10px] uppercase text-slate-400 font-bold tracking-wider">Frec. Cardíaca</span>
          <i class="fa-solid fa-heart-pulse text-xs" [ngClass]="getIconColorClass(metricas.frecuenciaCardiaca.estado)"></i>
        </div>
        <div class="text-3xl font-black text-slate-100 relative z-10">{{ metricas.frecuenciaCardiaca.valor }} <span class="text-xs text-slate-500 font-normal">tx/día</span></div>
        
        <!-- SVG EKG -->
        <div class="absolute bottom-0 left-0 w-full h-12 opacity-50">
          <svg class="w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,15 20,15 25,5 30,25 35,15 100,15" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.frecuenciaCardiaca.estado)"
                      class="ekg-line" [class.fast-pulse]="metricas.frecuenciaCardiaca.estado === 'critico'"></polyline>
          </svg>
        </div>
      </div>

      <!-- Presion Arterial -->
      <div class="glass-panel p-4 rounded-xl flex flex-col justify-between relative overflow-hidden group border"
           [ngClass]="getCardStatusClass(metricas.presionArterial.estado)">
        <div class="flex justify-between items-start mb-2 relative z-10">
          <span class="text-[10px] uppercase text-slate-400 font-bold tracking-wider">Presión Ppto</span>
          <i class="fa-solid fa-gauge-high text-xs" [ngClass]="getIconColorClass(metricas.presionArterial.estado)"></i>
        </div>
        <div class="text-3xl font-black text-slate-100 relative z-10">{{ metricas.presionArterial.valor }}<span class="text-xs text-slate-500 font-normal">%</span></div>
        
        <div class="absolute bottom-0 left-0 w-full h-12 opacity-50">
          <svg class="w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,15 40,15 45,2 50,28 55,15 100,15" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.presionArterial.estado)"
                      class="ekg-line" [class.fast-pulse]="metricas.presionArterial.estado === 'critico'"></polyline>
          </svg>
        </div>
      </div>

      <!-- Temperatura de Ahorro -->
      <div class="glass-panel p-4 rounded-xl flex flex-col justify-between relative overflow-hidden group border"
           [ngClass]="getCardStatusClass(metricas.temperaturaAhorro.estado)">
        <div class="flex justify-between items-start mb-2 relative z-10">
          <span class="text-[10px] uppercase text-slate-400 font-bold tracking-wider">Temp. Ahorro</span>
          <i class="fa-solid fa-temperature-half text-xs" [ngClass]="getIconColorClass(metricas.temperaturaAhorro.estado)"></i>
        </div>
        <div class="text-3xl font-black text-slate-100 relative z-10">{{ metricas.temperaturaAhorro.valor }}<span class="text-xs text-slate-500 font-normal">%</span></div>
        
        <div class="absolute bottom-0 left-0 w-full h-12 opacity-50">
          <svg class="w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 30">
            <polyline points="0,25 20,25 30,10 50,25 100,25" fill="none" stroke-width="2"
                      [ngClass]="getStrokeColorClass(metricas.temperaturaAhorro.estado)"
                      class="ekg-line" [class.fast-pulse]="metricas.temperaturaAhorro.estado === 'critico'"></polyline>
          </svg>
        </div>
      </div>

      <!-- Saturacion Categorias -->
      <div class="glass-panel p-4 rounded-xl flex flex-col justify-between relative overflow-hidden group border"
           [ngClass]="getCardStatusClass(metricas.saturacionCategorias.estado)">
        <div class="flex justify-between items-start mb-2 relative z-10">
          <span class="text-[10px] uppercase text-slate-400 font-bold tracking-wider">Saturación</span>
          <i class="fa-solid fa-droplet text-xs" [ngClass]="getIconColorClass(metricas.saturacionCategorias.estado)"></i>
        </div>
        <div class="text-3xl font-black text-slate-100 relative z-10">{{ metricas.saturacionCategorias.valor }}<span class="text-xs text-slate-500 font-normal">%</span></div>
        
        <div class="absolute bottom-0 left-0 w-full h-12 opacity-50">
          <svg class="w-full h-full" preserveAspectRatio="none" viewBox="0 0 100 30">
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
