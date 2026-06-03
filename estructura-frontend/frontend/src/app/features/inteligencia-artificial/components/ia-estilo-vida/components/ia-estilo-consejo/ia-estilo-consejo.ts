import { Component, Input, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterType, CLUSTERS_INFO } from '../../ia-estilo-vida.constants';

@Component({
  selector: 'app-ia-estilo-consejo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-estilo-consejo.html',
  styleUrl: './ia-estilo-consejo.scss'
})
export class IaEstiloConsejoComponent implements OnChanges {
  @Input() clusterSeleccionado: ClusterType = 'FOODIE';
  @Input() consejoBackend: string | null = null;
  @Input() oraculoHablando = false;

  get currentCluster() {
    return CLUSTERS_INFO[this.clusterSeleccionado];
  }

  textoMostrado = signal('');
  
  private typeWriterInterval: any;

  ngOnChanges(changes: SimpleChanges) {
    if (changes['consejoBackend']) {
      this.iniciarEfectoTypewriter();
    }
  }

  private iniciarEfectoTypewriter() {
    if (this.typeWriterInterval) {
      clearInterval(this.typeWriterInterval);
    }
    
    this.textoMostrado.set('');
    
    const cluster = this.currentCluster;
    const textoCompleto = this.consejoBackend 
      ? this.consejoBackend 
      : cluster.descripcion;
      
    let index = 0;
    const isMarkdownBold = false; // Simplified version without full markdown parser for now

    this.typeWriterInterval = setInterval(() => {
      if (index < textoCompleto.length) {
        let char = textoCompleto.charAt(index);
        
        // Skip basic markdown asterisks for visual clean output
        if (char === '*') {
          index++;
          return;
        }

        this.textoMostrado.update(v => v + char);
        index++;
      } else {
        clearInterval(this.typeWriterInterval);
      }
    }, 30); // 30ms per character speed
  }
}
