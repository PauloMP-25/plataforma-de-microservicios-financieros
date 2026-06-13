import { Component, Input, signal, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConsejoEstructuradoSimularMeta } from '../../../../../../../core/models/ia_coach/ia-base.model';

@Component({
  selector: 'app-meta-respuesta-coach',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './meta-respuesta-coach.html',
  styleUrl: './meta-respuesta-coach.scss'
})
export class MetaRespuestaCoachComponent implements OnChanges {
  @Input() consejo: ConsejoEstructuradoSimularMeta | string | null = null;
  
  consejoObj = signal<ConsejoEstructuradoSimularMeta | null>(null);
  consejoStr = signal<string>('');

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['consejo'] && this.consejo) {
      if (typeof this.consejo === 'object') {
        this.consejoObj.set(this.consejo as ConsejoEstructuradoSimularMeta);
        this.consejoStr.set('');
      } else {
        this.consejoObj.set(null);
        this.consejoStr.set(this.consejo);
      }
    }
  }
}
