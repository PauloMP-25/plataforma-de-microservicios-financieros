import { Component, ViewChildren, QueryList, ElementRef, AfterViewInit } from '@angular/core';
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

  digitos: string[] = ['', '', '', '', '', ''];
  cargando = false;
  errorMensaje = '';
  correoRecuperacion = '';

  constructor(private router: Router) {
    this.correoRecuperacion = sessionStorage.getItem('correo-recuperacion') || '';
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

    // Guardar código verificado para el paso 3
    console.log('Código OTP:', this.codigoCompleto);
    sessionStorage.setItem('codigo-otp', this.codigoCompleto);

    // TODO: Opcionalmente validar el código con el backend antes de avanzar
    setTimeout(() => {
      this.cargando = false;
      this.router.navigate(['/recuperar-contrasena/nueva']);
    }, 1000);
  }

  reenviarCodigo(): void {
    // TODO: Conectar con POST /api/v1/auth/recuperar-password usando el correo guardado
    console.log('Reenviar código a:', this.correoRecuperacion);
  }
}
