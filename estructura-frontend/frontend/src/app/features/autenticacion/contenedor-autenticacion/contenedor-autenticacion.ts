import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { IniciarSesion } from '../iniciar-sesion/iniciar-sesion';
import { CrearCuenta } from '../crear-cuenta/crear-cuenta';
import { VerificarCodigo } from '../../recuperar-contrasena/verificar-codigo/verificar-codigo';

// 1. Extendemos el tipo para incluir la vista 'canal' que pide el HTML
export type VistaAuth = 'login' | 'registro' | 'verificar' | 'exito' | 'canal';

@Component({
  selector: 'app-contenedor-autenticacion',
  standalone: true,
  imports: [CommonModule, RouterLink, IniciarSesion, CrearCuenta, VerificarCodigo],
  templateUrl: './contenedor-autenticacion.html',
  styleUrl: './contenedor-autenticacion.scss',
})
export class ContenedorAutenticacion implements OnInit {
  // 2. Cambiamos el tipo aquí para admitir 'canal'
  vistaActual: VistaAuth = 'login';

  /** Datos del registro para el paso de verificación */
  medioVerificacion: 'correo' | 'celular' = 'correo';
  destinoVerificacion = '';
  usuarioId = '';

  // 3. PROPIEDADES NUEVAS que el HTML necesita urgentemente:
  canalSeleccionado: 'EMAIL' | 'SMS' | 'WHATSAPP' | null = null;
  correoActivacion: string = 'ejemplo@correo.com'; 
  telefonoActivacion: string = '999999990';
  cargandoOtp: boolean = false;
  infoOtp: string | null = null;
  errorOtp: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private location: Location
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

  // 4. MÉTODOS NUEVOS que ejecutan los (click) del HTML:
  seleccionarCanal(canal: 'EMAIL' | 'SMS' | 'WHATSAPP'): void {
    this.canalSeleccionado = canal;
  }

  enviarCodigoActivacion(): void {
    this.cargandoOtp = true;
    this.infoOtp = 'Enviando código de verificación...';
    this.errorOtp = null;

    console.log(`Enviando OTP simulado por: ${this.canalSeleccionado}`);

    // Simulación para que no se quede congelado el botón en tu máquina
    setTimeout(() => {
      this.cargandoOtp = false;
      this.infoOtp = 'Código enviado con éxito.';
      this.vistaActual = 'verificar'; // Salta al siguiente paso
    }, 1500);
  }
}