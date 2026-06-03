import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'luka-escaner-carga',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="w-full min-h-[500px] flex flex-col items-center justify-center relative overflow-hidden">
      <!-- Laser de Escaneo -->
      <div class="absolute w-full h-[4px] bg-cyan-400 shadow-[0_0_20px_rgba(6,182,212,0.9),_0_0_10px_rgba(6,182,212,0.6)] animate-laser-scan left-0"></div>

      <div class="flex flex-col items-center text-center relative z-10">
        <!-- Icono de X-Ray / Radiología Animado -->
        <div class="w-24 h-24 rounded-full border border-cyan-500/30 flex items-center justify-center mb-6 relative animate-[pulse_1.5s_infinite]">
          <div class="absolute inset-2 rounded-full border border-dashed border-cyan-400/50 animate-[spin_12s_linear_infinite]"></div>
          <i class="fa-solid fa-radiation text-4xl text-cyan-400"></i>
        </div>

        <h3 class="text-xl font-bold font-mono tracking-widest text-cyan-400 uppercase mb-2">
          {{ mensajeActual }}
        </h3>
        <p class="text-xs font-mono text-slate-500 uppercase tracking-widest">
          SALA DE DIAGNÓSTICO RADIOGRÁFICO LUKA
        </p>

        <!-- Puntos de carga -->
        <div class="flex gap-2 mt-6">
          <span class="w-2 h-2 rounded-full bg-cyan-500 animate-[bounce_1s_infinite_100ms]"></span>
          <span class="w-2 h-2 rounded-full bg-cyan-500 animate-[bounce_1s_infinite_200ms]"></span>
          <span class="w-2 h-2 rounded-full bg-cyan-500 animate-[bounce_1s_infinite_300ms]"></span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .animate-laser-scan {
      animation: laser-scan-anim 2.5s ease-in-out infinite;
    }

    @keyframes laser-scan-anim {
      0% { top: 10%; }
      50% { top: 90%; }
      100% { top: 10%; }
    }
  `]
})
export class LukaEscanerCargaComponent implements OnInit, OnDestroy {
  mensajes = [
    'Estableciendo conexión neuronal...',
    'Extrayendo transacciones de los períodos A y B...',
    'Calculando volatilidad mensual con Pandas...',
    'Buscando anomalías y desvíos presupuestarios...',
    'Ejecutando diagnóstico evolutivo con Gemini Pro...',
    'Renderizando mapa de calor óseo...'
  ];
  
  mensajeActual = this.mensajes[0];
  private intervalId: any;

  ngOnInit() {
    let index = 0;
    this.intervalId = setInterval(() => {
      index = (index + 1) % this.mensajes.length;
      this.mensajeActual = this.mensajes[index];
    }, 600);
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }
}
