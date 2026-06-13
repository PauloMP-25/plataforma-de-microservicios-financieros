import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';
import { InsightEspejoTemporalDTO } from '../../../../core/models/ia_coach/ia-espejo.model';
import { IaEspejoCardComponent } from './components/ia-espejo-card/ia-espejo-card';

@Component({
  selector: 'app-ia-espejo-temporal',
  standalone: true,
  imports: [CommonModule, IaEspejoCardComponent],
  templateUrl: './ia-espejo-temporal.html',
  styleUrl: './ia-espejo-temporal.scss'
})
export class IaEspejoTemporalComponent implements OnInit, OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Hito activo seleccionado para visualizar el díptico interactivo (3, 6, 12 meses)
  hitoSeleccionado = signal<3 | 6 | 12>(12);

  @Input() set hitoSeleccionadoInput(value: 3 | 6 | 12) {
    if (value) {
      this.hitoSeleccionado.set(value);
    }
  }

  // Datos mock específicos para Paulo
  private mockInsight: InsightEspejoTemporalDTO = {
    datosPresente: {
      scoreActual: 42,
      saldoActual: 85.00,
      metasActivas: 3
    },
    proyeccionContinuidad: {
      hitos3Meses: {
        scoreProyectado: 38,
        ahorroAcumulado: 30.00,
        metasCumplidas: [],
        metasFracasadas: ['Viaje a Cusco']
      },
      hitos6Meses: {
        scoreProyectado: 35,
        ahorroAcumulado: 50.00,
        metasCumplidas: [],
        metasFracasadas: ['Viaje a Cusco', 'Laptop Gamer']
      },
      hitos12Meses: {
        scoreProyectado: 30,
        ahorroAcumulado: 100.00,
        metasCumplidas: [],
        metasFracasadas: ['Viaje a Cusco', 'Laptop Gamer', 'Ahorro Departamento']
      }
    },
    proyeccionTransformacion: {
      hitos3Meses: {
        scoreProyectado: 65,
        ahorroAcumulado: 450.00,
        metasCumplidas: ['Viaje a Cusco'],
        metasFracasadas: []
      },
      hitos6Meses: {
        scoreProyectado: 78,
        ahorroAcumulado: 950.00,
        metasCumplidas: ['Viaje a Cusco', 'Laptop Gamer'],
        metasFracasadas: []
      },
      hitos12Meses: {
        scoreProyectado: 85,
        ahorroAcumulado: 1550.00,
        metasCumplidas: ['Viaje a Cusco', 'Laptop Gamer', 'Ahorro Departamento'],
        metasFracasadas: []
      }
    },
    narrativasGemini: {
      cartaContinuidad: 'Hola Paulo del futuro. Veo que sigues gastando en mototaxi en lugar de caminar esas pocas cuadras, y los antojos diarios se siguen consumiendo tu presupuesto. Hoy, después de un año, tu ahorro acumulado apenas llega a S/ 100. Tus metas de realizar el Viaje a Cusco, comprar la Laptop Gamer y guardar para tu Ahorro Departamento siguen congeladas. Has continuado priorizando los gastos a corto plazo, y el mañana se ve idéntico al ayer.',
      cartaTransformacion: 'Hola Paulo. Qué gran decisión de ahorro tomaste al reducir esos gastos hormiga de transporte y antojos de fin de semana. Tras 12 meses de constancia, has acumulado S/ 1,550 en ahorros. Gracias a esto, no solo lograste financiar tu Viaje a Cusco y comprar la Laptop Gamer que tanto querías, sino que también diste el primer paso sólido acumulando capital para tu Ahorro Departamento. Este futuro alternativo brilla gracias al poder de tus pequeñas decisiones financieras.'
    }
  };

  // Signal que contiene los datos del espejo actual
  espejoData = signal<InsightEspejoTemporalDTO>(this.mockInsight);

  // Consejo del Coach Luka
  consejoLuka = computed<string>(() => {
    return this.resultado?.consejo || this.espejoData().narrativasGemini.cartaTransformacion;
  });

  ngOnInit() {
    this.cargarDatos();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resultado'] && this.resultado) {
      this.cargarDatos();
    }
  }

  private cargarDatos() {
    if (this.resultado && this.resultado.insight) {
      this.espejoData.set(this.resultado.insight as InsightEspejoTemporalDTO);
    } else {
      this.espejoData.set(this.mockInsight);
    }
  }

  // Cálculo dinámico del impacto neto del ahorro según el hito seleccionado
  diferenciaNeta = computed<number>(() => {
    const data = this.espejoData();
    const hito = this.hitoSeleccionado();
    let cont = 0;
    let trans = 0;

    if (hito === 3) {
      cont = data.proyeccionContinuidad.hitos3Meses.ahorroAcumulado;
      trans = data.proyeccionTransformacion.hitos3Meses.ahorroAcumulado;
    } else if (hito === 6) {
      cont = data.proyeccionContinuidad.hitos6Meses.ahorroAcumulado;
      trans = data.proyeccionTransformacion.hitos6Meses.ahorroAcumulado;
    } else {
      cont = data.proyeccionContinuidad.hitos12Meses.ahorroAcumulado;
      trans = data.proyeccionTransformacion.hitos12Meses.ahorroAcumulado;
    }

    return trans - cont;
  });

  // Helper para obtener el hito activo del espejo seleccionado
  getHitoData(tipo: 'continuidad' | 'transformacion') {
    const data = this.espejoData();
    const hito = this.hitoSeleccionado();
    const proyeccion = tipo === 'continuidad' ? data.proyeccionContinuidad : data.proyeccionTransformacion;

    if (hito === 3) return proyeccion.hitos3Meses;
    if (hito === 6) return proyeccion.hitos6Meses;
    return proyeccion.hitos12Meses;
  }

  // Alternar el hito temporal a visualizar
  setHito(meses: 3 | 6 | 12) {
    this.hitoSeleccionado.set(meses);
  }
}
