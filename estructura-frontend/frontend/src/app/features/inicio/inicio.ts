import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ServicioTema } from '../../core/services/servicio-tema';

@Component({
  selector: 'app-inicio',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  readonly anioActual = new Date().getFullYear();
  menuAbierto = false;
  readonly servicioTema = inject(ServicioTema);

  scrollHacia(idElemento: string): void {
    this.menuAbierto = false;
    const elemento = document.getElementById(idElemento);
    if (elemento) {
      elemento.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  alternarMenu(): void {
    this.menuAbierto = !this.menuAbierto;
  }
}
