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
  correoRecuperacion = '';

  constructor(
    private router: Router,
    private authService: AuthService
  ) {
    // Intenta cargar el correo si viene del flujo de recuperar contraseña
    this.correoRecuperacion = sessionStorage.getItem('correo-recuperacion') || '';
  }

  /** Destino que se muestra (prioriza el input, luego sessionStorage) */
  get destinoMostrado(): string {
    return this.destinoVerificacion || this.correoRecuperacion;
  }

  /** Texto descriptivo según el medio de verificación */
  get textoDescripcion(): string {
    if (this.medioVerificacion === 'celular') {
      return 'Ingresa el código de 6 dígitos que enviamos a tu número de celular.';
    }
    return 'Ingresa el código de 6 dígitos que enviamos a tu correo electrónico.';
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
    // TODO: Conectar con POST /api/v1/auth/recuperar-password o /verificar-registro
    console.log('Reenviar código a:', this.destinoMostrado);
  }
}
