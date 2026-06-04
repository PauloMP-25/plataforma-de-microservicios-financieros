import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion implements OnInit {
  vistaActual: 'login' | 'registro' | 'canal' | 'verificar' | 'exito' = 'login';

  cargandoOtp = false;
  infoOtp = '';
  errorOtp = '';
  correoActivacion = '';
  telefonoActivacion = '';
  canalSeleccionado: 'EMAIL' | 'SMS' | 'WHATSAPP' = 'EMAIL';

  seleccionarCanal(canal: 'EMAIL' | 'SMS' | 'WHATSAPP'): void {
    this.canalSeleccionado = canal;
  }

  enviarCodigoActivacion(): void {
    this.cargandoOtp = true;
    this.errorOtp = '';
    this.infoOtp = '';

    const payload = {
      email: this.correoActivacion,
      tipo: this.canalSeleccionado,
      telefono: this.canalSeleccionado !== 'EMAIL' ? this.telefonoActivacion : undefined
    };

    this.authService.solicitarOtpActivacion(payload).subscribe({
      next: (resp) => {
        this.cargandoOtp = false;
        if (resp.exito) {
          this.medioVerificacion = this.canalSeleccionado === 'EMAIL' ? 'correo' : 'celular';
          this.destinoVerificacion = this.canalSeleccionado === 'EMAIL' ? this.correoActivacion : this.telefonoActivacion;
          this.vistaActual = 'verificar';
        } else {
          this.errorOtp = resp.mensaje || 'Error al enviar el código de verificación';
        }
      },
      error: (err) => {
        this.cargandoOtp = false;
        this.errorOtp = err.error?.mensaje || 'Error de conexión con el servidor';
        console.error('Error al enviar OTP:', err);
      }
    });
  }
  /** Datos del registro para el paso de verificación */
  medioVerificacion: 'correo' | 'celular' = 'correo';
  destinoVerificacion = '';
  usuarioId = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location,
    private authService: AuthService
  ) {}

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
    const ruta = vista === 'login' ? '/autenticacion/iniciar-sesion' : '/autenticacion/crear-cuenta';
    this.location.go(ruta);
  }

  /** Maneja el registro exitoso y muestra la selección de canal */
  onRegistroExitoso(datos: { correo: string; celular?: string; usuarioId: string }): void {
    this.correoActivacion = datos.correo;
    this.telefonoActivacion = datos.celular || '';
    this.usuarioId = datos.usuarioId;
    this.vistaActual = 'canal';
  }

  /** Maneja la verificación exitosa del código */
  onCodigoVerificado(codigo: string): void {
    console.log('Cuenta verificada con código:', codigo);
    this.vistaActual = 'exito';
  }
}
