import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IaModulo } from '../../pages/ia-hub/ia-hub';

@Component({
  selector: 'app-ia-modulo-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-modulo-card.html',
  styleUrl: './ia-modulo-card.scss'
})
export class IaModuloCardComponent {
  @Input() modulo!: IaModulo;
  @Input() activo = false;
  @Output() seleccionar = new EventEmitter<void>();
}
