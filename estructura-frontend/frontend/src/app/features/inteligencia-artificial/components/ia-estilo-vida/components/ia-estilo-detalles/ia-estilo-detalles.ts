import { Component, Input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterType, CLUSTERS_INFO } from '../../ia-estilo-vida.constants';

@Component({
  selector: 'app-ia-estilo-detalles',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-estilo-detalles.html',
  styleUrl: './ia-estilo-detalles.scss'
})
export class IaEstiloDetallesComponent {
  @Input() clusterSeleccionado: ClusterType = 'FOODIE';
  @Input() montoAnalizado: string | number = '0.00';

  get currentCluster() {
    return CLUSTERS_INFO[this.clusterSeleccionado];
  }
}
