import { Component, OnInit, signal, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RespuestaModuloDTO, ConsejoEstructuradoEntrenamiento } from '../../../../core/models/ia_coach/ia-base.model';

import { IaZonaAvatarComponent } from './components/ia-zona-avatar/ia-zona-avatar';
import { IaZonaScoreBarComponent } from './components/ia-zona-score-bar/ia-zona-score-bar';
import { IaZonaVitalesComponent } from './components/ia-zona-vitales/ia-zona-vitales';
import { IaZonaRutinaComponent } from './components/ia-zona-rutina/ia-zona-rutina';

export interface MetricasSignosVitales {
  frecuenciaCardiaca: { valor: number, estado: 'normal' | 'alerta' | 'critico' };
  presionArterial: { valor: number, estado: 'normal' | 'alerta' | 'critico' };
  temperaturaAhorro: { valor: number, estado: 'normal' | 'alerta' | 'critico' };
  saturacionCategorias: { valor: number, estado: 'normal' | 'alerta' | 'critico' };
}

export interface RutinaEjercicio {
  id: string;
  nombre: string;
  descripcion: string;
  series: number;
  repeticiones: number;
  musculoTrabajado: string;
  metricaExito: string;
  completado: boolean;
}

export type PerfilAtleta = 'Atleta de Élite' | 'En Forma' | 'Sedentario' | 'Lesionado' | 'UCI Financiera';

export interface EstadoAtleta {
  perfil: PerfilAtleta;
  scoreLuka: number;
}

@Component({
  selector: 'app-ia-zona-entrenamiento',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    IaZonaAvatarComponent,
    IaZonaScoreBarComponent,
    IaZonaVitalesComponent,
    IaZonaRutinaComponent
  ],
  templateUrl: './ia-zona-entrenamiento.html',
  styleUrl: './ia-zona-entrenamiento.scss'
})
export class IaZonaEntrenamientoComponent implements OnInit, OnChanges {
  
  @Input() resultado!: RespuestaModuloDTO | null;
  @Input() cargando: boolean = false;

  entrenamientoCargado = signal<boolean>(false);

  // Datos Mockup base
  metricas = signal<MetricasSignosVitales>({
    frecuenciaCardiaca: { valor: 0, estado: 'normal' },
    presionArterial: { valor: 0, estado: 'normal' },
    temperaturaAhorro: { valor: 0, estado: 'normal' },
    saturacionCategorias: { valor: 0, estado: 'normal' }
  });

  estado = signal<EstadoAtleta>({
    perfil: 'Sedentario',
    scoreLuka: 50
  });

  rutinas = signal<RutinaEjercicio[]>([]);

  ngOnInit() {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['cargando'] && !this.cargando) {
      // terminó de cargar
    }
    if (changes['resultado'] && this.resultado) {
      this.entrenamientoCargado.set(true);
      
      const consejo = (this.resultado.consejo || {}) as ConsejoEstructuradoEntrenamiento;
      const insight = this.resultado.insight || {};

      this.estado.set({
        perfil: (consejo.estado_fisico as PerfilAtleta) || 'Sedentario',
        scoreLuka: 50 // Por ahora estático o si viene del backend
      });

      this.metricas.set({
        frecuenciaCardiaca: { valor: insight.frecuencia_cardiaca || 0, estado: ((insight.frecuencia_cardiaca || 0) > 2 ? 'alerta' : 'normal') },
        presionArterial: { valor: insight.presion_arterial || 0, estado: ((insight.presion_arterial || 0) > 1.2 ? 'critico' : 'normal') },
        temperaturaAhorro: { valor: insight.temperatura_ahorro || 0, estado: ((insight.temperatura_ahorro || 0) < 10 ? 'critico' : 'normal') },
        saturacionCategorias: { valor: insight.saturacion_pct || 0, estado: ((insight.saturacion_pct || 0) > 40 ? 'alerta' : 'normal') }
      });

      const rut = consejo.rutina || [];
      const mappedRutinas = rut.map((r, index: number) => ({
        id: `ej-${index}`,
        nombre: r.nombre,
        descripcion: r.descripcion,
        series: r.duracion_dias || 30,
        repeticiones: 1,
        musculoTrabajado: r.frecuencia || 'Diario',
        metricaExito: r.metrica_exito || 'Completar',
        completado: false
      }));

      this.rutinas.set(mappedRutinas);
    } else if (!this.resultado && !this.cargando) {
      this.entrenamientoCargado.set(false);
    }
  }

  private cargarDatosMock() {
    this.estado.set({
      perfil: 'Lesionado',
      scoreLuka: 58
    });

    this.metricas.set({
      frecuenciaCardiaca: { valor: 5, estado: 'alerta' },
      presionArterial: { valor: 70, estado: 'critico' },
      temperaturaAhorro: { valor: 15, estado: 'critico' },
      saturacionCategorias: { valor: 90, estado: 'critico' }
    });

    this.rutinas.set([
      {
        id: 'ej-1',
        nombre: 'Sprint de Ahorro',
        descripcion: 'No gastar en la categoría Entretenimiento/Antojos por 3 días seguidos.',
        series: 1,
        repeticiones: 3,
        musculoTrabajado: 'Entretenimiento/Antojos',
        metricaExito: 'S/ 30.00 retenidos',
        completado: false
      },
      {
        id: 'ej-2',
        nombre: 'Levantamiento de Presupuesto',
        descripcion: 'Registrar cada sol gastado en Pichangas antes de que termine el día.',
        series: 7,
        repeticiones: 1,
        musculoTrabajado: 'Deportes/Ocio',
        metricaExito: '100% visibilidad de gastos',
        completado: false
      },
      {
        id: 'ej-3',
        nombre: 'Flexibilidad de Cuota',
        descripcion: 'Separar S/ 6 fijos al inicio de la semana exclusivamente para las pichangas, evitando micro-gastos sorpresa.',
        series: 1,
        repeticiones: 1,
        musculoTrabajado: 'Planificación',
        metricaExito: 'Presupuesto blindado',
        completado: false
      }
    ]);
  }
}
