import { Component, ViewChildren, QueryList, ElementRef, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verificar-codigo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verificar-codigo.html',
  styleUrl: './verificar-codigo.scss',
})
export class VerificarCodigo {
  @ViewChildren('digitoInput') digitoInputs!: QueryList<ElementRef>;

  // ==========================================
  // SOLUCIÓN: Agregamos el Input que pide el HTML
  // ==========================================
  @Input() correoActivacion = '';

  /** ID del usuario para activación (opcional si es para recuperación) */
  @Input() usuarioId = '';
  /** Medio de verificación: 'correo' o 'celular' */
  @Input() medioVerificacion: 'correo' | 'celular' = 'correo';
  /** Destino al que se envió el código (email o teléfono) */
  @Input() destinoVerificacion = '';
  /** Si es true, el componente se renderiza embebido (sin fondo ni tarjeta propios) */
  @Input() embebido = false;
  /** Ruta a donde redirigir al hacer clic en "Volver" (solo modo standalone) */
  @Input() rutaVolver = '/recuperar-contrasena/correo';
  /** Ruta a donde navegar tras verificación exitosa (solo modo standalone) */
  @Input() rutaSiguiente = '/recuperar-contrasena/nueva';

  /** Emite cuando el código es verificado exitosamente */
  @Output() codigoVerificado = new EventEmitter<string>();

  digitos: string[] = ['', '', '', '', '', ''];
  cargando = false;
  errorMensaje = '';
  infoMensaje = '';
  correoRecuperacion = '';

  // ── Nuevas propiedades para cambio de canal en caliente ──
  nuevoCanal: 'EMAIL' | 'SMS' | 'WHATSAPP' | null = null;
  nuevoTelefono = '';
  telefonoTocado = false;

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    // Intenta cargar el correo si viene del flujo de recuperar contraseña
    this.correoRecuperacion = sessionStorage.getItem('correo-recuperacion') || '';
  }

  /** Destino que se muestra (prioriza el input del HTML, luego el alias nuevo, luego sessionStorage) */
  get destinoMostrado(): string {
    return this.destinoVerificacion || this.correoActivacion || this.correoRecuperacion;
  }

  /** Texto descriptivo según el medio de verificación */
  get textoDescripcion(): string {
    if (this.medioVerificacion === 'celular') {
      return 'Ingresa el código de 6 dígitos que enviamos a tu número de celular.';
    }
    return 'Ingresa el código de 6 dígitos que enviamos a tu correo electrónico.';
  }

  get nuevoTelefonoValido(): boolean {
    return /^\+?[0-9]{7,15}$/.test(this.nuevoTelefono.trim().replace(/\s+/g, ''));
  }

  seleccionarNuevoCanal(canal: 'EMAIL' | 'SMS' | 'WHATSAPP'): void {
    this.nuevoCanal = canal;
    this.telefonoTocado = false;
    this.errorMensaje = '';
    this.infoMensaje = '';
    if (canal === 'EMAIL') {
      this.nuevoTelefono = '';
    } else {
      this.nuevoTelefono = this.medioVerificacion === 'celular' ? this.destinoVerificacion : '';
    }
  }

  onDigitoInput(evento: Event, indice: number): void {
    const input = evento.target as HTMLInputElement;
    const valor = input.value.replace(/\D/g, '');
    this.digitos[indice] = valor.slice(0, 1);
    input.value = this.digitos[indice];

    // Avanzar al siguiente campo
    if (valor && indice < 5) {
      const inputs = this.digitoInputs.toArray();
      inputs[indice + 1].nativeElement.focus();
    }
  }

  onDigitoKeydown(evento: KeyboardEvent, indice: number): void {
    // Retroceder al campo anterior con Backspace
    if (evento.key === 'Backspace' && !this.digitos[indice] && indice > 0) {
      const inputs = this.digitoInputs.toArray();
      inputs[indice - 1].nativeElement.focus();
    }
  }

  onPegar(evento: ClipboardEvent): void {
    evento.preventDefault();
    const texto = evento.clipboardData?.getData('text')?.replace(/\D/g, '') || '';
    for (let i = 0; i < 6 && i < texto.length; i++) {
      this.digitos[i] = texto[i];
    }
    // Focus en el último dígito pegado
    const inputs = this.digitoInputs.toArray();
    const ultimoIndice = Math.min(texto.length, 6) - 1;
    if (ultimoIndice >= 0) inputs[ultimoIndice].nativeElement.focus();
  }

  get codigoCompleto(): string {
    return this.digitos.join('');
  }

  get codigoValido(): boolean {
    return this.codigoCompleto.length === 6;
  }

  verificarCodigo(): void {
    if (!this.codigoValido) return;
    this.cargando = true;
    this.errorMensaje = '';
    this.infoMensaje = '';

    const codigo = this.codigoCompleto;
    sessionStorage.setItem('codigo-otp', codigo);

    // Si tenemos usuarioId, es flujo de activación de cuenta
    if (this.usuarioId) {
      const telefono = this.medioVerificacion === 'celular' ? this.destinoVerificacion : undefined;
      
      this.authService.activarCuenta(this.usuarioId, codigo, telefono).subscribe({
        next: (resp) => {
          this.cargando = false;
          if (resp.exito) {
            this.emitirYRedirigir(codigo);
          } else {
            this.errorMensaje = resp.mensaje || 'Código inválido';
          }
        },
        error: (err) => {
          this.cargando = false;
          this.errorMensaje = err.error?.mensaje || 'Error al validar código';
        }
      });
    } else {
      // Flujo de recuperación de contraseña u otros (sin validación directa aquí por ahora)
      setTimeout(() => {
        this.cargando = false;
        this.emitirYRedirigir(codigo);
      }, 1000);
    }
  }

  private emitirYRedirigir(codigo: string): void {
    if (this.codigoVerificado.observed) {
      this.codigoVerificado.emit(codigo);
    } else {
      this.router.navigate([this.rutaSiguiente]);
    }
  }

  reenviarCodigo(): void {
    if (!this.usuarioId && this.destinoMostrado) {
      this.cargando = true;
      this.errorMensaje = '';
      this.infoMensaje = '';
      this.authService.solicitarRecuperacion({ correo: this.destinoMostrado }).subscribe({
        next: (resp) => {
          this.cargando = false;
          if (resp.exito) {
            sessionStorage.setItem('registro-id', resp.datos);
            this.infoMensaje = 'Código reenviado con éxito';
            setTimeout(() => this.infoMensaje = '', 4000);
          } else {
            this.errorMensaje = resp.mensaje || 'Error al reenviar el código';
          }
        },
        error: (err) => {
          this.cargando = false;
          this.errorMensaje = err.error?.mensaje || 'Error al reenviar el código';
        }
      });
    } else {
      console.log('Reenviar código de activación a:', this.destinoMostrado);
      // Flujo de activación: si el usuario pide reenviar al canal actual
      this.cargando = true;
      this.errorMensaje = '';
      this.infoMensaje = '';
      const email = this.correoActivacion || this.correoRecuperacion;
      const tipo = this.medioVerificacion === 'correo' ? 'EMAIL' : 'SMS'; // Fallback a SMS si es celular por defecto
      const telefono = this.medioVerificacion === 'celular' ? this.destinoVerificacion.replace(/\s+/g, '') : undefined;

      this.authService.solicitarOtpActivacion({ email, tipo, telefono }).subscribe({
        next: (resp) => {
          this.cargando = false;
          if (resp.exito) {
            this.infoMensaje = 'Código reenviado con éxito';
            setTimeout(() => this.infoMensaje = '', 4000);
          } else {
            this.errorMensaje = resp.mensaje || 'Error al reenviar el código';
          }
        },
        error: (err) => {
          this.cargando = false;
          this.errorMensaje = err.error?.mensaje || 'Error al reenviar el código';
        }
      });
    }
  }

  confirmarCambioCanal(): void {
    if (!this.nuevoCanal) return;

    if ((this.nuevoCanal === 'SMS' || this.nuevoCanal === 'WHATSAPP') && !this.nuevoTelefonoValido) {
      this.telefonoTocado = true;
      this.errorMensaje = 'Ingresa un número válido de 7 a 15 dígitos.';
      return;
    }

    this.cargando = true;
    this.errorMensaje = '';
    this.infoMensaje = '';

    const email = this.correoActivacion || this.correoRecuperacion;
    const telefono = (this.nuevoCanal === 'SMS' || this.nuevoCanal === 'WHATSAPP') ? this.nuevoTelefono.trim().replace(/\s+/g, '') : undefined;

    if (this.usuarioId) {
      // Flujo de activación de cuenta
      const payload = {
        email: email,
        tipo: this.nuevoCanal,
        telefono: telefono
      };

      this.authService.solicitarOtpActivacion(payload).subscribe({
        next: (resp) => {
          this.cargando = false;
          if (resp.exito) {
            this.medioVerificacion = this.nuevoCanal === 'EMAIL' ? 'correo' : 'celular';
            this.destinoVerificacion = this.nuevoCanal === 'EMAIL' ? email : this.nuevoTelefono.trim();
            this.nuevoCanal = null;
            this.infoMensaje = 'Código OTP enviado al nuevo canal con éxito';
            setTimeout(() => this.infoMensaje = '', 4000);
          } else {
            this.errorMensaje = resp.mensaje || 'Error al enviar el código';
          }
        },
        error: (err) => {
          this.cargando = false;
          this.errorMensaje = err.error?.mensaje || 'Error de conexión con el servidor';
        }
      });
    } else {
      // Flujo de recuperación de contraseña
      const payload = {
        correo: email,
        tipo: this.nuevoCanal,
        telefono: telefono
      };

      this.authService.solicitarRecuperacion(payload).subscribe({
        next: (resp) => {
          this.cargando = false;
          if (resp.exito) {
            this.medioVerificacion = this.nuevoCanal === 'EMAIL' ? 'correo' : 'celular';
            this.destinoVerificacion = this.nuevoCanal === 'EMAIL' ? email : this.nuevoTelefono.trim();
            this.nuevoCanal = null;
            this.infoMensaje = 'Código OTP enviado al nuevo canal con éxito';
            setTimeout(() => this.infoMensaje = '', 4000);
          } else {
            this.errorMensaje = resp.mensaje || 'Error al enviar el código';
          }
        },
        error: (err) => {
          this.cargando = false;
          this.errorMensaje = err.error?.mensaje || 'Error de conexión con el servidor';
        }
      });
    }
  }
}