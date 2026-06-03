import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';
import { AuthService } from '../../../core/services/auth.service';
import { TipoVerificacionOtp } from '../../../core/models/auth/user.model';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion implements OnInit {
  vistaActual: 'login' | 'registro' | 'canal' | 'verificar' | 'exito' = 'login';

  /** Datos del registro para el paso de verificación */
  medioVerificacion: 'correo' | 'celular' = 'correo';
  destinoVerificacion = '';
  usuarioId = '';
  correoActivacion = '';
  telefonoActivacion = '';
  canalSeleccionado: TipoVerificacionOtp = 'EMAIL';
  cargandoOtp = false;
  errorOtp = '';
  infoOtp = '';

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

  /** Maneja el registro exitoso y muestra la verificación de código */
  onRegistroExitoso(datos: { medio: 'correo' | 'celular'; destino: string; usuarioId: string; correo: string; telefono?: string }): void {
    this.medioVerificacion = datos.medio;
    this.destinoVerificacion = datos.destino;
    this.usuarioId = datos.usuarioId;
    this.correoActivacion = datos.correo;
    this.telefonoActivacion = datos.telefono || '';
    this.canalSeleccionado = datos.medio === 'celular' ? 'SMS' : 'EMAIL';
    this.errorOtp = '';
    this.infoOtp = 'Elige el canal para recibir tu código de activación.';
    this.vistaActual = 'canal';
  }

  seleccionarCanal(canal: TipoVerificacionOtp): void {
    this.canalSeleccionado = canal;
    this.errorOtp = '';
  }

  enviarCodigoActivacion(): void {
    if (!this.correoActivacion) {
      this.errorOtp = 'No se encontró el correo de activación. Registra tu cuenta nuevamente.';
      return;
    }

    const requiereTelefono = this.canalSeleccionado === 'SMS' || this.canalSeleccionado === 'WHATSAPP';
    if (requiereTelefono && !this.telefonoActivacion) {
      this.errorOtp = 'Ingresa un teléfono en el registro para usar SMS o WhatsApp.';
      return;
    }

    this.cargandoOtp = true;
    this.errorOtp = '';
    this.infoOtp = '';
    this.authService.solicitarOtpActivacion({
      email: this.correoActivacion,
      telefono: requiereTelefono ? this.telefonoActivacion : undefined,
      tipo: this.canalSeleccionado
    }).subscribe({
      next: (resp) => {
        this.cargandoOtp = false;
        if (resp.exito) {
          this.medioVerificacion = requiereTelefono ? 'celular' : 'correo';
          this.destinoVerificacion = requiereTelefono ? this.telefonoActivacion : this.correoActivacion;
          this.infoOtp = resp.mensaje || 'Código enviado correctamente.';
          this.vistaActual = 'verificar';
        } else {
          this.errorOtp = resp.mensaje || 'No se pudo enviar el código de activación.';
        }
      },
      error: (err) => {
        this.cargandoOtp = false;
        this.errorOtp = err.error?.mensaje || 'Error al solicitar el código de activación.';
      }
    });
  }

  /** Maneja la verificación exitosa del código */
  onCodigoVerificado(codigo: string): void {
    this.vistaActual = 'exito';
  }
}
