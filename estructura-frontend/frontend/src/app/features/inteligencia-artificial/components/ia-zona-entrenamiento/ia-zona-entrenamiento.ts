import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
export class IaZonaEntrenamientoComponent implements OnInit {
  
  cargando = signal<boolean>(false);
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

  consultarEntrenamiento(fechaInicio: string, fechaFin: string) {
    this.cargando.set(true);
    this.entrenamientoCargado.set(false);

    // Simular carga de 1.5s
    setTimeout(() => {
      this.cargarDatosMock();
      this.cargando.set(false);
      this.entrenamientoCargado.set(true);
    }, 1500);
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
