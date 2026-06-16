import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-meta-filters',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  styleUrl: './meta-filters.component.scss',
  host: {
    'class': 'metas-page__filters card anim-fade-up',
  },
  template: `
    <div class="metas-page__filters-grid">
      <!-- Filtro Estado -->
      <div class="metas-page__filter-group">
        <label for="filtroEstado">Estado</label>
        <select id="filtroEstado" [value]="filtroEstado()" (change)="estadoChange.emit($any($event.target).value)">
          <option value="Todas">Todas</option>
          <option value="Activas">Activas</option>
          <option value="Cumplidas">Cumplidas</option>
          <option value="Vencidas">Vencidas</option>
        </select>
      </div>

      <!-- Filtro Mes -->
      <div class="metas-page__filter-group">
        <label for="filtroMes">Mes</label>
        <select id="filtroMes" [value]="filtroMes()" (change)="mesChange.emit($any($event.target).value)">
          <option value="Todos">Todos</option>
          <option value="0">Enero</option>
          <option value="1">Febrero</option>
          <option value="2">Marzo</option>
          <option value="3">Abril</option>
          <option value="4">Mayo</option>
          <option value="5">Junio</option>
          <option value="6">Julio</option>
          <option value="7">Agosto</option>
          <option value="8">Septiembre</option>
          <option value="9">Octubre</option>
          <option value="10">Noviembre</option>
          <option value="11">Diciembre</option>
        </select>
      </div>

      <!-- Filtro Año -->
      <div class="metas-page__filter-group">
        <label for="filtroAnio">Año</label>
        <select id="filtroAnio" [value]="filtroAnio()" (change)="anioChange.emit($any($event.target).value)">
          <option value="Todos">Todos</option>
          @for (anio of anios(); track anio) {
            <option [value]="anio">{{ anio }}</option>
          }
        </select>
      </div>

      <!-- Rango de Monto Objetivo -->
      <div class="metas-page__filter-group">
        <label>Rango de monto objetivo (S/)</label>
        <div class="metas-page__range-inputs">
          <input type="number" placeholder="Mínimo" [value]="filtroMontoMin()" (input)="montoMinChange.emit($any($event.target).value ? +$any($event.target).value : null)">
          <span>-</span>
          <input type="number" placeholder="Máximo" [value]="filtroMontoMax()" (input)="montoMaxChange.emit($any($event.target).value ? +$any($event.target).value : null)">
        </div>
      </div>

      <!-- Botones de Acción de Filtros -->
      <div class="metas-page__filter-actions">
        <button type="button" class="btn-clear" (click)="clear.emit()">
          <i class="fa-solid fa-rotate-left"></i> Limpiar
        </button>
      </div>
    </div>
  `
})
export class MetaFiltersComponent {
  filtroEstado = input.required<string>();
  filtroMes = input.required<string>();
  filtroAnio = input.required<string>();
  filtroMontoMin = input<number | null>(null);
  filtroMontoMax = input<number | null>(null);
  anios = input.required<number[]>();

  estadoChange = output<string>();
  mesChange = output<string>();
  anioChange = output<string>();
  montoMinChange = output<number | null>();
  montoMaxChange = output<number | null>();
  clear = output<void>();
}
