import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, RouterLink, IniciarSesion, CrearCuenta],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion {
  vistaActual: 'login' | 'registro' = 'login';

  cambiarVista(vista: 'login' | 'registro'): void {
    this.vistaActual = vista;
  }
}
