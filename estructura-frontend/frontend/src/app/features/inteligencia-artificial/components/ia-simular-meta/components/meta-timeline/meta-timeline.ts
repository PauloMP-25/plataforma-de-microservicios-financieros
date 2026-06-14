import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './meta-timeline.html',
  styleUrl: './meta-timeline.scss'
})
export class MetaTimelineComponent {
  @Input() esViableSimulado: boolean = false;
  @Input() porcentajeLograble: number = 0;
  @Input() ahorroPrevio: number = 0;
  @Input() montoObjetivo: number = 0;
  @Input() blurAmount: number = 0;
}
