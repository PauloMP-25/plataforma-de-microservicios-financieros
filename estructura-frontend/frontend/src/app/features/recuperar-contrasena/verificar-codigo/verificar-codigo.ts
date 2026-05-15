import { Component, ViewChildren, QueryList, ElementRef, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

@Component({
  selector: 'app-verificar-codigo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verificar-codigo.html',
  styleUrl: './verificar-codigo.scss',
})
export class VerificarCodigo {
  @ViewChildren('digitoInput') digitoInputs!: QueryList<ElementRef>;

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

  constructor(private router: Router) {
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

    // Guardar código verificado
    console.log('Código OTP:', this.codigoCompleto);
    sessionStorage.setItem('codigo-otp', this.codigoCompleto);

    // TODO: Validar el código con el backend
    setTimeout(() => {
      this.cargando = false;
      // Si tiene listeners, emitir evento; si no, navegar
      if (this.codigoVerificado.observed) {
        this.codigoVerificado.emit(this.codigoCompleto);
      } else {
        this.router.navigate([this.rutaSiguiente]);
      }
    }, 1000);
  }

  reenviarCodigo(): void {
    // TODO: Conectar con POST /api/v1/auth/recuperar-password o /verificar-registro
    console.log('Reenviar código a:', this.destinoMostrado);
  }
}
