import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { IaService } from '../../../../core/services/ia.service';
import { RespuestaModuloDTO, RespuestaClasificacionDTO } from '../../../../core/models/financiero/ia.model';
import { IaModulo } from '../../../../layout/sidebar/sidebar/ia-panel/ia-panel';

import { IaModuloCardComponent } from '../../components/ia-modulo-card/ia-modulo-card';
import { IaPanelActivoComponent } from '../../components/ia-panel-activo/ia-panel-activo';
import { IaResultadoComponent } from '../../components/ia-resultado/ia-resultado';

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
    IaResultadoComponent
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

  ejecutarAnalisis(payload: any) {
    const mod = this.moduloSeleccionado();
    if (!mod) return;

    this.cargando.set(true);
    this.errorMsg.set(null);
    this.resultado.set(null);

    switch (mod.id) {
      case 'gasto-hormiga':
        this.iaService.getGastoHormiga(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'predecir-gastos':
        this.iaService.getPredecirGastos(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'habitos-financieros':
        this.iaService.getHabitosFinancieros(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'estilo-vida':
        this.iaService.getEstiloVida(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'reporte-completo':
        this.iaService.getReporteCompleto(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'simular-meta':
        this.iaService.getSimularMeta(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'reto-ahorro':
        this.iaService.getRetoAhorro(payload).subscribe({
          next: (res) => this.handleSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
      case 'clasificar-transaccion':
        this.iaService.getClasificarTransaccion(payload).subscribe({
          next: (res) => this.handleClasificacionSuccess(res.datos),
          error: (err) => this.handleError(err)
        });
        break;
    }
  }

  private handleSuccess(datos: RespuestaModuloDTO) {
    this.resultado.set(datos);
    this.cargando.set(false);
  }

  private handleClasificacionSuccess(datos: RespuestaClasificacionDTO) {
    // Convertimos RespuestaClasificacionDTO a un formato compatible con el visor de resultados
    const mockRespuesta: RespuestaModuloDTO = {
      id_respuesta: `clasif_${datos.id_temporal}`,
      usuario_id: 'luka_user',
      modulo: 'Auto-Clasificación',
      fecha_generacion: new Date().toISOString(),
      estado_coach: 'EXITOSO',
      usando_fallback: datos.usando_fallback,
      insight: {
        id_temporal: datos.id_temporal,
        sugerencias_categorias: datos.sugerencias
      },
      consejo: `El motor de inteligencia artificial de Gemini Pro ha analizado las etiquetas y notas provistas. Las categorías recomendadas en orden de relevancia son: ${datos.sugerencias.join(', ')}. Te sugerimos asignar la primera opción si coincide con tu presupuesto.`
    };
    this.resultado.set(mockRespuesta);
    this.cargando.set(false);
  }

  private handleError(err: any) {
    console.error('Error al llamar al servicio de IA:', err);
    this.errorMsg.set('Hubo un problema de red o de autenticación al comunicarse con el microservicio de IA. Verifica tu conexión e inténtalo de nuevo.');
    this.cargando.set(false);
  }
}
