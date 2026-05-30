import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { IngresoFormData } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingreso-preview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ingreso-preview.html',
})
export class IngresoPreviewComponent {
  @Input() data!: IngresoFormData;
}

