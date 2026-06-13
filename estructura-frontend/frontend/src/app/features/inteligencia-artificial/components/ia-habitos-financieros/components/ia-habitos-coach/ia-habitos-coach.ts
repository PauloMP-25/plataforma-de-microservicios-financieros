import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConsejoHabitosFinancierosDTO } from '../../../../../../../core/models/ia_coach/ia-habitos.model';

@Component({
  selector: 'app-ia-habitos-coach',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-habitos-coach.html',
  styleUrl: './ia-habitos-coach.scss',
})
export class IaHabitosCoachComponent {
  @Input() consejo: ConsejoHabitosFinancierosDTO | null = null;
}
