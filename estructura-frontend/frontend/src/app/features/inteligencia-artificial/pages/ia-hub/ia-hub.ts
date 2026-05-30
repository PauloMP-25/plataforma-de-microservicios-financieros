import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { IaService } from '../../../../core/services/ia.service';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

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
import { IaEstiloVidaComponent } from '../../components/ia-estilo-vida/ia-estilo-vida';
import { IaReporteAnualComponent } from '../../components/ia-reporte-anual/ia-reporte-anual';






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
    id: 'reto-ahorro',
    label: 'Reto de Ahorro',
    descripcion: 'Tu coach Luka te propone misiones semanales personalizadas. Supera los retos para inyectar capital directo a tus ahorros.',
    icon: 'fa-solid fa-trophy',
    tag: 'COACH',
    tagColor: '#10b981',
    endpoint: '/api/v1/ia/reto-ahorro',
    filtroFecha: true,
    bentoClass: 'bento-wide',
    colorProfile: 'matrix-green',
    features: [
      { icon: 'fa-solid fa-gamepad', text: 'Misiones semanales de ahorro' },
      { icon: 'fa-solid fa-sack-dollar', text: 'Ahorro directo a metas' },
      { icon: 'fa-solid fa-award', text: 'Puntos para tu score Luka' }
    ]
  },
  {
    id: 'modulo-futuro-1',
    label: 'Próximamente',
    descripcion: 'Nuevo módulo de análisis en desarrollo. Muy pronto tu coach financiero Luka te sorprenderá con más herramientas inteligentes.',
    icon: 'fa-solid fa-hourglass-start',
    tag: 'ANÁLISIS',
    tagColor: '#64748b',
    endpoint: '',
    filtroFecha: false,
    bentoClass: 'bento-wide',
    colorProfile: 'cyber-grey',
    proximamente: true,
    features: [
      { icon: 'fa-solid fa-hourglass-start', text: 'Nuevo análisis inteligente' },
      { icon: 'fa-solid fa-circle-nodes', text: 'Integración con Luka Brain' },
      { icon: 'fa-solid fa-microchip', text: 'Módulo en desarrollo' }
    ]
  },
  {
    id: 'modulo-futuro-2',
    label: 'Próximamente',
    descripcion: 'Nueva funcionalidad del Coach IA en fase de diseño. Estamos creando nuevas formas de optimizar tus finanzas diarias.',
    icon: 'fa-solid fa-hourglass-half',
    tag: 'COACH',
    tagColor: '#64748b',
    endpoint: '',
    filtroFecha: false,
    bentoClass: 'bento-wide',
    colorProfile: 'cyber-grey',
    proximamente: true,
    features: [
      { icon: 'fa-solid fa-hourglass-half', text: 'Nueva mentoría del Coach' },
      { icon: 'fa-solid fa-brain', text: 'Recomendaciones autónomas' },
      { icon: 'fa-solid fa-gears', text: 'Funcionalidad en diseño' }
    ]
  },
  {
    id: 'modulo-futuro-3',
    label: 'Próximamente',
    descripcion: 'Módulo inteligente adicional en planificación. Expandiendo las capacidades predictivas de nuestra red neuronal.',
    icon: 'fa-solid fa-hourglass-end',
    tag: 'ANÁLISIS',
    tagColor: '#64748b',
    endpoint: '',
    filtroFecha: false,
    bentoClass: 'bento-wide',
    colorProfile: 'cyber-grey',
    proximamente: true,
    features: [
      { icon: 'fa-solid fa-hourglass-end', text: 'Análisis predictivo futuro' },
      { icon: 'fa-solid fa-robot', text: 'Red neuronal ampliada' },
      { icon: 'fa-solid fa-screwdriver-wrench', text: 'Módulo en planificación' }
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
    IaReporteAnualComponent
  ],
  templateUrl: './ia-hub.html',
  styleUrl: './ia-hub.scss'
})
export class IaHubComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  modulos = IA_MODULOS;
  moduloSeleccionado = signal<IaModulo | null>(null);
  resultado = signal<RespuestaModuloDTO | null>(null);
  cargando = signal(false);
  errorMsg = signal<string | null>(null);

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
        } else {
          this.moduloSeleccionado.set(null);
        }
      } else {
        this.moduloSeleccionado.set(null);
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

    // Retardo simulado para recrear la animación premium de la red neuronal de Gemini Pro
    setTimeout(() => {
      try {
        const mockRes = this._generarRespuestaMock(mod, payload);
        this.resultado.set(mockRes);
        this.cargando.set(false);
      } catch (err: any) {
        this.cargando.set(false);
        this.errorMsg.set('Ocurrió un error en la simulación estática: ' + err.message);
      }
    }, 1500);
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
        prediccion_total: 1850.00,
        variacion_estimada: 7.5,
        alertas: ['Margen de seguridad ajustado', 'Fondo de reserva recomendado equivalente a tres meses de gastos'],
        promedio_historico: 1800.00,
        proyeccion_proximo_mes: 1850.00,
        pendiente: 'SUBE',
        porcentaje_variacion_mensual: 7.5,
        deficit_estimado: 0.00,
        ingreso_esperado: 2000.00,
        margen_seguridad: 7.5,
        nivel_riesgo: 'MODERADO',
        historial_meses: [1750, 1820, 1780, 1800, 1850]
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
        meta_nombre: payload?.nombre_meta || 'Laptop Gamer',
        monto_objetivo: Number(payload?.monto_objetivo) || 3500,
        aporte_mensual: Number(payload?.aporte_mensual_deseado) || 350,
        meses_estimados: 6.5,
        fecha_proyectada: 'Marzo 2027',
        viabilidad_fecha_objetivo: true,
        ahorro_faltante: 3000.00,
        escenario_alternativo: {
          aporte: (Number(payload?.aporte_mensual_deseado) || 350) + 150,
          meses: Math.ceil((Number(payload?.monto_objetivo) || 3500) / ((Number(payload?.aporte_mensual_deseado) || 350) + 150))
        },
        raw_payload: payload
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
      'clasificar-transaccion': {
        descripcion: payload?.descripcion || 'Rappi Alimentos',
        monto: Number(payload?.monto) || 45.00,
        sugerencias: [
          { categoria: 'Alimentación', confianza: 95 },
          { categoria: 'Delivery', confianza: 82 },
          { categoria: 'Ocio', confianza: 34 }
        ]
      }
    };

    const consejos: Record<string, string> = {
      'gasto-hormiga': 'Paulo, **vamos al grano**. Tus gastos en **\'Cafetería\'** han subido un **20%** este mes. Lo que ves como S/ 12.00 diarios hoy, se traduce en una fuga de **S/ 4,320.00 al año**. Con ese dinero podrías comprarte la **\'Laptop Gamer\'** que tanto quieres y aún te sobraría para los periféricos. Estás descuidando tu meta por una comodidad momentánea. Para empezar con fuerza, esta semana ponte el reto de llevar tu propio café en un termo al campus al menos tres días. Verás que ese pequeño cambio acelerará tu camino hacia esa nueva computadora y te dará la tranquilidad que necesitas para programar. ¡Deja de financiar el marketing de las grandes cadenas y empieza a financiar tu herramienta de trabajo!',
      'predecir-gastos': 'Estimado Paulo Moron, tras realizar el **análisis econométrico** de su historial transaccional, proyectamos que sus egresos para el próximo periodo ascenderán a **S/ 1,850.00**. Dado que sus ingresos mensuales se sitúan en S/ 2,000.00, su margen de maniobra es del **7.5%**, lo cual se considera un nivel de riesgo moderado ante contingencias. Le recomendamos formalmente priorizar la constitución de un fondo de reserva equivalente a tres meses de gastos. Evite comprometerse con nuevas obligaciones financieras durante el próximo trimestre para asegurar la viabilidad de su meta principal de la **\'Laptop Gamer\'** sin comprometer sus necesidades básicas.',
      'habitos-financieros': '¡Hola Paulo! He notado que tus **Sábados a las 6 PM** son el momento donde tu billetera más sufre, especialmente en **\'Restaurantes\'**. Parece que el fin de semana te invita a celebrar, ¡y eso está bien!, pero esos pequeños impulsos están frenando tu meta de la **Laptop Gamer**. <br/><br/>**Hábito Atómico:** Prueba la **\'Regla de las 48 horas\'**: si ves algo que quieres comprar un sábado, espérate al lunes. Si aún lo quieres, cómpralo. Verás cómo el 80% de esos antojos desaparecen solos. ¡Tú tienes el control!',
      'estilo-vida': `Paulo, tras analizar tus movimientos, te he bautizado como **'El Foodie Explorador'**. Tienes un paladar exigente: el 65% de tus gastos no fijos se van en descubrir nuevos sabores en restaurantes y barras de café. <br/><br/>**Valor de Salud:** Como tu perfil es gastronómico, podrías ahorrar un **15%** mensual si aprovechas los días de promociones bancarias en tus locales favoritos o si te pones un presupuesto semanal de 'salidas' fijo. Ese ahorro extra de S/ 120.00 aceleraría tu meta de la **Laptop Gamer** en casi un mes. ¡Sigue explorando, pero con estrategia!`,
      'reporte-completo': `Paulo, tu **Score LUKA es 78/100**. Has mantenido un crecimiento constante en tus ahorros desde el 1 de enero. Tu balance anual positivo de S/ 2,450.00 indica una gestión responsable, aunque detectamos un punto crítico en Marzo. Eres un 'Ahorrador Estratégico'. Tu gestión es superior al 80% de los usuarios de tu perfil. Mantén este ritmo y cerrarás el año con la solvencia necesaria para todas tus metas y estarás estrenando esa nueva laptop antes de lo previsto.`,
      'simular-meta': `¡Paulo, tu meta de la **'Laptop Gamer'** es **TOTALMENTE VIABLE** y estás más cerca de lo que crees! Con tu capacidad de ahorro actual de S/ 450.00 al mes y tu ahorro previo de S/ 500.00, en aproximadamente **6.5 meses** estarás estrenando equipo. Pero espera, he analizado tus finanzas y si logras optimizar solo un poco tus gastos de ocio, podrías subir ese aporte a S/ 550.00 y tenerla en solo 5 meses. ¡Imagina la potencia de ese procesador trabajando para ti medio año antes! Mantén el enfoque, cada sol ahorrado hoy es un frame más por segundo en tu nueva computadora. ¡Tú puedes!`,
      'reto-ahorro': '¡Misión: Operación Cocina en Casa! 🏆 Paulo, he detectado que tu \'Enemigo Final\' de esta semana son los Restaurantes. Tu misión, si decides aceptarla, es evitar comer fuera por los próximos 7 días. Si lo logras, habrás salvado **S/ 85.00** para tu fondo de la \'Laptop Gamer\'. ¿Aceptas el reto, Jugador 1?',
      'clasificar-transaccion': `🏷️ **Clasificación sugerida:** Para "${payload?.descripcion || 'Rappi Alimentos'}" de S/. ${payload?.monto || 45.00}, Luka recomienda la categoría **"Alimentación"** con 95% de confianza.`
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
          { etiqueta: 'Fijos (Vivienda/Servicios)', valor: 1200, color: '#10b981' },
          { etiqueta: 'Alimentación', valor: 450, color: '#3b82f6' },
          { etiqueta: 'Entretenimiento', valor: 150, color: '#f43f5e' },
          { etiqueta: 'Otros', valor: 50, color: '#6b7280' }
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
          { etiqueta: 'Meta Total', valor: Number(payload?.monto_objetivo) || 3500, color: '#cbd5e1' },
          { etiqueta: 'Aporte Mensual Deseado', valor: Number(payload?.aporte_mensual_deseado) || 350, color: '#22d3ee' },
          { etiqueta: 'Escenario de Ahorro Recomendado (+S/.150/mes)', valor: (Number(payload?.aporte_mensual_deseado) || 350) + 150, color: '#a855f7' }
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
      'predecir-gastos': { valor: 1850.00, etiqueta: 'Gasto Proyectado Próximo Mes', tendencia: 'ALZA', variacion_porcentaje: 7.5, unidad: 'S/.' },
      'habitos-financieros': { valor: 72, etiqueta: 'Índice de Hábitos Financieros', tendencia: 'ESTABLE', variacion_porcentaje: 0, unidad: '/100' },
      'estilo-vida': { valor: 55, etiqueta: 'Porcentaje Gasto Necesidades', tendencia: 'BAJA', variacion_porcentaje: -3.2, unidad: '%' },
      'reporte-completo': { valor: 720, etiqueta: 'Ahorro Neto este Mes', tendencia: 'ALZA', variacion_porcentaje: 25.4, unidad: 'S/.' },
      'simular-meta': { valor: Math.ceil((Number(payload?.monto_objetivo) || 3500) / (Number(payload?.aporte_mensual_deseado) || 350)), etiqueta: 'Meses Requeridos para la Meta', tendencia: 'ESTABLE', variacion_porcentaje: 0, unidad: 'meses' },
      'reto-ahorro': { valor: 85.00, etiqueta: 'Ahorro Estimado Total', tendencia: 'ALZA', variacion_porcentaje: 100, unidad: 'S/.' },
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
