import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IngresoFormData, OptionItem } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingreso-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ingreso-form.html',
})
export class IngresoFormComponent {
  @Input() categorias: OptionItem[] = [];
  @Input() metodos: OptionItem[] = [];
  @Input() model: IngresoFormData = {
    monto: 1500,
    fechaTransaccion: '',
    descripcion: '',
    categoria: '',
    metodoPago: 'TRANSFERENCIA',
    etiquetas: [],
  };
  @Input() sugerencias: string[] = [];

  @Output() modelChange = new EventEmitter<IngresoFormData>();
  @Output() guardar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
  @Output() seleccionarSugerencia = new EventEmitter<string>();
  @Output() crearCategoriaManualmente = new EventEmitter<string>();

  nuevaEtiqueta = '';
  nuevaCategoriaNombre = '';

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
}
