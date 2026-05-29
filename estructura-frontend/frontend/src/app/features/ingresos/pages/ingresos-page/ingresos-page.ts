import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ingresos-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ingresos-page.html',
  styleUrl: './ingresos-page.scss',
})
export class IngresosPage {
  
  // Propiedad para controlar si el modal se muestra o se oculta
  isModalAbierto: boolean = false;

  // Método para abrir el modal al dar click en "Registrar Ingreso"
  abrirModal(): void {
    this.isModalAbierto = true;
  }

  // Método para cerrar el modal al dar click en "Descartar" o la "X"
  cerrarModal(): void {
    this.isModalAbierto = false;
  }

}