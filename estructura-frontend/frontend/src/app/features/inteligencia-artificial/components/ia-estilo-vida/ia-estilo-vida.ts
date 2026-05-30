import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';

export type ClusterType = 'FOODIE' | 'DIGITAL' | 'WELLNESS' | 'EXPLORER' | 'MINIMALISTA';

interface ClusterInfo {
  id: ClusterType;
  emoji: string;
  nombre: string;
  nombreDisplay: string;
  color: string;
  rasgos: string[];
  descripcion: string;
  porcentajePredeterminado: number;
  categorias: string[];
}

@Component({
  selector: 'app-ia-estilo-vida',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-estilo-vida.html',
  styleUrl: './ia-estilo-vida.scss'
})
export class IaEstiloVidaComponent implements OnInit, OnChanges {
  @Input() resultado: RespuestaModuloDTO | null = null;
  @Input() cargando = false;

  // Cluster seleccionado interactivamente (morfing)
  clusterSeleccionado = signal<ClusterType>('FOODIE');

  // Controladores de estado visual
  mostrarCompartir = signal(false);
  tarjetaVolteada = signal(false);

  // Diccionario de clusters con sus rasgos, colores y descripciones
  clustersInfo: Record<ClusterType, ClusterInfo> = {
    FOODIE: {
      id: 'FOODIE',
      emoji: '🥑',
      nombre: 'El Foodie Explorador',
      nombreDisplay: 'FOODIE',
      color: '#f59e0b', // Amber
      rasgos: ['Gastrónomo', 'Social', 'Delivery Lover'],
      descripcion: 'Paulo, tienes un paladar exigente. El 65% de tus gastos variables se concentran en restaurantes y cafeterías. Inviertes en memorias gastronómicas, pero podrías ahorrar S/ 120.00 al mes si aprovechas promociones bancarias.',
      porcentajePredeterminado: 65,
      categorias: ['Restaurantes', 'Cafeterías', 'Delivery', 'Supermercados']
    },
    DIGITAL: {
      id: 'DIGITAL',
      emoji: '💻',
      nombre: 'El Techie Digital',
      nombreDisplay: 'DIGITAL',
      color: '#22d3ee', // Cyan
      rasgos: ['Gadgets', 'Streaming', 'Automatizado'],
      descripcion: 'Te encanta el software premium, los servicios en la nube y las suscripciones que optimizan tu productividad. Mantén un ojo en las suscripciones inactivas para evitar pérdidas silenciosas.',
      porcentajePredeterminado: 48,
      categorias: ['Software', 'Streaming', 'Videojuegos', 'Telefonía']
    },
    WELLNESS: {
      id: 'WELLNESS',
      emoji: '🌿',
      nombre: 'Zen Consciente',
      nombreDisplay: 'WELLNESS',
      color: '#10b981', // Emerald
      rasgos: ['Deportes', 'Eco-Consciente', 'Frugal'],
      descripcion: 'Inviertes fuertemente en tu bienestar físico y mental. Tus prioridades son membresías de gimnasio, nutrición y salud preventiva, reflejando finanzas estables y planificadas.',
      porcentajePredeterminado: 55,
      categorias: ['Gimnasio', 'Salud', 'Suplementos', 'Orgánicos']
    },
    EXPLORER: {
      id: 'EXPLORER',
      emoji: '✈️',
      nombre: 'Aventurero del Mundo',
      nombreDisplay: 'EXPLORER',
      color: '#a855f7', // Purple
      rasgos: ['Viajero', 'Eventos', 'Coleccionista de Memorias'],
      descripcion: 'Prefieres atesorar recuerdos y vivencias antes que acumular cosas materiales. Tu dinero fluye hacia boletos de avión, conciertos y escapadas de fin de semana.',
      porcentajePredeterminado: 60,
      categorias: ['Vuelos', 'Hospedaje', 'Conciertos', 'Transporte']
    },
    MINIMALISTA: {
      id: 'MINIMALISTA',
      emoji: '🧘',
      nombre: 'Esencialista Puro',
      nombreDisplay: 'MINIMALISTA',
      color: '#94a3b8', // Slate
      rasgos: ['Simple', 'Ahorrador', 'Compra Consciente'],
      descripcion: 'Valoras el espacio, el tiempo y la libertad financiera por encima del consumo. Tus compras son altamente filtradas y posees el mayor índice de ahorro de la comunidad Luka.',
      porcentajePredeterminado: 75,
      categorias: ['Ahorros', 'Inversiones', 'Esenciales', 'Seguros']
    }
  };

  // Listado ordenado de clusters para la visualización del collage
  clustersList: ClusterType[] = ['FOODIE', 'DIGITAL', 'WELLNESS', 'EXPLORER', 'MINIMALISTA'];

  // Obtiene los datos del cluster seleccionado
  currentCluster = computed(() => {
    return this.clustersInfo[this.clusterSeleccionado()];
  });

  ngOnInit() {
    this.detectarClusterDominante();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resultado'] && this.resultado) {
      this.detectarClusterDominante();
    }
  }

  private detectarClusterDominante() {
    if (!this.resultado) return;
    const dominant = (this.resultado.insight?.cluster_dominante || 'FOODIE').toUpperCase() as ClusterType;
    if (this.clustersList.includes(dominant)) {
      this.clusterSeleccionado.set(dominant);
    }
  }

  seleccionarCluster(cluster: ClusterType) {
    this.clusterSeleccionado.set(cluster);
    this.tarjetaVolteada.set(false); // Resetear flip al cambiar
  }

  toggleFlip() {
    this.tarjetaVolteada.update(prev => !prev);
  }

  toggleCompartir() {
    this.mostrarCompartir.update(prev => !prev);
  }
}
