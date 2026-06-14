import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RespuestaModuloDTO } from '../../../../core/models/financiero/ia.model';
import { ClusterType, CLUSTERS_LIST, CLUSTERS_INFO } from './ia-estilo-vida.constants';

// Subcomponents
import { IaEstiloPolaroidsComponent } from './components/ia-estilo-polaroids/ia-estilo-polaroids';
import { IaEstiloCarnetComponent } from './components/ia-estilo-carnet/ia-estilo-carnet';
import { IaEstiloConsejoComponent } from './components/ia-estilo-consejo/ia-estilo-consejo';
import { IaEstiloDetallesComponent } from './components/ia-estilo-detalles/ia-estilo-detalles';

@Component({
  selector: 'app-ia-estilo-vida',
  standalone: true,
  imports: [
    CommonModule, 
    IaEstiloPolaroidsComponent, 
    IaEstiloCarnetComponent, 
    IaEstiloConsejoComponent, 
    IaEstiloDetallesComponent
  ],
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

  consejoBackend = computed(() => {
    return this.resultado?.consejo || null;
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
    // Simula que deja de hablar después de que el typewriter termine (aprox basado en texto largo)
    // El texto promedio tiene ~150 chars. A 30ms por char = 4500ms
    setTimeout(() => {
      this.oraculoHablando.set(false);
    }, 5000);
  }
}
