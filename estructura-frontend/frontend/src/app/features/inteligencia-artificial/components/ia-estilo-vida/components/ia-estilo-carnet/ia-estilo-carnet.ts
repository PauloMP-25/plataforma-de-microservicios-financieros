import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterType, CLUSTERS_INFO } from '../../ia-estilo-vida.constants';

@Component({
  selector: 'app-ia-estilo-carnet',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-estilo-carnet.html',
  styleUrl: './ia-estilo-carnet.scss'
})
export class IaEstiloCarnetComponent {
  @Input() clusterSeleccionado: ClusterType = 'FOODIE';

  get currentCluster() {
    return CLUSTERS_INFO[this.clusterSeleccionado];
  }
}
