import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IaModulo } from '../../ia-hub';

@Component({
  selector: 'app-ia-hub-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ia-hub-header.html',
  styleUrls: ['./ia-hub-header.scss']
})
export class IaHubHeaderComponent {
  @Input() moduloActivo: IaModulo | null = null;
}
