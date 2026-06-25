import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subscription, Observable } from 'rxjs';

import { IaService } from '../../../../core/services/ia.service';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';

import { IaModuloCardComponent } from './components/ia-modulo-card/ia-modulo-card';
import { IaPanelActivoComponent } from './components/ia-panel-activo/ia-panel-activo';
import { IaResultadoComponent } from './components/ia-resultado/ia-resultado';
import { IaHubHeaderComponent } from './components/ia-hub-header/ia-hub-header';
import { IaHubControlsComponent } from './components/ia-hub-controls/ia-hub-controls';
import { IaGastoHormigaComponent } from '../../components/ia-gasto-hormiga/ia-gasto-hormiga';
import { IaPrediccionGastosComponent } from '../../components/ia-prediccion-gastos/ia-prediccion-gastos';
import { IaHabitosFinancierosComponent } from '../../components/ia-habitos-financieros/ia-habitos-financieros';
import { IaRetoAhorroComponent } from '../../components/ia-reto-ahorro/ia-reto-ahorro';
import { IaSimularMetaComponent } from '../../components/ia-simular-meta/ia-simular-meta';
import { IaReporteAnualComponent } from '../../components/ia-reporte-anual/ia-reporte-anual';
import { IaEspejoTemporalComponent } from '../../components/ia-espejo-temporal/ia-espejo-temporal';
import { IaEstiloVidaComponent } from '../../components/ia-estilo-vida/ia-estilo-vida';
import { IaZonaEntrenamientoComponent } from '../../components/ia-zona-entrenamiento/ia-zona-entrenamiento';
import { IaComprobadorEvolucionComponent } from '../../components/ia-comprobador-evolucion/ia-comprobador-evolucion';

export interface IaModulo {
  id: string;
  label: string;
  descripcion: string;
  icon: string;
  tag: 'ANÁLISIS' | 'COACH';
  tagColor: string;
  endpoint: string;
  filtroFecha: boolean;
  params?: string[];
  bentoClass?: string;
  colorProfile?: string;
  proximamente?: boolean;
  features?: { icon: string, text: string }[];
}

export type TabGroup = 'TODOS' | 'ANÁLISIS' | 'COACH';

const IA_MODULOS: IaModulo[] = [
  {
    id: 'predecir-gastos',
    label: 'Predicción de Gastos',
    descripcion: 'Deja que Luka analice tus patrones ocultos para proyectar con alta precisión tus próximos egresos y evitar sorpresas de fin de mes.',
    icon: 'fa-solid fa-chart-line',
    tag: 'ANÁLISIS',
    tagColor: '#06b6d4',
    endpoint: '/api/v1/ia/predecir-gastos',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'neon-cyan',
    features: [
      { icon: 'fa-solid fa-magnifying-glass-chart', text: 'Proyecta egresos mensuales' },
      { icon: 'fa-solid fa-triangle-exclamation', text: 'Alerta saldos negativos' },
      { icon: 'fa-solid fa-shield-halved', text: 'Sugiere fondos de reserva' }
    ]
  },
  {
    id: 'simular-meta',
    label: 'Simular Meta',
    descripcion: 'Luka calculará la ruta más eficiente para que alcances tus sueños. Simula tiempos, cuotas y descubre atajos financieros.',
    icon: 'fa-solid fa-bullseye',
    tag: 'COACH',
    tagColor: '#10b981',
    endpoint: '/api/v1/ia/simular-meta',
    filtroFecha: false,
    params: ['nombre_meta', 'monto_objetivo', 'aporte_mensual_deseado'],
    bentoClass: 'bento-wide',
    colorProfile: 'matrix-green',
    features: [
      { icon: 'fa-solid fa-calendar-days', text: 'Calcula tiempos y plazos' },
      { icon: 'fa-solid fa-calculator', text: 'Estima cuotas de ahorro' },
      { icon: 'fa-solid fa-compass', text: 'Traza rutas de ahorro' }
    ]
  },
  {
    id: 'espejo-temporal',
    label: 'El Espejo del Tiempo',
    descripcion: 'Luka proyecta tu futuro financiero basándose en tus decisiones de hoy. Observa el díptico de continuidad frente al de transformación.',
    icon: 'fa-solid fa-scale-unbalanced-flip',
    tag: 'ANÁLISIS',
    tagColor: '#f97316',
    endpoint: '/api/v1/ia/espejo-tiempo',
    filtroFecha: false,
    bentoClass: 'bento-wide',
    colorProfile: 'neon-orange',
    features: [
      { icon: 'fa-solid fa-clock-rotate-left', text: 'Hitos a 3, 6 y 12 meses' },
      { icon: 'fa-solid fa-wand-magic-sparkles', text: 'Carta de transformación' },
      { icon: 'fa-solid fa-scale-unbalanced', text: 'Contador de impacto neto' }
    ]
  },
  {
    id: 'habitos-financieros',
    label: 'Hábitos Financieros',
    descripcion: 'Luka evalúa la calidad de tus decisiones diarias. Descubre si tus rutinas de consumo te acercan a la riqueza o a la deuda.',
    icon: 'fa-solid fa-brain',
    tag: 'ANÁLISIS',
    tagColor: '#8b5cf6',
    endpoint: '/api/v1/ia/habitos-financieros',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'synth-purple',
    features: [
      { icon: 'fa-solid fa-calendar-week', text: 'Evalúa consumo semanal' },
      { icon: 'fa-solid fa-basket-shopping', text: 'Detecta categorías frecuentes' },
      { icon: 'fa-solid fa-chart-line', text: 'Mide impacto patrimonial' }
    ]
  },
  {
    id: 'estilo-vida',
    label: 'Estilo de Vida',
    descripcion: 'Una radiografía de tu perfil como consumidor. Luka te muestra si tu nivel de gasto refleja realmente la vida que deseas tener.',
    icon: 'fa-solid fa-person-walking',
    tag: 'ANÁLISIS',
    tagColor: '#d946ef',
    endpoint: '/api/v1/ia/estilo-vida',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'plasma-magenta',
    features: [
      { icon: 'fa-solid fa-user-gear', text: 'Perfil de consumidor' },
      { icon: 'fa-solid fa-chart-simple', text: 'Aplica regla 50/30/20' },
      { icon: 'fa-solid fa-wand-magic-sparkles', text: 'Consejos de consumo óptimo' }
    ]
  },
  {
    id: 'comprobador-evolucion',
    label: 'Comprobador de Evolución',
    descripcion: 'Sala de Radiología Financiera. Compara dos períodos de tiempo mediante un diagnóstico radiográfico y prescribe recetas de optimización.',
    icon: 'fa-solid fa-bone',
    tag: 'ANÁLISIS',
    tagColor: '#84cc16',
    endpoint: '/api/v1/ia/comprobador-evolucion',
    filtroFecha: false,
    bentoClass: 'bento-wide',
    colorProfile: 'phosphor-lime',
    features: [
      { icon: 'fa-solid fa-skull-crossbones', text: 'Radiografía de categorías' },
      { icon: 'fa-solid fa-prescription-bottle-medical', text: 'Receta de optimización' },
      { icon: 'fa-solid fa-heart-pulse', text: 'Comparativa de índices y delta' }
    ]
  },
  {
    id: 'gasto-hormiga',
    label: 'Gasto Hormiga',
    descripcion: 'Luka detecta esas minúsculas pero letales fugas de dinero recurrentes que están erosionando silenciosamente tu patrimonio.',
    icon: 'fa-solid fa-bug',
    tag: 'ANÁLISIS',
    tagColor: '#ef4444',
    endpoint: '/api/v1/ia/gasto-hormiga',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'alert-red',
    features: [
      { icon: 'fa-solid fa-bug', text: 'Rastrea micro-consumos' },
      { icon: 'fa-solid fa-arrow-trend-down', text: 'Proyecta fugas anuales' },
      { icon: 'fa-solid fa-ban', text: 'Sustituye gastos nocivos' }
    ]
  },
  {
    id: 'zona-entrenamiento',
    label: 'Zona de Entrenamiento',
    descripcion: 'Centro de Alto Rendimiento Financiero. Entrena tus finanzas y sigue las rutinas prescritas por tu Coach Luka.',
    icon: 'fa-solid fa-dumbbell',
    tag: 'COACH',
    tagColor: '#14b8a6',
    endpoint: '/api/v1/ia/zona-entrenamiento',
    filtroFecha: false,
    bentoClass: 'bento-wide',
    colorProfile: 'neon-teal',
    features: [
      { icon: 'fa-solid fa-heart-pulse', text: 'Métricas de signos vitales' },
      { icon: 'fa-solid fa-clipboard-list', text: 'Rutinas de ahorro personalizadas' },
      { icon: 'fa-solid fa-person-running', text: 'Evaluación de perfil atlético' }
    ]
  },
  {
    id: 'reto-ahorro',
    label: 'Reto de Ahorro',
    descripcion: 'Tu coach Luka te propone misiones semanales personalizadas. Supera los retos para inyectar capital directo a tus ahorros.',
    icon: 'fa-solid fa-trophy',
    tag: 'COACH',
    tagColor: '#3b82f6',
    endpoint: '/api/v1/ia/reto-ahorro',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'neon-blue',
    features: [
      { icon: 'fa-solid fa-gamepad', text: 'Misiones semanales de ahorro' },
      { icon: 'fa-solid fa-sack-dollar', text: 'Ahorro directo a metas' },
      { icon: 'fa-solid fa-award', text: 'Puntos para tu score Luka' }
    ]
  },
  {
    id: 'reporte-completo',
    label: 'Reporte Ejecutivo',
    descripcion: 'Luka, tu coach financiero, consolida todos tus movimientos en un informe panorámico 360° para evaluar tu salud económica global.',
    icon: 'fa-solid fa-chart-pie',
    tag: 'ANÁLISIS',
    tagColor: '#f59e0b',
    endpoint: '/api/v1/ia/reporte-completo',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'cyber-gold',
    features: [
      { icon: 'fa-solid fa-chart-pie', text: 'Consolida ingresos y egresos' },
      { icon: 'fa-solid fa-heart-pulse', text: 'Evalúa tu salud financiera' },
      { icon: 'fa-solid fa-lightbulb', text: 'Optimiza tu capital disponible' }
    ]
  }
];

@Component({
  selector: 'app-ia-hub',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    IaModuloCardComponent,
    IaPanelActivoComponent,
    IaResultadoComponent,
    IaHubHeaderComponent,
    IaHubControlsComponent,
    IaGastoHormigaComponent,
    IaPrediccionGastosComponent,
    IaHabitosFinancierosComponent,
    IaRetoAhorroComponent,
    IaSimularMetaComponent,
    IaEstiloVidaComponent,
    IaReporteAnualComponent,
    IaEspejoTemporalComponent,
    IaZonaEntrenamientoComponent,
    IaComprobadorEvolucionComponent
  ],
  templateUrl: './ia-hub.html',
  styleUrl: './ia-hub.scss'
})
export class IaHubComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private iaService = inject(IaService);

  modulos = IA_MODULOS;
  moduloSeleccionado = signal<IaModulo | null>(null);
  resultado = signal<RespuestaModuloDTO | null>(null);
  cargando = signal(false);
  errorMsg = signal<string | null>(null);
  hitoEspejo = signal<3 | 6 | 12>(12);

  // Animación de Transición
  transicionActiva = signal(false);

  // ── Filtros del Panel Principal ──
  tabActiva = signal<TabGroup>('TODOS');
  tabs: TabGroup[] = ['TODOS', 'ANÁLISIS', 'COACH'];
  tabIcons: Record<TabGroup, string> = {
    'TODOS': 'fa-solid fa-layer-group',
    'ANÁLISIS': 'fa-solid fa-magnifying-glass-chart',
    'COACH': 'fa-solid fa-graduation-cap',
  };

  modulosFiltrados = computed(() => {
    const tab = this.tabActiva();
    return tab === 'TODOS' ? this.modulos : this.modulos.filter(m => m.tag === tab);
  });

  consultasQuotaText = computed(() => {
    return `${this.iaService.consultasRestantes()}/${this.iaService.consultasMaximas()}`;
  });

  private querySub!: Subscription;

  ngOnInit() {
    this.querySub = this.route.queryParams.subscribe(params => {
      const moduloId = params['modulo'];
      if (moduloId) {
        const found = this.modulos.find(m => m.id === moduloId);
        if (found) {
          this.moduloSeleccionado.set(found);
          this.resultado.set(null); // Limpiar resultado previo al cambiar de módulo
          this.errorMsg.set(null);
          this.hitoEspejo.set(12);
        } else {
          this.moduloSeleccionado.set(null);
          this.resultado.set(null);
          this.errorMsg.set(null);
        }
      } else {
        this.moduloSeleccionado.set(null);
        this.resultado.set(null);
        this.errorMsg.set(null);
      }
    });
  }

  ngOnDestroy() {
    if (this.querySub) {
      this.querySub.unsubscribe();
    }
  }

  conteoPor(tab: string): number {
    return tab === 'TODOS' ? this.modulos.length : this.modulos.filter(m => m.tag === tab).length;
  }

  cambiarTab(tab: TabGroup): void {
    this.tabActiva.set(tab);
  }

  getCuotaText(tab: TabGroup): string {
    if (tab === 'TODOS') return '∞ Consultas';
    if (tab === 'ANÁLISIS') return '∞ Consultas';
    if (tab === 'COACH') return '∞ Consultas';
    return '';
  }

  seleccionarModulo(modulo: IaModulo) {
    if (modulo.proximamente) return;
    this.transicionActiva.set(true);
    setTimeout(() => {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { modulo: modulo.id },
        queryParamsHandling: 'merge'
      }).then(() => {
        this.transicionActiva.set(false);
      });
    }, 400); // Transición más ágil
  }

  deseleccionarModulo() {
    this.transicionActiva.set(true);
    setTimeout(() => {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { modulo: null },
        queryParamsHandling: 'merge'
      }).then(() => {
        this.transicionActiva.set(false);
      });
    }, 400);
  }

  // ── Simulación Estática con Respuesta Mock (Fase de Pruebas) ──
  ejecutarAnalisis(payload: any) {
    const mod = this.moduloSeleccionado();
    if (!mod) return;

    this.cargando.set(true);
    this.errorMsg.set(null);
    this.resultado.set(null);

    // Integración REAL con Backend para todos los módulos
    let apiCall: Observable<any> | null = null;

    switch (mod.id) {
      case 'gasto-hormiga':
        apiCall = this.iaService.getGastoHormiga(payload);
        break;
      case 'predecir-gastos':
        apiCall = this.iaService.getPredecirGastos(payload);
        break;
      case 'habitos-financieros':
        apiCall = this.iaService.getHabitosFinancieros(payload);
        break;
      case 'estilo-vida':
        apiCall = this.iaService.getEstiloVida(payload);
        break;
      case 'reporte-completo':
        apiCall = this.iaService.getReporteCompleto(payload);
        break;
      case 'simular-meta':
        apiCall = this.iaService.getSimularMeta(payload);
        break;
      case 'reto-ahorro':
        apiCall = this.iaService.getRetoAhorro(payload);
        break;
      case 'comprobador-evolucion':
        apiCall = this.iaService.getComprobadorEvolucion(payload);
        break;
      case 'zona-entrenamiento':
        apiCall = this.iaService.getZonaEntrenamiento(payload);
        break;
      case 'espejo-temporal':
        apiCall = this.iaService.getEspejoTemporal(payload);
        break;
    }

    if (apiCall) {
      apiCall.subscribe({
        next: (res) => {
          if (res.exito && res.datos) {
            this.resultado.set(res.datos);
          } else {
            this.errorMsg.set(res.mensaje || 'Error desconocido al consultar el backend.');
          }
          this.cargando.set(false);
        },
        error: (err) => {
          console.warn('Fallback a mockup estático activado debido a un error de conexión', err);
          this.errorMsg.set('Conexión con la clínica financiera falló (' + err.message + '). Mostrando datos de simulación estática (Mockup).');
          const mockRes = this._generarRespuestaMock(mod, payload);
          this.resultado.set(mockRes);
          this.iaService.descontarConsulta();
          this.cargando.set(false);
        }
      });
      return;
    }

    // Fallback para módulos no configurados
    setTimeout(() => {
      try {
        const mockRes = this._generarRespuestaMock(mod, payload);
        this.resultado.set(mockRes);
        this.iaService.descontarConsulta();
        this.cargando.set(false);
      } catch (err: any) {
        this.cargando.set(false);
        this.errorMsg.set('Ocurrió un error en la simulación estática: ' + err.message);
      }
    }, 2500);
  }

  private _generarRespuestaMock(modulo: IaModulo, payload?: any): RespuestaModuloDTO {
    const ahora = new Date().toISOString();

    const insights: Record<string, any> = {
      'gasto-hormiga': {
        hay_hormigas: true,
        principal_gasto_hormiga: 'Cafetería',
        total_gastos_hormiga: 360.50,
        proyeccion_fuga_anual: 4320.00,
        ingreso_mensual_referencia: 2000.00,
        variacion_vs_mes_anterior: 20.0,
        gastos_detectados: [
          { descripcion: 'Café diario fuera de casa', frecuencia: '30 veces/mes', total: 360.50, categoria: 'Cafetería', promedio_por_compra: 12.00, dia_mayor_gasto: 'Todos los días' }
        ]
      },
      'predecir-gastos': {
        tiene_datos: true,
        promedio_historico: 7676.94,
        proyeccion_proximo_mes: 17427.48,
        gasto_proyectado: 17427.48,
        balance_proyectado: -13927.48,
        pendiente: 'SUBE',
        porcentaje_variacion_mensual: 84.7,
        ingreso_mensual: 3500.0,
        riesgo_quiebra: true,
        deficit_estimado: 13927.48,
        meses_analizados: 2,
        tiene_tendencia_alcista: true,
        historial_meses: [7676.94, 15353.88, 17427.48],
        ingreso_esperado: 3500.0,
        hallazgos: {
          tiene_datos: true,
          promedio_historico: 7676.94,
          proyeccion_proximo_mes: 17427.48,
          gasto_proyectado: 17427.48,
          balance_proyectado: -13927.48,
          pendiente: 6500.36,
          porcentaje_variacion_mensual: 84.7,
          ingreso_mensual: 3500.0,
          riesgo_quiebra: true,
          deficit_estimado: 13927.48,
          meses_analizados: 2,
          tiene_tendencia_alcista: true
        }
      },
      'habitos-financieros': {
        frecuencia_analizada: 'SEMANAL',
        dia_mayor_gasto: 'Saturday',
        categoria_mas_frecuente: 'Restaurantes',
        total_transacciones_periodo: 18,
        es_saludable: false,
        puntuacion_habito: 72,
        dimensiones: {
          'ESTA_SEMANA': { Constancia: 75, Ahorro: 45, Control: 70, Diversidad: 50, Puntualidad: 85, Equilibrio: 60 },
          'SEMANA_PASADA': { Constancia: 65, Ahorro: 40, Control: 60, Diversidad: 45, Puntualidad: 80, Equilibrio: 55 }
        },
        heatmap_datos: (() => {
          const list = [];
          const diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

          for (let i = 0; i < 28; i++) {
            const w = Math.floor(i / 7) + 1;
            const dIdx = i % 7;
            const diaSemana = diasSemana[dIdx];
            const diaNum = i + 1;

            let monto = 0;
            let nivel = 1;
            let transacciones: any[] = [];

            if (diaSemana === 'Sábado') {
              monto = w === 4 ? 185.00 : (w === 3 ? 150.00 : (w === 2 ? 120.00 : 95.00));
              nivel = 5;
              transacciones = [
                { descripcion: 'Cena rústica', monto: monto * 0.6, categoria: 'Restaurantes', icono: 'fa-utensils' },
                { descripcion: 'Bebidas fin de semana', monto: monto * 0.3, categoria: 'Ocio', icono: 'fa-glass-water' },
                { descripcion: 'Taxi retorno', monto: monto * 0.1, categoria: 'Transporte', icono: 'fa-taxi' }
              ];
            } else if (diaSemana === 'Viernes') {
              monto = 65.00;
              nivel = 4;
              transacciones = [
                { descripcion: 'Cine 2D combo', monto: 45.00, categoria: 'Ocio', icono: 'fa-film' },
                { descripcion: 'Dulces snacks', monto: 20.00, categoria: 'Alimentación', icono: 'fa-cookie' }
              ];
            } else if (diaSemana === 'Lunes' && w === 2) {
              monto = 42.00;
              nivel = 3;
              transacciones = [
                { descripcion: 'Cafetería Starbucks', monto: 12.00, categoria: 'Cafetería', icono: 'fa-mug-hot' },
                { descripcion: 'Menú Ejecutivo', monto: 30.00, categoria: 'Alimentación', icono: 'fa-bowl-food' }
              ];
            } else {
              monto = Math.random() > 0.6 ? 12.00 : 0;
              nivel = monto > 0 ? 2 : 1;
              if (monto > 0) {
                transacciones = [{ descripcion: 'Cafecito rápido', monto: monto, categoria: 'Alimentación', icono: 'fa-mug-hot' }];
              }
            }

            list.push({
              fecha: `2026-05-${diaNum.toString().padStart(2, '0')}`,
              diaSemana,
              diaNum,
              monto,
              nivel,
              transacciones
            });
          }
          return list;
        })()
      },
      'estilo-vida': {
        cluster_dominante: 'FOODIE',
        porcentaje_dominancia: 65.4,
        total_analizado: 1250.00,
        personalidad_detectada: 'El Foodie Explorador'
      },
      'reporte-completo': {
        score_salud: 78,
        balance_anual: 2450.00,
        total_ingresos: 15000.00,
        total_gastos: 12550.00,
        categoria_critica: 'Ocio',
        porcentaje_gasto_critico: 25
      },
      'simular-meta': {
        modulo: "SIMULAR_META",
        total_transacciones_analizadas: 102,
        total_ingresos: 29499.54,
        total_gastos: 15353.88,
        balance_neto: 14145.66,
        promedio_gasto_mensual: 0.0,
        promedio_ingreso_mensual: 0.0,
        meta_nombre: "Laptop Gamer",
        monto_objetivo: Number(payload?.monto_objetivo) || 5000.0,
        aporte_mensual: Number(payload?.aporte_mensual_deseado) || 350.0,
        meses_estimados: 0.4,
        fecha_proyectada: 'Junio 2026',
        viabilidad_fecha_objetivo: true,
        ahorro_faltante: 5000.0,
        escenario_alternativo: {
          aporte: (Number(payload?.aporte_mensual_deseado) || 350) + 150,
          meses: Math.ceil((Number(payload?.monto_objetivo) || 5000) / ((Number(payload?.aporte_mensual_deseado) || 350) + 150))
        },
        raw_payload: {
          nombre_meta: payload?.nombre_meta || "Laptop Gamer",
          monto_objetivo: Number(payload?.monto_objetivo) || 5000,
          monto_actual_ahorrado: Number(payload?.monto_actual_ahorrado) || 0,
          aporte_mensual_deseado: Number(payload?.aporte_mensual_deseado) || 350
        },
        hallazgos: {
          meta_nombre: payload?.nombre_meta || "Laptop Gamer",
          es_viable: true,
          meses_para_lograrlo: 0.4,
          capacidad_ahorro_mensual: 14145.66,
          viabilidad_fecha_objetivo: null,
          ahorro_faltante: 5000.0,
          _hash_txs: "11b314776c3e783c",
          _descripcion_rango: "todos-los-periodos",
          _total_ingresos: 29499.54,
          _total_gastos: 15353.88
        }
      },
      'reto-ahorro': {
        estado_reto: 'ACTIVO',
        id_reto: 45,
        categoria_objetivo: 'Restaurantes',
        progreso_temporal: 45,
        monto_limite: 50.00,
        duracion_dias: 7,
        dificultad: 'Media',
        exito: true,
        ahorro_real: 85.00,
        gasto_real: 92.50
      },
      'espejo-temporal': {
        datosPresente: {
          scoreActual: 42,
          saldoActual: 85.00,
          metasActivas: 0
        },
        proyeccionContinuidad: {
          hitos3Meses: {
            scoreProyectado: 38,
            ahorroAcumulado: 30.00,
            metasCumplidas: [],
            metasFracasadas: []
          },
          hitos6Meses: {
            scoreProyectado: 35,
            ahorroAcumulado: 50.00,
            metasCumplidas: [],
            metasFracasadas: []
          },
          hitos12Meses: {
            scoreProyectado: 30,
            ahorroAcumulado: 100.00,
            metasCumplidas: [],
            metasFracasadas: []
          }
        },
        proyeccionTransformacion: {
          hitos3Meses: {
            scoreProyectado: 65,
            ahorroAcumulado: 450.00,
            metasCumplidas: [],
            metasFracasadas: []
          },
          hitos6Meses: {
            scoreProyectado: 78,
            ahorroAcumulado: 950.00,
            metasCumplidas: [],
            metasFracasadas: []
          },
          hitos12Meses: {
            scoreProyectado: 85,
            ahorroAcumulado: 1550.00,
            metasCumplidas: [],
            metasFracasadas: []
          }
        },
        narrativasGemini: {
          cartaContinuidad: 'Hola Paulo del futuro. Veo que sigues gastando sin un objetivo claro. Hoy, después de un año, tu ahorro acumulado apenas llega a S/ 100. No tienes metas activas, y tu dinero se diluye mes a mes sin construir patrimonio.',
          cartaTransformacion: 'Hola Paulo. Qué gran decisión de ahorro tomaste al reducir esos gastos hormiga de transporte y antojos de fin de semana. Tras 12 meses de constancia, has acumulado S/ 1,550 en ahorros. Aunque actualmente no tienes metas configuradas, estás construyendo un fondo de emergencia sólido.'
        }
      },
      'comprobador-evolucion': {
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
        narrativaGemini: 'Paulo, el diagnóstico revela que estás ganando estabilidad en tus bases, pero la falta de metas claras hace que tus excedentes se disipen en ocio. El Índice de Volatilidad es Caótico. Define una meta pronto para orientar este esfuerzo.'
      },
      'clasificar-transaccion': {
        descripcion: payload?.descripcion || 'Rappi Alimentos',
        monto: Number(payload?.monto) || 45.00,
        sugerencias: [
          { categoria: 'Alimentación', confianza: 95 },
          { categoria: 'Delivery', confianza: 82 },
          { categoria: 'Ocio', confianza: 34 }
        ]
      },
      'zona-entrenamiento': {
        hallazgos: {
          frecuencia_cardiaca: 2.5,
          presion_arterial: 1.5,
          temperatura_ahorro: 5,
          saturacion_pct: 45
        }
      }
    };

    const consejos: Record<string, any> = {
      'gasto-hormiga': {
        pensamiento_interno_ia: 'He analizado los gastos hormiga de Cesar, notando una mejora en la reducción de fugas. Mi objetivo es ofrecer 5 pasos prácticos y conectar la fuga con su meta.',
        analisis_ia: '¡Hola, Cesar! He estado revisando tus movimientos este mes. Tus gastos hormiga suman S/ 181.95, con el transporte siendo la categoría principal de fuga. ¡Pero hay buenas noticias! Has logrado disminuir tus gastos hormiga en un 11.2% respecto al mes anterior. Sin embargo, si esta tendencia de fuga se mantiene, podrías estar perdiendo S/ 2,183.40 al año.',
        conexion_emocional: 'Esos S/ 181.95 que se te escaparon este mes, y la proyección anual de S/ 2,183.40, podrían ser una parte de tus ahorros para tu \'Viaje a Japón\'. ¡Imagina todo lo que harías con ese dinero allá!',
        plan_accion_titulo: 'Plan de Acción: ¡Ahorra en Transporte para Japón!',
        plan_accion_pasos: [
          'Identifica rutas alternativas o considera caminar/bicicleta para trayectos cortos.',
          'Revisa tus gastos de transporte diarios y busca al menos una forma de optimizarlos esta semana (ej. usar transporte público en lugar de taxi).',
          'Establece un presupuesto semanal específico para transporte y monitorea que no lo excedas.',
          'Organiza viajes compartidos con compañeros que tengan rutas similares para dividir los costos.',
          'Camina en los tramos cortos de menos de 10 cuadras; además de ahorrar, beneficiará tu salud diaria.'
        ],
        comentario_positivo: '¡Sigue así, Cesar! Cada pequeño ajuste te acerca más a tu increíble viaje a Japón. ¡Estoy aquí para apoyarte en cada paso!'
      },
      'predecir-gastos': {
        pensamiento_interno_ia: "El riesgo de insolvencia es extremadamente alto. Con un ingreso de S/ 3,500.00 y gastos proyectados de S/ 17,427.48, el déficit de S/ 13,927.48 es insostenible. La tendencia de gastos está completamente desalineada con los ingresos, requiriendo una intervención drástica.",
        analisis_tendencia: "¡Hola, Cesar! LUKA aquí, tu estratega financiero. Mira, la proyección para el próximo mes nos muestra un panorama que necesita nuestra atención. Tus gastos proyectados se disparan a S/ 17,427.48, lo que representa un aumento del 84.7% mensual.",
        impacto_meta: "Con esta tendencia, tu emocionante viaje a Japón, que ya lleva un 15% de progreso, se ve seriamente comprometido. Un déficit proyectado de S/ 13,927.48 significa que no solo no podrás ahorrar para tu meta, sino que estarías gastando mucho más de lo que ingresas, alejándote de ese sueño.",
        recomendacion_matematica: "Para evitar el déficit proyectado y al menos igualar tus ingresos, necesitas reducir tus gastos proyectados en al menos S/ 13,927.48, lo que representa aproximadamente un 80% de tus gastos actuales proyectados.",
        mensaje_motivacional: "Sé que suena fuerte, pero estoy aquí para ayudarte a retomar el control y redirigir tu camino hacia tus metas. ¡Juntos podemos hacerlo!"
      },
      'habitos-financieros': {
        pensamiento_interno_ia: 'Se identificó que el gasto de los martes es un patrón fuerte que puede desestabilizar la meta si no se controla.',
        analisis_patron: '¡Hola Cesar! He estado revisando tus movimientos y he notado algo interesante: los martes parecen ser tu día de mayor gasto, especialmente en la categoría de Alimentación. Esto es un patrón claro que podemos observar en tus finanzas semanales.',
        habito_atomico_sugerido: 'Para empezar a tomar control de ese día, ¿qué te parece si cada lunes por la noche dedicas 5 minutos a planificar tus comidas del martes o, mejor aún, preparas tu almuerzo para llevar al trabajo ese día? Es un pequeño gesto que puede hacer una gran diferencia.',
        mensaje_motivacional: 'Recuerda, Cesar, cada pequeño cambio suma. ¡Tú tienes el poder de construir los hábitos que te llevarán a Japón!'
      },
      'estilo-vida': {
        pensamiento_interno_ia: 'Se detecta un fuerte enfoque en el cluster EXPLORER (15.9%) y un interés secundario en WELLNESS (5.2%). Esto sugiere un perfil que valora las experiencias y el autocuidado.',
        arquetipo: 'El Explorador Consciente',
        significado_arquetipo: 'Significa que te encanta descubrir nuevos lugares y experiencias, pero también valoras mucho tu bienestar y cuidado personal. Buscas un equilibrio entre la aventura y el autocuidado.',
        descripcion_perfil: '¡Hola, Cesar! Qué gusto verte por aquí. Analizando tus movimientos, veo que eres una persona que valora mucho las experiencias y el bienestar. Tus gastos muestran una clara inclinación a explorar el mundo y a invertir en tu cuidado personal, lo cual es genial para una vida plena.',
        consejo_tactico: 'Para seguir explorando sin afectar tu meta, ¿qué tal si buscas aventuras más cercanas o escapadas de fin de semana que no requieran grandes inversiones? También puedes explorar opciones de bienestar al aire libre.',
        alineacion_meta: 'Tu estilo de vida explorador está muy alineado con tu meta de viajar a Japón, ¡es la esencia de lo que te mueve! Sin embargo, es clave que tus exploraciones actuales no compitan demasiado con el ahorro para ese gran viaje.',
        mensaje_estilo_vida: 'Sigue explorando el mundo y cuidando de ti, Cesar. Cada paso consciente te acerca a tu próxima gran aventura.'
      },
      'reporte-completo': {
        pensamiento_interno_ia: 'El score es alto, finanzas estables.',
        analisis_score: 'Tu Score LUKA es 78/100. Has mantenido un crecimiento constante en tus ahorros desde el 1 de enero. Tu balance anual positivo de S/ 2,450.00 indica una gestión responsable.',
        impacto_meta: 'Eres un Ahorrador Estratégico. Mantén este ritmo y cerrarás el año con la solvencia necesaria.',
        veredicto_final: 'Aprobado',
        mensaje_motivacional: '¡Gran trabajo, Paulo!'
      },
      'simular-meta': {
        pensamiento_interno_ia: "El monto faltante para la Laptop Gamer es S/ 5000.0. La capacidad de ahorro mensual detectada es S/ 14145.66. Dado que la capacidad de ahorro excede significativamente el monto faltante, la meta es altamente viable y se puede alcanzar en menos de un mes (0.4 meses).",
        diagnostico_viabilidad: "¡Hola, Cesar! Soy LUKA y tengo excelentes noticias para ti. ¡Tu meta de la Laptop Gamer es completamente viable! Con tu capacidad de ahorro actual, podrás alcanzarla en un abrir y cerrar de ojos.",
        plan_accion: "Para alcanzar tu Laptop Gamer, simplemente mantén tu ritmo de ahorro actual. Te sugiero destinar S/ 5000.0 de tu capacidad de ahorro mensual directamente a esta meta. ¡Lo lograrás muy pronto!",
        tecnica_sugerida: "Ahorro Automático: Configura una transferencia automática desde tu cuenta principal a una cuenta de ahorro específica para tu Laptop Gamer tan pronto recibas tu ingreso. Así, el ahorro se convierte en una prioridad y no en lo que 'sobra'.",
        mensaje_motivacional: "¡Sigue así, Cesar! Tu disciplina financiera te está llevando a cumplir tus sueños más rápido de lo que imaginas. ¡Estoy emocionado por ver esa nueva laptop en tus manos!"
      },
      'reto-ahorro': {
        pensamiento_interno_ia: 'El usuario gasta en restaurantes. Cortar este gasto genera ahorro rápido.',
        titulo_mision: 'Operación Cocina en Casa 🏆',
        diagnostico: 'Paulo, he detectado que tu Enemigo Final de esta semana son los Restaurantes.',
        estrategia: 'Tu misión, si decides aceptarla, es evitar comer fuera por los próximos 7 días.',
        mensaje_motivacional: 'Si lo logras, habrás salvado S/ 85.00 para tus fondos. ¿Aceptas el reto, Jugador 1?'
      },
      'espejo-temporal': '¡Hola Paulo! Soy tu coach financiero Luka. Tras proyectar tus finanzas a 12 meses, la diferencia neta de ahorro acumulado proyectado entre tus dos futuros es de S/ 1,450.00. Si continúas con tus hábitos del pasado, el dinero se diluirá sin una meta. Sin embargo, al activar tu plan de transformación lograrás capitalizar tus ahorros a S/ 1,550.00, creando un sólido fondo de emergencia.',
      'comprobador-evolucion': {
        pensamiento_interno_ia: 'No hay metas activas, diagnosticando estado base.',
        veredicto_narrativo: 'Paulo, el diagnóstico revela que estás ganando estabilidad en tus bases, pero la falta de metas claras hace que tus excedentes se disipen en ocio. El Índice de Volatilidad es Caótico. Define una meta pronto para orientar este esfuerzo.',
        recetas_medicas: [
          { categoria: 'Ocio', diagnostico: 'Sin metas que protejan el excedente, el ocio absorbe la liquidez.', posologia: 'Definir una meta a corto plazo.', pronostico: 'Reservas estancadas.' }
        ]
      },
      'clasificar-transaccion': {
        pensamiento_interno_ia: 'Clasificando transacción.',
        categorias_sugeridas: ['Alimentación', 'Delivery', 'Ocio']
      },
      'zona-entrenamiento': {
        pensamiento_interno_ia: 'Generando rutina de contingencia desde el front-end.',
        estado_fisico: 'Sedentario',
        evaluacion_previa: 'Actualmente no has definido ninguna meta financiera en el sistema. Entrenaremos a ciegas para mejorar tus métricas vitales básicas.',
        rutina: [
          {
            nombre: 'Sprint de Ahorro',
            descripcion: 'No gastar en la categoría Entretenimiento/Antojos por 3 días seguidos.',
            duracion_dias: 3,
            frecuencia: 'Entretenimiento',
            metrica_exito: 'S/ 30.00 retenidos'
          },
          {
            nombre: 'Levantamiento de Presupuesto',
            descripcion: 'Registrar cada sol gastado en Pichangas antes de que termine el día.',
            duracion_dias: 7,
            frecuencia: 'Deportes',
            metrica_exito: '100% visibilidad'
          },
          {
            nombre: 'Flexibilidad de Cuota',
            descripcion: 'Separar S/ 6 fijos al inicio de la semana exclusivamente para salidas.',
            duracion_dias: 30,
            frecuencia: 'Planificación',
            metrica_exito: 'Presupuesto blindado'
          }
        ]
      }
    };

    // Estructuras de gráficos para visualizar en IaResultadoComponent
    const graficos: Record<string, any> = {
      'gasto-hormiga': {
        titulo: 'Fuga Mensual por Categoría',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Cafeterías', valor: 360.50, color: '#f59e0b' }
        ]
      },
      'predecir-gastos': {
        titulo: 'Proyección de Gastos por Categoría',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Fijos (Vivienda/Servicios)', valor: 12000, color: '#10b981' },
          { etiqueta: 'Alimentación', valor: 4928.72, color: '#3b82f6' },
          { etiqueta: 'Entretenimiento', valor: 450, color: '#f43f5e' },
          { etiqueta: 'Otros', valor: 48.76, color: '#6b7280' }
        ]
      },
      'habitos-financieros': {
        titulo: 'Desglose de Puntuación de Hábitos',
        tipoGrafico: 'BARRAS',
        unidad: '%',
        datos: [
          { etiqueta: 'Constancia de Registro', valor: 75, color: '#10b981' },
          { etiqueta: 'Control de Presupuestos', valor: 70, color: '#3b82f6' },
          { etiqueta: 'Capacidad de Ahorro', valor: 45, color: '#ef4444' }
        ]
      },
      'estilo-vida': {
        titulo: 'Regla de Oro: Distribución del Gasto',
        tipoGrafico: 'BARRAS',
        unidad: '%',
        datos: [
          { etiqueta: 'Necesidades (Actual)', valor: 55, color: '#3b82f6' },
          { etiqueta: 'Deseos / Estilo Vida (Actual)', valor: 35, color: '#f59e0b' },
          { etiqueta: 'Ahorro (Actual)', valor: 10, color: '#ef4444' }
        ]
      },
      'reporte-completo': {
        titulo: 'Balance de Ingresos vs Egresos',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Ingresos Totales', valor: 3200, color: '#10b981' },
          { etiqueta: 'Gastos Totales', valor: 2480, color: '#ef4444' }
        ]
      },
      'simular-meta': {
        titulo: 'Ahorro Acumulado Recomendado',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Meta Total', valor: Number(payload?.monto_objetivo) || 5000, color: '#cbd5e1' },
          { etiqueta: 'Aporte Mensual Deseado', valor: Number(payload?.aporte_mensual_deseado) || 350, color: '#22d3ee' },
          { etiqueta: 'Capacidad de Ahorro Real', valor: 14145.66, color: '#a855f7' }
        ]
      },
      'reto-ahorro': {
        titulo: 'Ahorro Acumulado por Semana',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Semana 1 (Fase No-Café)', valor: 50, color: '#f59e0b' },
          { etiqueta: 'Semana 2 (Fase Cocinar)', valor: 120, color: '#3b82f6' },
          { etiqueta: 'Semana 3 (Fase Cancelar App)', valor: 150, color: '#a855f7' },
          { etiqueta: 'Semana 4 (Fase S/.5 diario)', valor: 178, color: '#10b981' }
        ]
      },
      'clasificar-transaccion': {
        titulo: 'Nivel de Coincidencia de Categoría',
        tipoGrafico: 'BARRAS',
        unidad: '%',
        datos: [
          { etiqueta: 'Alimentación (Recomendado)', valor: 95, color: '#10b981' },
          { etiqueta: 'Delivery/Rappi', valor: 82, color: '#f59e0b' },
          { etiqueta: 'Ocio', valor: 34, color: '#cbd5e1' }
        ]
      }
    };

    const kpis: Record<string, any> = {
      'gasto-hormiga': { valor: 360.50, etiqueta: 'Fuga mensual por antojos', tendencia: 'ALZA', variacion_porcentaje: 20.0, unidad: 'S/.' },
      'predecir-gastos': { valor: 17427.48, etiqueta: 'Gasto Proyectado Próximo Mes', tendencia: 'ALZA', variacion_porcentaje: 84.7, unidad: 'S/.' },
      'habitos-financieros': { valor: 72, etiqueta: 'Índice de Hábitos Financieros', tendencia: 'ESTABLE', variacion_porcentaje: 0, unidad: '/100' },
      'estilo-vida': { valor: 55, etiqueta: 'Porcentaje Gasto Necesidades', tendencia: 'BAJA', variacion_porcentaje: -3.2, unidad: '%' },
      'reporte-completo': { valor: 720, etiqueta: 'Ahorro Neto este Mes', tendencia: 'ALZA', variacion_porcentaje: 25.4, unidad: 'S/.' },
      'simular-meta': { valor: 0.4, etiqueta: 'Meses Requeridos para la Meta', tendencia: 'ESTABLE', variacion_porcentaje: 0, unidad: 'meses' },
      'reto-ahorro': { valor: 85.00, etiqueta: 'Ahorro Estimado Total', tendencia: 'ALZA', variacion_porcentaje: 100, unidad: 'S/.' },
      'espejo-temporal': { valor: 1450, etiqueta: 'Impacto Neto Proyectado a 12 Meses', tendencia: 'ALZA', variacion_porcentaje: 0, unidad: 'S/.' },
      'comprobador-evolucion': { valor: 64, etiqueta: 'Índice de Salud Financiera Metódica (IMF)', tendencia: 'BAJA', variacion_porcentaje: 0, unidad: '%' },
      'clasificar-transaccion': { valor: 95, etiqueta: 'Nivel de Confianza', tendencia: 'ALZA', variacion_porcentaje: 0, unidad: '%' }
    };

    return {
      id_respuesta: `mock_${modulo.id}_${Math.random().toString(36).substring(2, 9)}`,
      usuario_id: 'usuario_luka_test',
      modulo: modulo.label,
      fecha_generacion: ahora,
      estado_coach: 'EXITOSO',
      usando_fallback: true,
      insight: insights[modulo.id] || {},
      consejo: consejos[modulo.id] || 'Simulación completada exitosamente en el entorno de pruebas.',
      grafico: graficos[modulo.id],
      kpi: kpis[modulo.id]
    };
  }
}
