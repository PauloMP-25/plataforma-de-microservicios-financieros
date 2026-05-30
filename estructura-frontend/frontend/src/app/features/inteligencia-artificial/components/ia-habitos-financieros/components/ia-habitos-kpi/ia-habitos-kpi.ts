import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ia-habitos-kpi',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-habitos-kpi.html',
  styleUrl: './ia-habitos-kpi.scss',
})
export class IaHabitosKpiComponent {
  @Input() puntuacionHabito: number = 72;
  @Input() esSaludable: boolean = false;
  @Input() diaMayorGasto: string = 'Sábado';
  @Input() categoriaMasFrecuente: string = 'Restaurantes';
  @Input() totalTransacciones: number = 18;
}
