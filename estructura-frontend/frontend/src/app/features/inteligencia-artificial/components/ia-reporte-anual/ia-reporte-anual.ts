import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';
import { ConsejoReporteCompletoDTO } from '../../../../core/models/ia_coach/ia-reporte-completo.model';

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

  escenarioActivo = signal<'EXCELENTE' | 'CRITICO'>('EXCELENTE');

  scoreVisual = signal(0);
  balanceVisual = signal(0);
  ingresosVisual = signal(0);
  gastosVisual = signal(0);

  consejoReporte = signal<ConsejoReporteCompletoDTO | null>(null);

  fuegosArtificialesActivos = computed(() => this.scoreVisual() > 80);

  anguloAguja = computed(() => {
    const score = this.scoreVisual();
    return (score / 100) * 180 - 90;
  });

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
      if (this.resultado.consejo && typeof this.resultado.consejo === 'object') {
        this.consejoReporte.set(this.resultado.consejo as ConsejoReporteCompletoDTO);
      }
      this.cargarDatosEscenario();
    }
  }

  setEscenario(escenario: 'EXCELENTE' | 'CRITICO') {
    this.escenarioActivo.set(escenario);
    this.cargarDatosEscenario();
  }

  private cargarDatosEscenario() {
    const esc = this.escenarioActivo();
    
    let targetScore = 88;
    let targetBalance = 2450;
    let targetIngresos = 15000;
    let targetGastos = 12550;

    if (esc === 'CRITICO') {
      targetScore = 42;
      targetBalance = -180;
      targetIngresos = 8000;
      targetGastos = 8180;
      
      if (!this.resultado?.consejo || typeof this.resultado.consejo === 'string') {
        this.consejoReporte.set({
          pensamiento_interno_ia: 'El score es de 42, con balance negativo. Se requieren recortes drásticos.',
          analisis_score: 'Tu salud financiera está en un nivel crítico. Los egresos han superado a tus ingresos en el año.',
          impacto_meta: 'Esta situación te aleja de tus objetivos principales de ahorro. Las suscripciones están agotando el presupuesto libre.',
          veredicto_final: 'Deficiente',
          mensaje_motivacional: '¡Es el momento de tomar el control! Reestructura tus gastos fijos y podrás salir del déficit.'
        });
      }
    } else {
      if (!this.resultado?.consejo || typeof this.resultado.consejo === 'string') {
        this.consejoReporte.set({
          pensamiento_interno_ia: 'El usuario tiene un balance superavitario, excelente gestión.',
          analisis_score: 'Tu balance neto positivo de S/ 2,450.00 indica una gestión responsable. Has mantenido un crecimiento constante.',
          impacto_meta: 'Identificamos que tu punto crítico es Ocio y Delivery. Controlar esto acelerará tu meta de la Laptop Gamer.',
          veredicto_final: 'Excelente',
          mensaje_motivacional: '¡Mantén este gran ritmo! Pequeños ajustes te llevarán a la maestría.'
        });
      }
    }

    this.animarContador(this.scoreVisual, targetScore, 1000);
    this.animarContador(this.balanceVisual, targetBalance, 1200);
    this.animarContador(this.ingresosVisual, targetIngresos, 1400);
    this.animarContador(this.gastosVisual, targetGastos, 1400);
  }

  private animarContador(signalRef: any, target: number, duration: number) {
    const start = signalRef();
    const startTime = performance.now();

    const update = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      
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
