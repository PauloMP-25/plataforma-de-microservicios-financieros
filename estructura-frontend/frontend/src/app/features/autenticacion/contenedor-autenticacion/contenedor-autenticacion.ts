import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';
import { AuthService } from '../../../core/services/auth.service';

// 1. Extendemos el tipo para incluir la vista 'canal' que pide el HTML
export type VistaAuth = 'login' | 'registro' | 'verificar' | 'exito' | 'canal';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
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
  telefonoTocado = false;

  get requiereTelefono(): boolean {
    return this.canalSeleccionado !== 'EMAIL';
  }

  get telefonoNormalizado(): string {
    return this.telefonoActivacion.trim();
  }

  get telefonoValido(): boolean {
    return /^\+?[0-9]{7,15}$/.test(this.telefonoNormalizado.replace(/\s+/g, ''));
  }

  get puedeEnviarCodigo(): boolean {
    return !this.cargandoOtp && (!this.requiereTelefono || this.telefonoValido);
  }

  seleccionarCanal(canal: 'EMAIL' | 'SMS' | 'WHATSAPP'): void {
    this.canalSeleccionado = canal;
    this.errorOtp = '';
    this.infoOtp = '';
    if (canal === 'EMAIL') {
      this.telefonoTocado = false;
    }
  }

  enviarCodigoActivacion(): void {
    if (this.requiereTelefono && !this.telefonoValido) {
      this.telefonoTocado = true;
      this.errorOtp = 'Ingresa un número válido para recibir el código por celular.';
      return;
    }

    this.cargandoOtp = true;
    this.errorOtp = '';
    this.infoOtp = '';

    const payload = {
      email: this.correoActivacion,
      tipo: this.canalSeleccionado,
      telefono: this.requiereTelefono ? this.telefonoNormalizado.replace(/\s+/g, '') : undefined
    };

    this.authService.solicitarOtpActivacion(payload).subscribe({
      next: (resp) => {
        this.cargandoOtp = false;
        if (resp.exito) {
          this.medioVerificacion = this.canalSeleccionado === 'EMAIL' ? 'correo' : 'celular';
          this.destinoVerificacion = this.canalSeleccionado === 'EMAIL' ? this.correoActivacion : this.telefonoNormalizado;
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
    this.errorOtp = '';
    this.infoOtp = '';
    const ruta = vista === 'login' ? '/autenticacion/iniciar-sesion' : '/autenticacion/crear-cuenta';
    this.location.go(ruta);
  }

  /** Maneja el registro exitoso y muestra la selección de canal */
  onRegistroExitoso(datos: { correo: string; usuarioId: string }): void {
    this.correoActivacion = datos.correo;
    this.telefonoActivacion = '';
    this.telefonoTocado = false;
    this.canalSeleccionado = 'EMAIL';
    this.usuarioId = datos.usuarioId;
    this.vistaActual = 'canal';
  }

  /** Maneja la verificación exitosa del código */
  onCodigoVerificado(codigo: string): void {
    console.log('Cuenta verificada con código:', codigo);
    this.vistaActual = 'exito';
  }
}
