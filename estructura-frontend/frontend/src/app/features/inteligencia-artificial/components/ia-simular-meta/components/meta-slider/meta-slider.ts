import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-slider',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './meta-slider.html',
  styleUrl: './meta-slider.scss'
})
export class MetaSliderComponent {
  @Input() aporteMensualBase: number = 0;
  @Input() ahorroAdicional: number = 0;
  @Output() sliderChange = new EventEmitter<Event>();

  onSliderChange(event: Event) {
    this.sliderChange.emit(event);
  }
}
