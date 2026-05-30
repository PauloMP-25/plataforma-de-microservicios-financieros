import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-consejo',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './meta-consejo.html',
  styleUrl: './meta-consejo.scss'
})
export class MetaConsejoComponent {
  @Input() esViableSimulado: boolean = false;
  @Input() aporteMensualBase: number = 0;
  @Input() ahorroAdicional: number = 0;
  @Input() mesesProyectados: number = 0;
  @Input() mesesDeseados: number = 0;
}
