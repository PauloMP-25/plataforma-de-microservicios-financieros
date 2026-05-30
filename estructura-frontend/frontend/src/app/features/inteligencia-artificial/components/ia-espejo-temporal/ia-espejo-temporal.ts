import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO, InsightEspejoTemporalDTO } from '../../../../core/models/financiero/ia.model';
import { IaEspejoCardComponent } from './components/ia-espejo-card/ia-espejo-card';
import { IaEspejoImpactComponent } from './components/ia-espejo-impact/ia-espejo-impact';

@Component({
  selector: 'app-ia-espejo-temporal',
  standalone: true,
  imports: [CommonModule, IaEspejoCardComponent, IaEspejoImpactComponent],
  templateUrl: './ia-espejo-temporal.html',
  styleUrl: './ia-espejo-temporal.scss'
})
export class IaEspejoTemporalComponent implements OnInit, OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Hito activo seleccionado para visualizar el díptico interactivo (3, 6, 12 meses)
  hitoSeleccionado = signal<3 | 6 | 12>(12);

  // Datos mock específicos para Paulo
  private mockInsight: InsightEspejoTemporalDTO = {
    datosPresente: {
      scoreActual: 42,
      saldoActual: 85.00,
      metasActivas: 1
    },
    proyeccionContinuidad: {
      hitos3Meses: {
        scoreProyectado: 38,
        ahorroAcumulado: 30.00,
        metasCumplidas: [],
        metasFracasadas: ['Pichanga Semanal']
      },
      hitos6Meses: {
        scoreProyectado: 35,
        ahorroAcumulado: 50.00,
        metasCumplidas: [],
        metasFracasadas: ['Pichanga Semanal', 'Zapatillas de Futsal']
      },
      hitos12Meses: {
        scoreProyectado: 30,
        ahorroAcumulado: 100.00,
        metasCumplidas: [],
        metasFracasadas: ['Pichanga Semanal', 'Zapatillas de Futsal', 'Curso de Desarrollo Web']
      }
    },
    proyeccionTransformacion: {
      hitos3Meses: {
        scoreProyectado: 65,
        ahorroAcumulado: 450.00,
        metasCumplidas: ['Pichanga Semanal'],
        metasFracasadas: []
      },
      hitos6Meses: {
        scoreProyectado: 78,
        ahorroAcumulado: 950.00,
        metasCumplidas: ['Pichanga Semanal', 'Zapatillas de Futsal'],
        metasFracasadas: []
      },
      hitos12Meses: {
        scoreProyectado: 85,
        ahorroAcumulado: 1550.00,
        metasCumplidas: ['Pichanga Semanal', 'Zapatillas de Futsal', 'Curso de Desarrollo Web'],
        metasFracasadas: []
      }
    },
    narrativasGemini: {
      cartaContinuidad: 'Hola Paulo del futuro. Veo que sigues gastando en mototaxi en lugar de caminar esas pocas cuadras, y los "antojos" diarios se siguen consumiendo tu presupuesto. Hoy, después de un año, tu ahorro acumulado apenas llega a S/ 100. Tuviste que perderte la pichanga de los fines de semana varias veces porque no te alcanzaban los S/ 6 de la cuota, y tu meta de comprar las zapatillas de futsal y el Curso de Desarrollo Web siguen congeladas. Has continuado gastando en el corto plazo, y el mañana se ve idéntico al ayer.',
      cartaTransformacion: 'Hola Paulo. Qué gran decisión tomaste al empezar a caminar, reduciendo esos gastos hormiga de transporte e impulsos diarios. Tras 12 meses de constancia, has acumulado S/ 1,550 en ahorros. No solo pagaste tu cuota de la pichanga sin preocupaciones y te compraste las zapatillas que querías, sino que lograste financiar tu Curso de Desarrollo Web e iniciar tu camino como programador. Este futuro alternativo brilla gracias al poder de tus pequeñas decisiones financieras.'
    }
  };

  // Signal que contiene los datos del espejo actual
  espejoData = signal<InsightEspejoTemporalDTO>(this.mockInsight);

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
