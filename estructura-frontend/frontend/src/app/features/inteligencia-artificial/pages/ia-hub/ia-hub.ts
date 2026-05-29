import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { IaService } from '../../../../core/services/ia.service';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

import { IaModuloCardComponent } from '../../components/ia-modulo-card/ia-modulo-card';
import { IaPanelActivoComponent } from '../../components/ia-panel-activo/ia-panel-activo';
import { IaResultadoComponent } from '../../components/ia-resultado/ia-resultado';
import { IaGastoHormigaComponent } from '../../components/ia-gasto-hormiga/ia-gasto-hormiga';
import { IaPrediccionGastosComponent } from '../../components/ia-prediccion-gastos/ia-prediccion-gastos';


export interface IaModulo {
  id:          string;
  label:       string;
  descripcion: string;
  icon:        string;
  tag:         'ANÁLISIS' | 'COACH' | 'CLASIFICACIÓN';
  tagColor:    string;
  endpoint:    string;
  filtroFecha: boolean;
  params?:     string[];
}

export type TabGroup = 'TODOS' | 'ANÁLISIS' | 'COACH' | 'CLASIFICACIÓN';

const IA_MODULOS: IaModulo[] = [
  {
    id:          'gasto-hormiga',
    label:       'Gasto Hormiga',
    descripcion: 'Detecta pequeños gastos recurrentes que erosionan tu presupuesto.',
    icon:        'fa-solid fa-bug',
    tag:         'ANÁLISIS',
    tagColor:    '#f59e0b',
    endpoint:    '/api/v1/ia/gasto-hormiga',
    filtroFecha: true,
  },
  {
    id:          'predecir-gastos',
    label:       'Predicción de Gastos',
    descripcion: 'Proyecta tus gastos futuros con base en tus patrones históricos.',
    icon:        'fa-solid fa-chart-line',
    tag:         'ANÁLISIS',
    tagColor:    '#f59e0b',
    endpoint:    '/api/v1/ia/predecir-gastos',
    filtroFecha: true,
  },
  {
    id:          'habitos-financieros',
    label:       'Hábitos Financieros',
    descripcion: 'Evalúa la calidad de tus hábitos de ahorro y gasto.',
    icon:        'fa-solid fa-brain',
    tag:         'ANÁLISIS',
    tagColor:    '#f59e0b',
    endpoint:    '/api/v1/ia/habitos-financieros',
    filtroFecha: true,
  },
  {
    id:          'estilo-vida',
    label:       'Estilo de Vida',
    descripcion: 'Analiza si tus finanzas reflejan el estilo de vida que deseas.',
    icon:        'fa-solid fa-person-walking',
    tag:         'ANÁLISIS',
    tagColor:    '#f59e0b',
    endpoint:    '/api/v1/ia/estilo-vida',
    filtroFecha: true,
  },
  {
    id:          'reporte-completo',
    label:       'Reporte Ejecutivo',
    descripcion: 'Informe 360° que combina todos los módulos de análisis.',
    icon:        'fa-solid fa-file-chart-column',
    tag:         'ANÁLISIS',
    tagColor:    '#f59e0b',
    endpoint:    '/api/v1/ia/reporte-completo',
    filtroFecha: true,
  },
  {
    id:          'simular-meta',
    label:       'Simular Meta',
    descripcion: 'Simula cuánto tiempo necesitas para alcanzar una meta de ahorro.',
    icon:        'fa-solid fa-bullseye-arrow',
    tag:         'COACH',
    tagColor:    '#10b981',
    endpoint:    '/api/v1/ia/simular-meta',
    filtroFecha: false,
    params:      ['nombre_meta', 'monto_objetivo', 'aporte_mensual_deseado'],
  },
  {
    id:          'reto-ahorro',
    label:       'Reto de Ahorro',
    descripcion: 'Genera un plan de reto personalizado para mejorar tu ahorro.',
    icon:        'fa-solid fa-trophy',
    tag:         'COACH',
    tagColor:    '#10b981',
    endpoint:    '/api/v1/ia/reto-ahorro',
    filtroFecha: true,
  },
  {
    id:          'clasificar-transaccion',
    label:       'Auto-Clasificar',
    descripcion: 'Sugiere categorías para tus transacciones usando IA Gemini.',
    icon:        'fa-solid fa-tags',
    tag:         'CLASIFICACIÓN',
    tagColor:    '#8b5cf6',
    endpoint:    '/api/v1/ia/clasificar-transaccion',
    filtroFecha: false,
    params:      ['descripcion', 'monto'],
  },
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
    IaGastoHormigaComponent,
    IaPrediccionGastosComponent
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

  // ── Filtros del Panel Principal ──
  tabActiva = signal<TabGroup>('TODOS');
  tabs: TabGroup[] = ['TODOS', 'ANÁLISIS', 'COACH', 'CLASIFICACIÓN'];
  tabIcons: Record<TabGroup, string> = {
    'TODOS':          'fa-solid fa-layer-group',
    'ANÁLISIS':       'fa-solid fa-magnifying-glass-chart',
    'COACH':          'fa-solid fa-graduation-cap',
    'CLASIFICACIÓN':  'fa-solid fa-tags',
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

  seleccionarModulo(modulo: IaModulo) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { modulo: modulo.id },
      queryParamsHandling: 'merge'
    });
  }

  deseleccionarModulo() {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { modulo: null },
      queryParamsHandling: 'merge'
    });
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
        total_gastos_hormiga: 184.00,
        proyeccion_fuga_anual: 2208.00,
        ingreso_mensual_referencia: 3200.00,
        variacion_vs_mes_anterior: 14.2,
        gastos_detectados: [
          { descripcion: 'Café Espresso diario', frecuencia: '22 veces/mes', total: 154.00, categoria: 'Cafetería', promedio_por_compra: 7.00, dia_mayor_gasto: 'Lunes' },
          { descripcion: 'Suscripción inactiva App', frecuencia: '1 vez/mes', total: 30.00, categoria: 'Suscripciones', promedio_por_compra: 30.00, dia_mayor_gasto: 'N/A' }
        ]
      },
      'predecir-gastos': {
        prediccion_total: 2340.00,
        variacion_estimada: 12.5,
        alertas: ['Incremento en entretenimiento detectado para los fines de semana', 'Gasto proyectado en servicios básicos estable'],
        promedio_historico: 2100.00,
        proyeccion_proximo_mes: 2340.00,
        pendiente: 'SUBE',
        porcentaje_variacion_mensual: 12.5,
        deficit_estimado: 140.00,
        historial_meses: [1950, 2050, 2010, 2200, 2100]
      },
      'habitos-financieros': {
        puntuacion_habito: 72,
        nivel: 'Bueno',
        fortalezas: ['Constancia en registro de transacciones diarios', 'Presupuesto de alimentación respetado'],
        oportunidades: ['El 34% de tus consumos no planificados ocurren los días viernes por la noche']
      },
      'estilo-vida': {
        perfil: 'Explorador Consciente',
        resumen: 'Tus finanzas reflejan gastos enfocados en experiencias y viajes. Sin embargo, descuidas el ahorro a largo plazo.',
        porcentajes_actuales: { necesidades: 55, deseos: 35, ahorro: 10 }
      },
      'reporte-completo': {
        resumen_ejecutivo: 'Reporte Financiero 360° para el periodo seleccionado.',
        ingresos_totales: 3200.00,
        gastos_totales: 2480.00,
        ahorro_neto: 720.00,
        tasa_ahorro: 22.5,
        indicadores: { salud: 'BUENA', alertas: 3 }
      },
      'simular-meta': {
        meta_nombre: payload?.nombre_meta || 'Laptop Gamer',
        monto_objetivo: Number(payload?.monto_objetivo) || 3500,
        aporte_mensual: Number(payload?.aporte_mensual_deseado) || 350,
        meses_estimados: Math.ceil((Number(payload?.monto_objetivo) || 3500) / (Number(payload?.aporte_mensual_deseado) || 350)),
        fecha_proyectada: 'Marzo 2027',
        escenario_alternativo: {
          aporte: (Number(payload?.aporte_mensual_deseado) || 350) + 150,
          meses: Math.ceil((Number(payload?.monto_objetivo) || 3500) / ((Number(payload?.aporte_mensual_deseado) || 350) + 150))
        }
      },
      'reto-ahorro': {
        duracion_dias: 30,
        dificultad: 'Media',
        plan_semanal: [
          { semana: 1, reto: 'No-cafeterías ni compras en tiendas de conveniencia', ahorro_estimado: 50 },
          { semana: 2, reto: 'Cocinar en casa de lunes a jueves', ahorro_estimado: 70 },
          { semana: 3, reto: 'Auditar y cancelar una suscripción inactiva', ahorro_estimado: 30 },
          { semana: 4, reto: 'Ahorro diario progresivo: desde S/. 1 hasta S/. 7', ahorro_estimado: 28 }
        ]
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
      'gasto-hormiga': '🐜 **Gastos hormiga bajo la lupa:** El café diario y las suscripciones que no usas drenan S/. 184 al mes. Redirigiendo esto a tu fondo de emergencia, acumularías S/. 2,208 en un año. ¡Pruébalo!',
      'predecir-gastos': '📈 **Predicción de gastos:** Proyectamos gastos de S/. 2,340 para el próximo mes. El entretenimiento tiende a subir en fines de semana; fijar un límite de S/. 200 evitaría desvíos.',
      'habitos-financieros': '🧠 **Evaluación de hábitos:** Calificación de 72/100. Tu fortaleza es el registro diario de compras. Para subir la nota, automatiza tu ahorro apenas recibas tu sueldo.',
      'estilo-vida': '🌿 **Perfil de Estilo de Vida:** "Explorador Consciente". Inviertes en memorias pero ahorras poco. Te aconsejamos ajustar al modelo 50% necesidades, 30% deseos y 20% ahorro.',
      'reporte-completo': '📊 **Reporte 360° Ejecutivo:** Balance positivo de S/. 720. Vas por buen camino, pero mantente alerta a la categoría de alimentación fuera del hogar que creció un 15% este mes.',
      'simular-meta': `🎯 **Simulador de Metas:** Para tu meta de **"${payload?.nombre_meta || 'Laptop Gamer'}"** (S/. ${payload?.monto_objetivo || 3500}), aportando S/. ${payload?.aporte_mensual_deseado || 350} al mes, lo lograrás en **${Math.ceil((payload?.monto_objetivo || 3500) / (payload?.aporte_mensual_deseado || 350))} meses** (Marzo 2027).`,
      'reto-ahorro': '🏆 **Reto Luka Ahorro:** Te retamos a un plan de 30 días dividido en fases semanales. Si cumples los objetivos semanales, obtendrás un ahorro estimado de **S/. 178**.',
      'clasificar-transaccion': `🏷️ **Clasificación sugerida:** Para "${payload?.descripcion || 'Rappi Alimentos'}" de S/. ${payload?.monto || 45.00}, Gemini Pro recomienda la categoría **"Alimentación"** con 95% de confianza.`
    };

    // Estructuras de gráficos para visualizar en IaResultadoComponent
    const graficos: Record<string, any> = {
      'gasto-hormiga': {
        titulo: 'Fuga Mensual por Categoría',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Cafeterías', valor: 154, color: '#f59e0b' },
          { etiqueta: 'Aplicaciones', valor: 30, color: '#a855f7' }
        ]
      },
      'predecir-gastos': {
        titulo: 'Proyección de Gastos por Categoría',
        tipoGrafico: 'BARRAS',
        unidad: 'S/.',
        datos: [
          { etiqueta: 'Fijos (Vivienda/Servicios)', valor: 1200, color: '#10b981' },
          { etiqueta: 'Alimentación', valor: 550, color: '#3b82f6' },
          { etiqueta: 'Entretenimiento', valor: 350, color: '#f43f5e' },
          { etiqueta: 'Otros', valor: 240, color: '#6b7280' }
        ]
      },
      'habitos-financieros': {
        titulo: 'Desglose de Puntuación de Hábitos',
        tipoGrafico: 'BARRAS',
        unidad: '%',
        datos: [
          { etiqueta: 'Consistencia de Registro', valor: 95, color: '#10b981' },
          { etiqueta: 'Control de Presupuestos', valor: 78, color: '#3b82f6' },
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
      'gasto-hormiga': { valor: 184, etiqueta: 'Fuga total estimada', tendencia: 'ALZA', variacion_porcentaje: 14.2, unidad: 'S/.' },
      'predecir-gastos': { valor: 2340, etiqueta: 'Gasto Proyectado Próximo Mes', tendencia: 'ALZA', variacion_porcentaje: 8.5, unidad: 'S/.' },
      'habitos-financieros': { valor: 72, etiqueta: 'Índice de Hábitos Financieros', tendencia: 'ESTABLE', variacion_porcentaje: 0, unidad: '/100' },
      'estilo-vida': { valor: 55, etiqueta: 'Porcentaje Gasto Necesidades', tendencia: 'BAJA', variacion_porcentaje: -3.2, unidad: '%' },
      'reporte-completo': { valor: 720, etiqueta: 'Ahorro Neto este Mes', tendencia: 'ALZA', variacion_porcentaje: 25.4, unidad: 'S/.' },
      'simular-meta': { valor: Math.ceil((Number(payload?.monto_objetivo) || 3500) / (Number(payload?.aporte_mensual_deseado) || 350)), etiqueta: 'Meses Requeridos para la Meta', tendencia: 'ESTABLE', variacion_porcentaje: 0, unidad: 'meses' },
      'reto-ahorro': { valor: 178, etiqueta: 'Ahorro Estimado Total', tendencia: 'ALZA', variacion_porcentaje: 100, unidad: 'S/.' },
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
      consejo: consejos[modulo.id] || '✅ Simulación completada exitosamente en el entorno de pruebas.',
      grafico: graficos[modulo.id],
      kpi: kpis[modulo.id]
    };
  }
}
