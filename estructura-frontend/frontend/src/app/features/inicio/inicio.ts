import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

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
