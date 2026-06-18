import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoriaMeta } from '../../services/metas-utility.service';

@Component({
  selector: 'app-meta-purpose-selector',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styleUrl: './meta-purpose-selector.component.scss',
  host: {
    'class': 'meta-form-page__form-group'
  },
  template: `
    <label class="form-label-purpose">
      <i class="fa-solid fa-wand-magic-sparkles"></i> Elige un Propósito
    </label>
    
    <div class="meta-form-page__purpose-grid">
      @for (cat of categorias(); track cat.id) {
        <button type="button" 
                class="purpose-btn" 
                [class.purpose-btn--active]="selectedCategory() === cat.id"
                [disabled]="disabled()"
                (click)="onSelect(cat.id)">
          <i [class]="cat.icono"></i>
          <span>{{ cat.nombre }}</span>
        </button>
      }
    </div>
  `
})
export class MetaPurposeSelectorComponent {
  categorias = input.required<CategoriaMeta[]>();
  selectedCategory = input.required<string>();
  disabled = input(false);

  select = output<string>();

  onSelect(catId: string) {
    if (this.disabled()) return;
    this.select.emit(catId);
  }
}
