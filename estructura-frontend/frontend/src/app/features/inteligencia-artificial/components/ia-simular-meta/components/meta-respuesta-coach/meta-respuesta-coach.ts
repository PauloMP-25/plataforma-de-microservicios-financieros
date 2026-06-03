import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-respuesta-coach',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './meta-respuesta-coach.html',
  styleUrl: './meta-respuesta-coach.scss'
})
export class MetaRespuestaCoachComponent {
  @Input() consejo: string | null | undefined = '';
}
