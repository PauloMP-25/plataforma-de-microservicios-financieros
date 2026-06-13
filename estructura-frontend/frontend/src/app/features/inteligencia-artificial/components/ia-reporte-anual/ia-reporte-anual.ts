import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';

@Component({
  selector: 'app-ia-reporte-anual',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-reporte-anual.html',
  styleUrl: './ia-reporte-anual.scss'
})
export class IaReporteAnualComponent implements OnInit, OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Escenario de depuración para alternar en QA
  escenarioActivo = signal<'EXCELENTE' | 'CRITICO'>('EXCELENTE');

  // Contadores visuales animados para simular counter-up
  scoreVisual = signal(0);
  balanceVisual = signal(0);
  ingresosVisual = signal(0);
  gastosVisual = signal(0);

  // Activador de fuegos artificiales en canvas CSS
  fuegosArtificialesActivos = computed(() => this.scoreVisual() > 80);

  // Rotación física de la aguja del Dial (-90deg a +90deg)
  anguloAguja = computed(() => {
    const score = this.scoreVisual();
    return (score / 100) * 180 - 90;
  });

  // Altura del termómetro de gastos en porcentaje (gastos / ingresos)
  porcentajeTermometro = computed(() => {
    const ing = this.ingresosVisual();
    const gas = this.gastosVisual();
    if (ing <= 0) return 0;
    return Math.min(100, Math.ceil((gas / ing) * 100));
  });

  ngOnInit() {
    this.cargarDatosEscenario();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resultado'] && this.resultado) {
      this.cargarDatosEscenario();
    }
  }

  setEscenario(escenario: 'EXCELENTE' | 'CRITICO') {
    this.escenarioActivo.set(escenario);
    this.cargarDatosEscenario();
  }

  private cargarDatosEscenario() {
    const esc = this.escenarioActivo();
    
    // Configurar valores objetivo según el escenario seleccionado
    let targetScore = 88;
    let targetBalance = 2450;
    let targetIngresos = 15000;
    let targetGastos = 12550;

    if (esc === 'CRITICO') {
      targetScore = 42;
      targetBalance = -180;
      targetIngresos = 8000;
      targetGastos = 8180;
    }

    // Ejecutar animación de counter-up
    this.animarContador(this.scoreVisual, targetScore, 1000);
    this.animarContador(this.balanceVisual, targetBalance, 1200);
    this.animarContador(this.ingresosVisual, targetIngresos, 1400);
    this.animarContador(this.gastosVisual, targetGastos, 1400);
  }

  // Utilidad ligera para animar incrementos de números
  private animarContador(signalRef: any, target: number, duration: number) {
    const start = signalRef();
    const startTime = performance.now();

    const update = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
      // Easing cuadrático de salida para suavidad
      const ease = progress * (2 - progress);
      const currentVal = Math.round(start + (target - start) * ease);
      
      signalRef.set(currentVal);

      if (progress < 1) {
        requestAnimationFrame(update);
      }
    };

    requestAnimationFrame(update);
  }
}
