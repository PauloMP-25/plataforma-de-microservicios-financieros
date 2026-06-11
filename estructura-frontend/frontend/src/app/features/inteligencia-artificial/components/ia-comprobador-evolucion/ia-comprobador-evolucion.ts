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
      if (this.resultado.insight.hallazgos || (this.resultado.consejo && typeof this.resultado.consejo !== 'string')) {
        // Formato Real del Backend
        this.dashboardData = this.mapearDesdeBackend(this.resultado);
      } else {
        // Formato Mock Antiguo
        this.dashboardData = this.resultado.insight as EvolucionDashboardData;
      }
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

  private mapearDesdeBackend(res: RespuestaModuloDTO): EvolucionDashboardData {
    const hallazgos = res.insight?.hallazgos || {};
    const consejo = typeof res.consejo === 'string' ? JSON.parse(res.consejo || '{}') : (res.consejo || {});

    const imf = hallazgos.score_imf || 0;
    const narrativa = consejo.veredicto_narrativo || hallazgos.diagnostico_imf || '';

    const categorias: Record<string, CategoriaHueso> = {
      macro: { id: 'macro', nombre: 'Costos Fijos & Servicios (Cabeza/Columna)', estado: 'sano', gastoPeriodoA: 0, gastoPeriodoB: 0, desviacion: 0, descripcionLogro: 'Sin desviaciones críticas.' },
      ahorro: { id: 'ahorro', nombre: 'Ahorro Neto Acumulado (Esternón)', estado: 'sano', gastoPeriodoA: hallazgos.tasa_ahorro_a || 0, gastoPeriodoB: hallazgos.tasa_ahorro_b || 0, desviacion: hallazgos.delta_tasa_ahorro || 0, descripcionLogro: hallazgos.delta_tasa_ahorro >= 0 ? 'Reserva neta saludable y en crecimiento.' : 'Alerta: Capacidad de ahorro disminuida.' },
      medianas: { id: 'medianas', nombre: 'Alimentación & Supermercados (Costillas)', estado: 'sano', gastoPeriodoA: 0, gastoPeriodoB: 0, desviacion: 0, descripcionLogro: 'Alimentación bajo control.' },
      ocio: { id: 'ocio', nombre: 'Restaurantes, Salidas & Delivery (Brazos)', estado: 'sano', gastoPeriodoA: 0, gastoPeriodoB: 0, desviacion: 0, descripcionLogro: 'Gastos de ocio planificados.' },
      compras: { id: 'compras', nombre: 'Compras Ocasionales & Antojos (Piernas)', estado: 'sano', gastoPeriodoA: 0, gastoPeriodoB: 0, desviacion: 0, descripcionLogro: 'Gastos impulsivos controlados.' }
    };

    if (hallazgos.delta_tasa_ahorro < 0) {
      categorias['ahorro'].estado = 'fracturado';
    }

    const buscarReceta = (nombreG: string) => {
      const recetas = consejo.recetas_medicas || [];
      return recetas.find((r: any) => r.categoria.toLowerCase().includes(nombreG.toLowerCase()));
    };

    const conquistasList = hallazgos.categorias_conquistadas || [];
    const reincidentesList = hallazgos.categorias_reincidentes || [];

    const aplicarCategoria = (item: any, estadoAsignar: 'sanando' | 'fracturado') => {
      const catName = item.categoria.toLowerCase();
      let huesoId = 'compras';
      if (catName.includes('fijo') || catName.includes('servicio') || catName.includes('vivienda') || catName.includes('transporte')) huesoId = 'macro';
      else if (catName.includes('alimenta') || catName.includes('supermercado') || catName.includes('comida')) huesoId = 'medianas';
      else if (catName.includes('ocio') || catName.includes('restaurante') || catName.includes('delivery')) huesoId = 'ocio';

      const hueso = categorias[huesoId];
      hueso.estado = estadoAsignar;
      hueso.desviacion = item.aumento_pct || -item.reduccion_pct || 0;
      hueso.nombre = item.categoria;

      const receta = buscarReceta(item.categoria);
      if (receta) {
        hueso.descripcionLogro = receta.diagnostico;
        (hueso as any).posologia = receta.posologia;
        (hueso as any).pronostico = receta.pronostico;
      } else {
        hueso.descripcionLogro = estadoAsignar === 'fracturado' ? `Alerta: Incremento de gastos en ${item.categoria}.` : `Progreso: Gasto reducido en ${item.categoria}.`;
      }
    };

    conquistasList.forEach((c: any) => aplicarCategoria(c, 'sanando'));
    reincidentesList.forEach((c: any) => aplicarCategoria(c, 'fracturado'));

    const conquistas = conquistasList.map((c: any) => `${c.categoria} (-${c.reduccion_pct}%)`);
    const alertas = reincidentesList.map((c: any) => `${c.categoria} (+${c.aumento_pct}%)`);

    return {
      categorias,
      imf,
      narrativaGemini: narrativa,
      kpis: {
        deltaAhorro: { valor: hallazgos.delta_tasa_ahorro || 0, variacionRelativa: hallazgos.delta_tasa_ahorro || 0 },
        ivg: { valor: hallazgos.ivg_b || 0, clasificacion: (hallazgos.ivg_b > 40) ? 'Caótico' : ((hallazgos.ivg_b > 20) ? 'Inestable' : 'Estable') },
        conquistas: conquistas.length ? conquistas : ['Sin conquistas notables'],
        alertas: alertas.length ? alertas : ['Ninguna alerta crítica']
      }
    };
  }
}
