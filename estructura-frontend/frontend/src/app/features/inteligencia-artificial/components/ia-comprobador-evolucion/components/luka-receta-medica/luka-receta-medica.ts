import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoriaHueso } from '../luka-esqueleto-svg/luka-esqueleto-svg';

@Component({
  selector: 'luka-receta-medica',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- Overlay oscurecedor de fondo -->
    <div class="fixed inset-0 bg-slate-950/60 backdrop-blur-sm z-40 transition-opacity duration-300"
         [class.pointer-events-none]="!isOpen"
         [class.opacity-0]="!isOpen"
         [class.opacity-100]="isOpen"
         (click)="close()">
    </div>

    <!-- Panel lateral (Receta Médica) -->
    <div class="fixed top-0 right-0 h-screen w-full sm:w-[460px] z-50 bg-[#fdfbf7] text-slate-800 shadow-2xl transition-transform duration-500 ease-out transform flex flex-col justify-between border-l border-stone-300"
         [class.translate-x-full]="!isOpen"
         [class.translate-x-0]="isOpen"
         style="font-family: 'Georgia', 'Times New Roman', serif;">
      
      <!-- Cabecera de la receta -->
      <div class="p-6 border-b border-stone-300 flex justify-between items-start">
        <div class="text-stone-900">
          <h4 class="text-xl font-bold uppercase tracking-widest text-stone-800" style="font-family: 'Playfair Display', serif;">RECETA DE OPTIMIZACIÓN</h4>
          <p class="text-xs text-stone-500 mt-1 uppercase tracking-wider font-mono">Luka Mediacal Labs & Gemini Pro</p>
        </div>
        <button (click)="close()" class="w-8 h-8 rounded-full border border-stone-300 flex items-center justify-center text-stone-500 hover:text-stone-950 hover:bg-stone-100 transition-colors">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>

      <!-- Contenido de la Receta (Scrollable) -->
      <div class="p-8 flex-grow overflow-y-auto space-y-6 text-sm text-stone-700 leading-relaxed">
        
        <!-- Paciente & Categoría -->
        <div class="grid grid-cols-2 gap-4 border-b border-stone-300 pb-4 text-xs font-mono uppercase text-stone-500">
          <div>
            <span class="block text-[10px] text-stone-400">Paciente:</span>
            <span class="font-bold text-stone-800">Paulo Moron</span>
          </div>
          <div>
            <span class="block text-[10px] text-stone-400">Diagnóstico en:</span>
            <span class="font-bold text-stone-900 text-sm italic">{{ categoria?.nombre }}</span>
          </div>
        </div>

        <!-- Indicadores Clínicos -->
        <div class="space-y-2 border-b border-stone-300 pb-4">
          <h5 class="text-xs font-bold text-stone-800 uppercase tracking-widest font-mono">Indicadores Clínicos</h5>
          <div class="grid grid-cols-3 gap-2 text-center text-xs font-mono pt-1">
            <div class="bg-stone-100 p-2 rounded border border-stone-200">
              <span class="block text-[10px] text-stone-400">Gasto A (Base)</span>
              <span class="font-bold text-stone-800">S/ {{ categoria?.gastoPeriodoA?.toFixed(2) }}</span>
            </div>
            <div class="bg-stone-100 p-2 rounded border border-stone-200">
              <span class="block text-[10px] text-stone-400">Gasto B (Evol)</span>
              <span class="font-bold text-stone-900">S/ {{ categoria?.gastoPeriodoB?.toFixed(2) }}</span>
            </div>
            <div class="bg-red-50 p-2 rounded border border-red-200">
              <span class="block text-[10px] text-red-600 font-bold">Desviación</span>
              <span class="font-bold text-red-700">+{{ categoria?.desviacion?.toFixed(1) }}%</span>
            </div>
          </div>
        </div>

        <!-- Posología de Gemini (Las 3 acciones) -->
        <div class="space-y-4">
          <h5 class="text-xs font-bold text-stone-800 uppercase tracking-widest font-mono">Tratamiento & Posología</h5>
          
          <div class="space-y-3">
            <div class="flex gap-3 items-start">
              <span class="font-mono font-bold text-lg text-stone-400">01.</span>
              <p class="text-stone-700 pt-0.5">
                <span class="font-bold text-stone-900">Reducción Directa de Micro-gastos:</span> Aplicar un tope drástico de consumo diario a la categoría <span class="italic font-bold">"{{ categoria?.nombre }}"</span>. Evitar las compras impulsivas tras las 6:00 PM los fines de semana.
              </p>
            </div>

            <div class="flex gap-3 items-start">
              <span class="font-mono font-bold text-lg text-stone-400">02.</span>
              <p class="text-stone-700 pt-0.5">
                <span class="font-bold text-stone-900">Pre-aprobación del Gasto:</span> Antes de realizar cualquier desembolso, transferir el 20% del valor planeado al monedero de ahorro seguro de Luka.
              </p>
            </div>

            <div class="flex gap-3 items-start">
              <span class="font-mono font-bold text-lg text-stone-400">03.</span>
              <p class="text-stone-700 pt-0.5">
                <span class="font-bold text-stone-900">Sustitución de Hábitos Nocivos:</span> Buscar alternativas de ocio gratuitas que no comprometan tus cuotas semanales de Pichanga o tu Laptop Gamer.
              </p>
            </div>
          </div>
        </div>

        <!-- Pronóstico Financiero -->
        <div class="bg-stone-100/50 p-4 rounded-xl border border-stone-200 text-xs italic space-y-1">
          <span class="font-bold text-stone-800 font-mono block not-italic uppercase tracking-widest text-[10px] mb-1">Pronóstico a 3 Meses</span>
          Si completas con éxito esta prescripción médica, se estima un retorno económico neto de <span class="font-bold text-emerald-700 not-italic">S/ 360.00</span> en ahorros directos para tus objetivos activos.
        </div>

      </div>

      <!-- Firma del Especialista -->
      <div class="p-6 border-t border-stone-300 bg-stone-50 flex items-center justify-between">
        <div>
          <p class="text-[10px] text-stone-400 font-mono">AUTORIZADO POR:</p>
          <p class="text-sm font-bold text-stone-800 mt-0.5">Luka AI Medical Staff</p>
        </div>
        <!-- Firma estilizada -->
        <div class="text-stone-500 font-serif italic text-2xl tracking-tighter opacity-85 select-none pr-4">
          Gemini Pro MD
        </div>
      </div>

    </div>
  `
})
export class LukaRecetaMedicaComponent {
  @Input() isOpen = false;
  @Input() categoria: CategoriaHueso | null = null;
  @Output() onClose = new EventEmitter<void>();

  close() {
    this.onClose.emit();
  }
}
