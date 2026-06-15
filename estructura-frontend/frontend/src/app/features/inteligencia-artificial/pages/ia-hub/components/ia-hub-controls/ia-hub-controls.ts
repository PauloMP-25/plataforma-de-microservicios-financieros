import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TabGroup } from '../../ia-hub';

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

  get quotaPercentage(): number {
    const parts = this.consultasRestantes.split('/');
    if (parts.length === 2) {
      const current = parseInt(parts[0], 10);
      const max = parseInt(parts[1], 10);
      if (max > 0) {
        return (current / max) * 100;
      }
    }
    return 100;
  }

  getCuotaText(tab: TabGroup): string {
    return '';
  }
}
