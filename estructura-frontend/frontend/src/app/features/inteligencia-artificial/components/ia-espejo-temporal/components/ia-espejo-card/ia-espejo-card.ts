import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProyeccionHitoDTO } from '../../../../../../core/models/financiero/ia.model';

@Component({
  selector: 'app-ia-espejo-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-espejo-card.html',
  styleUrl: './ia-espejo-card.scss'
})
export class IaEspejoCardComponent {
  @Input() tipo: 'continuidad' | 'transformacion' = 'continuidad';
  @Input() hitoData!: ProyeccionHitoDTO;
  @Input() cartaText = '';
}
