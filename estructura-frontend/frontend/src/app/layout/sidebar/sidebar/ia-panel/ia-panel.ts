// =============================================
// LUKA — IA Panel Component (Standalone)
// Módulo de Inteligencia Artificial en el Sidebar
// NO modifica ningún archivo existente del equipo.
// =============================================
import { Component, Input, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

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

const IA_MODULOS: IaModulo[] = [
  // ─── Grupo ANÁLISIS ──────────────────────────────────
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
  // ─── Grupo COACH ─────────────────────────────────────
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
  // ─── Grupo CLASIFICACIÓN ─────────────────────────────
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

type TabGroup = 'TODOS' | 'ANÁLISIS' | 'COACH' | 'CLASIFICACIÓN';

@Component({
  selector:    'app-ia-panel',
  standalone:  true,
  imports:     [CommonModule, RouterModule],
  templateUrl: './ia-panel.html',
  styleUrl:    './ia-panel.scss',
})
export class IaPanelComponent {
  @Input() expanded = true;   // recibe si el sidebar está expandido

  private router = inject(Router);
  IA_MODULOS_COUNT = 8;

  // ── Estado del panel ──────────────────────────────────────────────────
  panelAbierto   = signal(false);
  moduloActivo   = signal<IaModulo | null>(null);
  modalAbierto   = signal(false);
  tabActiva      = signal<TabGroup>('TODOS');
  fechaInicio    = signal('');
  fechaFin       = signal('');
  cargando       = signal(false);
  resultadoMock  = signal('');

  // ── Tabs ──────────────────────────────────────────────────────────────
  tabs: TabGroup[] = ['TODOS', 'ANÁLISIS', 'COACH', 'CLASIFICACIÓN'];

  tabIcons: Record<TabGroup, string> = {
    'TODOS':          'fa-solid fa-layer-group',
    'ANÁLISIS':       'fa-solid fa-magnifying-glass-chart',
    'COACH':          'fa-solid fa-graduation-cap',
    'CLASIFICACIÓN':  'fa-solid fa-tags',
  };

  // ── Módulos filtrados ─────────────────────────────────────────────────
  modulosFiltrados = computed(() => {
    const tab = this.tabActiva();
    return tab === 'TODOS' ? IA_MODULOS : IA_MODULOS.filter(m => m.tag === tab);
  });

  // Conteo por grupo
  conteoPor = (tag: string) =>
    tag === 'TODOS' ? IA_MODULOS.length : IA_MODULOS.filter(m => m.tag === tag).length;

  // ── Acciones ──────────────────────────────────────────────────────────
  togglePanel(): void {
    this.panelAbierto.update(v => !v);
  }

  abrirModulo(modulo: IaModulo): void {
    // Cerrar panel del sidebar al navegar
    this.panelAbierto.set(false);
    // Redirigir a la página de IA con el módulo activo
    this.router.navigate(['/inteligencia-artificial'], { queryParams: { modulo: modulo.id } });
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.moduloActivo.set(null);
  }

  cambiarTab(tab: TabGroup): void {
    this.tabActiva.set(tab);
  }

  // ── Simular consulta (estático por ahora) ─────────────────────────────
  ejecutarConsulta(): void {
    this.cargando.set(true);
    this.resultadoMock.set('');
    // Simulación de respuesta de Gemini (estático por ahora)
    setTimeout(() => {
      this.cargando.set(false);
      this.resultadoMock.set(this._generarRespuestaMock(this.moduloActivo()!));
    }, 1800);
  }

  private _generarRespuestaMock(modulo: IaModulo): string {
    const respuestas: Record<string, string> = {
      'gasto-hormiga':        '🐜 **Detectamos 8 gastos hormiga** en los últimos 30 días. El café diario (S/. 7 × 22 días = **S/. 154**) es tu mayor fuga invisible. Redirige ese monto a tu meta de emergencia.',
      'predecir-gastos':      '📈 **Proyección del próximo mes:** S/. 2,340. Tus gastos en entretenimiento crecen +18% vs. promedio. Considera establecer un límite de S/. 200 en esa categoría.',
      'habitos-financieros':  '🧠 **Puntuación de hábitos: 72/100 (Bueno)**. Fortaleza: constancia en registro de gastos. Área de mejora: el 34% de tus compras son impulsivas los viernes.',
      'estilo-vida':          '🌿 **Tu perfil:** "Explorador Consciente". Gastas bien en experiencias pero descuidas el ahorro estructurado. Recomendamos el método 50/30/20.',
      'reporte-completo':     '📊 **Reporte Ejecutivo — Mayo 2026**: Ingresos S/. 3,200 | Gastos S/. 2,480 | Ahorro neto: S/. 720 (22.5%). ✅ Meta de ahorro alcanzada. 3 alertas de gasto hormiga activas.',
      'simular-meta':         '🎯 **Meta: Laptop gamer S/. 3,500**. Con tu aporte de S/. 350/mes la logras en **10 meses** (Mar 2027). Si ahorras S/. 500/mes, la alcanzas en 7 meses.',
      'reto-ahorro':          '🏆 **Reto 30 días activo**: Semana 1: no-cafeterías. Semana 2: cocina en casa 4 días. Semana 3: revisar suscripciones. Semana 4: reto de los S/. 5 diarios.',
      'clasificar-transaccion':'🏷️ **Sugerencias de categoría**: "RAPPI S/. 45" → 🍔 Alimentación (92% confianza) | "Netflix S/. 35" → 🎬 Suscripciones (99%) | "UBER S/. 22" → 🚗 Transporte (88%)',
    };
    return respuestas[modulo.id] ?? '✅ Análisis completado exitosamente por Gemini Pro.';
  }
}
