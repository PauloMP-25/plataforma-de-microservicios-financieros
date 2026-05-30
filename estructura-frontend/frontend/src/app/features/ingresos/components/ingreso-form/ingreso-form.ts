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
    fechaTransaccion: '31/05/2025',
    descripcion: 'Pago mensual de la empresa ABC',
    categoria: 'Salario',
    metodoPago: 'TRANSFERENCIA',
    etiquetas: ['Trabajo', 'Mensual'],
  };
  @Input() sugerencia = { categoria: 'Salario', confianza: 0.98 };

  @Output() modelChange = new EventEmitter<IngresoFormData>();
  @Output() guardar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
  @Output() usarSugerencia = new EventEmitter<void>();

  nuevaEtiqueta = '';

  onModelChange(): void { this.modelChange.emit(this.model); }

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

