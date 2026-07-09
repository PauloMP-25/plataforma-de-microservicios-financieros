import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GastoFormData, OptionItem } from '../../types/gastos.interfaces';
import { CategoriaSugerida } from '../../../../core/models/ia_coach/ia-base.model';

@Component({
  selector: 'app-gasto-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gasto-form.html',
})
export class GastoFormComponent {
  @Input() categorias: OptionItem[] = [];
  @Input() metodos: OptionItem[] = [];
  @Input() model: GastoFormData = {
    nombreGasto: '',
    monto: 1500,
    fechaTransaccion: '',
    descripcion: '',
    categoria: '',
    categoriaNombre: '',
    metodoPago: 'TRANSFERENCIA',
    etiquetas: [],
  };
  @Input() sugerencias: CategoriaSugerida[] = [];
  @Input() clasificandoIa = false;
  @Input() intentosIaRestantes = 0;
  @Input() intentosIaMaximos = 2;
  @Input() sugerenciaSeleccionada: CategoriaSugerida | null = null;

  @Output() modelChange = new EventEmitter<GastoFormData>();
  @Output() guardar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
  @Output() seleccionarSugerencia = new EventEmitter<CategoriaSugerida>();
  @Output() confirmarSugerencia = new EventEmitter<void>();
  @Output() crearCategoriaManualmente = new EventEmitter<string>();
  @Output() clasificarIa = new EventEmitter<void>();

  nuevaEtiqueta = '';
  nuevaCategoriaNombre = '';
  nuevaCategoria = '';

  // Getters to inject the custom option safely
  get categoriasConCrear(): OptionItem[] {
    return [
      ...this.categorias,
      { label: '＋ Crear nueva categoría...', value: 'CREAR_NUEVA' }
    ];
  }

  onModelChange(): void {
    this.modelChange.emit(this.model);
  }

  /** Resuelve el nombre legible de la categoría seleccionada y emite el cambio. */
  onCategoriaChange(): void {
    const opcion = this.categoriasConCrear.find(c => c.value === this.model.categoria);
    this.model = {
      ...this.model,
      categoriaNombre: opcion ? opcion.label : this.model.categoria,
    };
    this.modelChange.emit(this.model);
  }

  confirmarCrearCategoria(): void {
    const nombre = (this.nuevaCategoriaNombre || '').trim();
    if (!nombre) return;

    this.crearCategoriaManualmente.emit(nombre);
    this.nuevaCategoriaNombre = '';
  }

  agregarEtiqueta(): void {
    const raw = (this.nuevaEtiqueta || '').trim();
    if (!raw) return;
    const unaPalabra = raw.split(' ')[0];
    if (!this.model.etiquetas.includes(unaPalabra)) {
      this.model.etiquetas = [...this.model.etiquetas, unaPalabra];
      this.onModelChange();
    }
    this.nuevaEtiqueta = '';
  }

  eliminarEtiqueta(tag: string): void {
    this.model.etiquetas = this.model.etiquetas.filter(t => t !== tag);
    this.onModelChange();
  }

  get puedeSugerirCategoriaIa(): boolean {
    return (this.model.descripcion?.trim()?.length || 0) >= 4 && !this.clasificandoIa && this.intentosIaRestantes > 0 && this.sugerencias.length === 0;
  }
}
