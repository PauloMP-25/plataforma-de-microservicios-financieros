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
  @Input() consejo: any = null;

  get consejoFormateado(): string {
    if (!this.consejo) return '';
    if (typeof this.consejo === 'object') {
      return `
        <p>${this.consejo.introduccion}</p>
        <p><strong>Diagnóstico:</strong> ${this.consejo.diagnostico_viabilidad}</p>
        <p><strong>Plan de Acción:</strong> ${this.consejo.plan_accion}</p>
        ${this.consejo.tecnica_sugerida ? `<p class="highlight-tip">💡 <strong>Técnica Sugerida:</strong> ${this.consejo.tecnica_sugerida}</p>` : ''}
        <p><em>${this.consejo.mensaje_motivacional}</em></p>
      `;
    }
    return this.consejo;
  }
}
