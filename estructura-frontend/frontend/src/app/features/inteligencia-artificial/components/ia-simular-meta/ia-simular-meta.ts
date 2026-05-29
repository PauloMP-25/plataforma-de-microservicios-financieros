import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

@Component({
  selector: 'app-ia-simular-meta',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-simular-meta.html',
  styleUrl: './ia-simular-meta.scss'
})
export class IaSimularMetaComponent implements OnInit, OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Escenario ficticio para evaluar estados viables/no viables en QA
  estadoEscenario = signal<'VIABLE' | 'NO_VIABLE'>('VIABLE');

  // Slider de ahorro adicional mensual (S/. 0 a S/. 500)
  ahorroAdicional = signal<number>(0);

  // Datos financieros base extraídos del resultado o mock
  montoObjetivo = computed(() => Number(this.resultado?.grafico?.datos?.[0]?.valor) || 3500);
  aporteMensualBase = computed(() => Number(this.resultado?.grafico?.datos?.[1]?.valor) || 350);
  ahorroPrevio = signal<number>(500);

  // Recálculo interactivo de meses requeridos
  mesesProyectados = computed(() => {
    const faltante = this.montoObjetivo() - this.ahorroPrevio();
    const aporteTotal = this.aporteMensualBase() + this.ahorroAdicional();
    if (aporteTotal <= 0) return 99;
    return Math.max(1, Math.ceil(faltante / aporteTotal));
  });

  // Determinar la fecha objetivo simulada sumando meses a la fecha actual
  fechaProyectadaStr = computed(() => {
    const meses = this.mesesProyectados();
    const hoy = new Date();
    hoy.setMonth(hoy.getMonth() + meses);
    const nombresMeses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    return `${nombresMeses[hoy.getMonth()]} ${hoy.getFullYear()}`;
  });

  // Determina la viabilidad en tiempo real (si es menor o igual a 6 meses se considera altamente viable)
  esViableSimulado = computed(() => {
    if (this.estadoEscenario() === 'NO_VIABLE') return false;
    return this.mesesProyectados() <= 7;
  });

  // Cálculo del nivel de desenfoque (blur) basado en los meses restantes
  // A menor cantidad de meses restantes, más nítida es la imagen (0px de blur)
  blurAmount = computed(() => {
    const meses = this.mesesProyectados();
    if (!this.esViableSimulado()) return 16; // Desenfoque alto permanente
    if (meses <= 4) return 0;
    if (meses <= 6) return 2;
    return 6;
  });

  ngOnInit() {
    this.resetSimulador();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resultado'] && this.resultado) {
      this.resetSimulador();
    }
  }

  resetSimulador() {
    this.ahorroAdicional.set(0);
    const isViable = this.resultado?.insight?.viabilidad_fecha_objetivo !== false;
    this.estadoEscenario.set(isViable ? 'VIABLE' : 'NO_VIABLE');
  }

  setEscenario(escenario: 'VIABLE' | 'NO_VIABLE') {
    this.estadoEscenario.set(escenario);
    if (escenario === 'NO_VIABLE') {
      this.ahorroAdicional.set(0); // Forzar sin aporte extra para ver el estado de alerta
    } else {
      this.ahorroAdicional.set(150); // Forzar aporte extra sugerido
    }
  }

  onSliderChange(event: Event) {
    const target = event.target as HTMLInputElement;
    this.ahorroAdicional.set(Number(target.value));
  }
}
