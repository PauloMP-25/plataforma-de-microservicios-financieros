import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoriaHueso } from '../luka-esqueleto-svg/luka-esqueleto-svg';

@Component({
  selector: 'luka-receta-medica',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './luka-receta-medica.html',
  styleUrl: './luka-receta-medica.scss'
})
export class LukaRecetaMedicaComponent {
  @Input() isOpen = false;
  @Input() categoria: CategoriaHueso | null = null;
  @Output() onClose = new EventEmitter<void>();

  close() {
    this.onClose.emit();
  }
}
