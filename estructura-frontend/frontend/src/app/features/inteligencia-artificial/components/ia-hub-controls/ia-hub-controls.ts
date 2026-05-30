import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabGroup } from '../../pages/ia-hub/ia-hub';

@Component({
  selector: 'app-ia-hub-controls',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-hub-controls.html',
  styleUrls: ['./ia-hub-controls.scss']
})
export class IaHubControlsComponent {
  @Input() tabs: TabGroup[] = [];
  @Input() tabActiva: TabGroup = 'TODOS';
  @Input() tabIcons: Record<TabGroup, string> = {} as any;

  // Nueva propiedad solicitada por el usuario para la cuota general
  @Input() consultasRestantes: string = '50/50';

  @Output() tabChange = new EventEmitter<TabGroup>();

  getCuotaText(tab: TabGroup): string {
    // Si la cuota ya se maneja de forma global, esto podría omitirse o quedarse vacío.
    return '';
  }
}
