import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-espejo-impact',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-espejo-impact.html',
  styleUrl: './ia-espejo-impact.scss'
})
export class IaEspejoImpactComponent {
  @Input() diferencia = 0;
}
