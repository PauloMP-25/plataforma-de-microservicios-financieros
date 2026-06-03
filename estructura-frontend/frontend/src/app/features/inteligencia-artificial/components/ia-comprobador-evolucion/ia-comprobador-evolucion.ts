import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';
import { IaHubComponent } from '../../pages/ia-hub/ia-hub';

import { LukaEscanerCargaComponent } from './components/luka-escaner-carga/luka-escaner-carga';
import { LukaEsqueletoSvgComponent, CategoriaHueso } from './components/luka-esqueleto-svg/luka-esqueleto-svg';
import { LukaPanelSignosVitalesComponent, KPIEvolucion } from './components/luka-panel-signos-vitales/luka-panel-signos-vitales';
import { LukaRecetaMedicaComponent } from './components/luka-receta-medica/luka-receta-medica';
import { LukaVeredictoFinalComponent } from './components/luka-veredicto-final/luka-veredicto-final';

export enum EstadoModulo {
  SELECTOR = 'SELECTOR',
  ESCANEO = 'ESCANEO',
  DASHBOARD = 'DASHBOARD'
}

export interface PeticionComparacionDTO {
  usuarioUuid: string;
  rangoA_inicio: string;
  rangoA_fin: string;
  rangoB_inicio: string;
  rangoB_fin: string;
}

export interface EvolucionDashboardData {
  categorias: Record<string, CategoriaHueso>;
  kpis: KPIEvolucion;
  imf: number;
  narrativaGemini: string;
}

@Component({
  selector: 'app-ia-comprobador-evolucion',
  standalone: true,
  imports: [
    CommonModule,
    LukaEscanerCargaComponent,
    LukaEsqueletoSvgComponent,
    LukaPanelSignosVitalesComponent,
    LukaRecetaMedicaComponent,
    LukaVeredictoFinalComponent
  ],
  templateUrl: './ia-comprobador-evolucion.html',
  styleUrl: './ia-comprobador-evolucion.scss'
})
export class IaComprobadorEvolucionComponent implements OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  private parentHub = inject(IaHubComponent);

  EstadoModulo = EstadoModulo;
  estadoActual: EstadoModulo = EstadoModulo.SELECTOR;

  // Estado Modal Receta
  isModalOpen = false;
  categoriaSeleccionada: CategoriaHueso | null = null;

  // Datos del Dashboard
  dashboardData!: EvolucionDashboardData;

  ngOnChanges(changes: SimpleChanges) {
    if (this.cargando) {
      this.estadoActual = EstadoModulo.ESCANEO;
    } else if (this.resultado && this.resultado.insight && Object.keys(this.resultado.insight).length > 0) {
      this.dashboardData = this.resultado.insight as EvolucionDashboardData;
      this.estadoActual = EstadoModulo.DASHBOARD;
    } else {
      this.estadoActual = EstadoModulo.SELECTOR;
    }
  }

  abrirReceta(categoriaId: string) {
    const cat = this.dashboardData?.categorias?.[categoriaId];
    if (cat) {
      this.categoriaSeleccionada = cat;
      this.isModalOpen = true;
    }
  }

  cerrarReceta() {
    this.isModalOpen = false;
    this.categoriaSeleccionada = null;
  }

  resetearModulo() {
    this.estadoActual = EstadoModulo.SELECTOR;
    this.isModalOpen = false;
    this.categoriaSeleccionada = null;
  }

  private cargarDashboardMock() {
    this.dashboardData = {
      categorias: {
        macro: {
          id: 'macro',
          nombre: 'Costos Fijos & Servicios (Cabeza/Columna)',
          estado: 'sano',
          gastoPeriodoA: 1200.00,
          gastoPeriodoB: 1150.00,
          desviacion: -4.1,
          descripcionLogro: '¡Columna alineada! Lograste optimizar tus servicios fijos y no incurriste en sobrecostos.'
        },
        ahorro: {
          id: 'ahorro',
          nombre: 'Ahorro Neto Acumulado (Esternón)',
          estado: 'sano',
          gastoPeriodoA: 300.00,
          gastoPeriodoB: 440.00,
          desviacion: -46.6,
          descripcionLogro: '¡Esternón fuerte! Tu reserva neta aumentó significativamente comparando ambos periodos.'
        },
        medianas: {
          id: 'medianas',
          nombre: 'Alimentación & Supermercados (Costillas)',
          estado: 'sano',
          gastoPeriodoA: 450.00,
          gastoPeriodoB: 420.00,
          desviacion: -6.6,
          descripcionLogro: '¡Caja torácica protegida! Compras planificadas de menú y provisiones mensuales.'
        },
        ocio: {
          id: 'ocio',
          nombre: 'Restaurantes, Salidas & Delivery (Brazos)',
          estado: 'fracturado',
          gastoPeriodoA: 200.00,
          gastoPeriodoB: 350.00,
          desviacion: 75.0,
          descripcionLogro: 'Fractura detectada por incremento de consumo recurrente los fines de semana.'
        },
        compras: {
          id: 'compras',
          nombre: 'Compras Ocasionales & Antojos (Piernas)',
          estado: 'fracturado',
          gastoPeriodoA: 150.00,
          gastoPeriodoB: 280.00,
          desviacion: 86.6,
          descripcionLogro: 'Micro-fracturas por compras sorpresa y delivery no planificado.'
        }
      },
      kpis: {
        deltaAhorro: {
          valor: 140.00,
          variacionRelativa: 46.6
        },
        ivg: {
          valor: 78,
          clasificacion: 'Caótico'
        },
        conquistas: ['Control de Fijos', 'Tasa Ahorro +46.6%', 'Alimentación bajo control'],
        alertas: ['Gasto Ocio Desmedido (+75%)', 'Frecuencia de antojos en alza']
      },
      imf: 64,
      narrativaGemini: 'Paulo, el diagnóstico revela que estás ganando estabilidad en tus bases (servicios fijos y alimentación protegida), pero tus extremidades (gastos de ocio y compras ocasionales) están fracturando gravemente tu flujo de caja neto. El Índice de Volatilidad de Gastos es del 78% (Caótico), lo que indica una alta inestabilidad semanal. Si logras aplicar la posología y sanar estas fracturas reduciendo la comida fuera de casa los fines de semana, consolidarás la base ideal para tu Laptop Gamer.'
    };
  }
}
