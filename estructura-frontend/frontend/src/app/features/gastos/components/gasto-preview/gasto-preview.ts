import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { GastoFormData } from '../../types/gastos.interfaces';

@Component({
  selector: 'app-gasto-preview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './gasto-preview.html',
})
export class GastoPreviewComponent {
  @Input() data!: GastoFormData;
}

