import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion implements OnInit {
  vistaActual: 'login' | 'registro' | 'verificar' | 'exito' = 'login';

  /** Datos del registro para el paso de verificación */
  medioVerificacion: 'correo' | 'celular' = 'correo';
  destinoVerificacion = '';
  usuarioId = '';

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    // Leer el estado inicial desde los datos de la ruta
    this.route.data.subscribe(data => {
      if (data['vista']) {
        this.vistaActual = data['vista'];
      }
    });
  }

  cambiarVista(vista: 'login' | 'registro'): void {
    this.vistaActual = vista;
  }

  /** Maneja el registro exitoso y muestra la verificación de código */
  onRegistroExitoso(datos: { medio: 'correo' | 'celular'; destino: string; usuarioId: string }): void {
    this.medioVerificacion = datos.medio;
    this.destinoVerificacion = datos.destino;
    this.usuarioId = datos.usuarioId;
    this.vistaActual = 'verificar';
  }

  /** Maneja la verificación exitosa del código */
  onCodigoVerificado(codigo: string): void {
    console.log('Cuenta verificada con código:', codigo);
    this.vistaActual = 'exito';
  }
}
