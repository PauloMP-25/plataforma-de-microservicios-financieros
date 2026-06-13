import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/ia_coach/ia-base.model';
import { ConsejoEstiloVidaDTO } from '../../../../core/models/ia_coach/ia-estilo-vida.model';
import { ClusterType, CLUSTERS_LIST, CLUSTERS_INFO } from './ia-estilo-vida.constants';

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

  // Estado central
  clusterSeleccionado = signal<ClusterType>('FOODIE');
  oraculoHablando = signal<boolean>(false);

  // Computado del cluster actual
  currentCluster = computed(() => {
    return CLUSTERS_INFO[this.clusterSeleccionado()];
  });

  // Datos extraídos del backend
  montoAnalizado = computed(() => {
    return this.resultado?.insight?.total_analizado || '1250.00';
  });

  consejoBackend = computed((): ConsejoEstiloVidaDTO | null => {
    if (this.resultado?.consejo) {
      if (typeof this.resultado.consejo === 'string') {
         try {
           return JSON.parse(this.resultado.consejo) as ConsejoEstiloVidaDTO;
         } catch {
           return null;
         }
      }
      return this.resultado.consejo as ConsejoEstiloVidaDTO;
    }
    return null;
  });

  ngOnInit() {
    this.detectarClusterDominante();
    this.simularCoachHablando();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['resultado'] && this.resultado) {
      this.detectarClusterDominante();
      this.simularCoachHablando();
    }
  }

  private detectarClusterDominante() {
    if (!this.resultado) return;
    const dominant = (this.resultado.insight?.cluster_dominante || 'FOODIE').toUpperCase() as ClusterType;
    if (CLUSTERS_LIST.includes(dominant)) {
      this.clusterSeleccionado.set(dominant);
    }
  }

  actualizarCluster(nuevoCluster: ClusterType) {
    this.clusterSeleccionado.set(nuevoCluster);
    this.simularCoachHablando();
  }

  private simularCoachHablando() {
    this.oraculoHablando.set(true);
    setTimeout(() => {
      this.oraculoHablando.set(false);
    }, 4500);
  }
}
