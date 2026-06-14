import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterType, CLUSTERS_INFO, CLUSTERS_LIST } from '../../ia-estilo-vida.constants';

@Component({
  selector: 'app-ia-estilo-polaroids',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="polaroid-wall">
      <div class="spotlight"></div>
      
      <div 
        *ngFor="let clId of clustersList" 
        class="polaroid-card"
        [class.dominant]="clusterSeleccionado === clId"
        [class.stacked]="clusterSeleccionado !== clId"
        (click)="seleccionar(clId)"
      >
        <div class="polaroid-inner">
          <div class="polaroid-flipper">
            <div class="polaroid-front">
              <div class="polaroid-photo">
                <div class="photo-emoji">{{ clustersInfo[clId].emoji }}</div>
                <div class="photo-overlay-wash"></div>
              </div>
              <div class="polaroid-caption">
                <span class="caption-title">{{ clustersInfo[clId].nombreDisplay }}</span>
              </div>
            </div>
            <div class="polaroid-back" [style.backgroundColor]="clustersInfo[clId].color">
              <span class="back-desc">{{ clustersInfo[clId].descripcionCorta }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrl: './ia-estilo-polaroids.scss'
})
export class IaEstiloPolaroidsComponent {
  @Input() clusterSeleccionado: ClusterType = 'FOODIE';
  @Output() clusterSeleccionadoChange = new EventEmitter<ClusterType>();

  clustersInfo = CLUSTERS_INFO;
  clustersList = CLUSTERS_LIST;

  seleccionar(clId: ClusterType) {
    this.clusterSeleccionadoChange.emit(clId);
  }
}
