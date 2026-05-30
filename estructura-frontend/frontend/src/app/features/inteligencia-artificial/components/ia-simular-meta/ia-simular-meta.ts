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

  // Slider de ahorro adicional mensual (S/. 0 a S/. 500)
  ahorroAdicional = signal<number>(0);

  // Datos financieros base extraídos del resultado (raw_payload)
  montoObjetivo = computed(() => Number(this.resultado?.insight?.raw_payload?.monto_objetivo) || 3000);
  aporteMensualBase = computed(() => Number(this.resultado?.insight?.raw_payload?.aporte_mensual_deseado) || 200);
  ahorroPrevio = computed(() => Number(this.resultado?.insight?.raw_payload?.monto_actual_ahorrado) || 500);
  nombreMeta = computed(() => this.resultado?.insight?.raw_payload?.nombre_meta || 'Mi Meta');
  mesesDeseados = computed(() => Number(this.resultado?.insight?.raw_payload?.meses_objetivo) || 12);

  // Recálculo interactivo de meses requeridos
  mesesProyectados = computed(() => {
    const faltante = this.montoObjetivo() - this.ahorroPrevio();
    const aporteTotal = this.aporteMensualBase() + this.ahorroAdicional();
    if (aporteTotal <= 0 || faltante <= 0) return 0;
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

  // Porcentaje del objetivo que se lograría en los meses deseados
  porcentajeLograble = computed(() => {
    const totalAhorrado = this.ahorroPrevio() + ((this.aporteMensualBase() + this.ahorroAdicional()) * this.mesesDeseados());
    return Math.min(100, Math.max(0, (totalAhorrado / this.montoObjetivo()) * 100));
  });

  // Determina la viabilidad comparando meses proyectados vs los meses deseados por el usuario
  esViableSimulado = computed(() => {
    return this.mesesProyectados() <= this.mesesDeseados();
  });

  // Generación del Prompt
  promptGenerado = computed(() => {
    if (!this.resultado) return '';
    return `Actúa como un experto financiero y Coach IA. El usuario desea alcanzar la meta "${this.nombreMeta()}" que cuesta S/. ${this.montoObjetivo()}. Actualmente tiene ahorrados S/. ${this.ahorroPrevio()} y planea aportar S/. ${this.aporteMensualBase()} cada mes. Su objetivo es lograrlo en ${this.mesesDeseados()} meses. Evalúa la viabilidad matemática de este plan. Si no es viable, sugiere cuánto ahorro adicional mensual necesitaría o cuánto debería extender el plazo, y bríndale un plan estratégico motivador.`;
  });

  // Cálculo del nivel de desenfoque (blur) basado en la viabilidad
  blurAmount = computed(() => {
    if (!this.esViableSimulado()) return 16; // Desenfoque alto si no es viable
    const gap = this.mesesDeseados() - this.mesesProyectados();
    if (gap >= 2) return 0;
    return 2;
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
  }

  onSliderChange(event: Event) {
    const target = event.target as HTMLInputElement;
    this.ahorroAdicional.set(Number(target.value));
  }
}
