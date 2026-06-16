import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-kpi',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styleUrl: './meta-kpi.component.scss',
  host: {
    'class': 'metas-page__kpi-card card',
  },
  template: `
    <div class="metas-page__kpi-icon" [class]="iconClass()">
      <i [class]="icon()"></i>
    </div>
    <div class="metas-page__kpi-info">
      <span class="metas-page__kpi-title">{{ title() }}</span>
      <span class="metas-page__kpi-value">{{ value() }}</span>
      <span class="metas-page__kpi-sub">{{ sub() }}</span>
    </div>
  `
})
export class MetaKpiComponent {
  title = input.required<string>();
  value = input.required<string | number>();
  sub = input<string>('');
  icon = input.required<string>();
  iconClass = input<string>('');
}
