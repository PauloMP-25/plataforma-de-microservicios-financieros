import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-crear-cuenta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './crear-cuenta.html',
  styleUrl: './crear-cuenta.scss',
})
export class CrearCuenta {
  /** Emite cuando el registro es exitoso para pasar al paso de selección de canal */
  @Output() registroExitoso = new EventEmitter<{ correo: string; usuarioId: string }>();

  formulario: FormGroup;
  mostrarPassword = false;
  mostrarConfirmar = false;
  cargando = false;
  errorMensaje = '';
  esConflictoRegistro = false;
  passwordFocus = false;

  get reqLength() { return (this.formulario.get('password')?.value || '').length >= 8; }
  get reqLower() { return /[a-z]/.test(this.formulario.get('password')?.value || ''); }
  get reqUpper() { return /[A-Z]/.test(this.formulario.get('password')?.value || ''); }
  get reqNum() { return /\d/.test(this.formulario.get('password')?.value || ''); }
  get reqSpec() { return /[@$!%*?&#\-]/.test(this.formulario.get('password')?.value || ''); }

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.formulario = this.fb.group({
      nombreUsuario: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required, 
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#\-]).{8,}$/)
      ]],
      confirmarPassword: ['', [Validators.required]]
    }, { validators: this.validarCoincidencia });
  }

  /** Validación cruzada: las contraseñas deben coincidir */
  validarCoincidencia(grupo: AbstractControl): ValidationErrors | null {
    const password = grupo.get('password')?.value;
    const confirmar = grupo.get('confirmarPassword')?.value;
    if (password && confirmar && password !== confirmar) {
      return { noCoinciden: true };
    }
    return null;
  }

  alternarPassword(): void { this.mostrarPassword = !this.mostrarPassword; }
  alternarConfirmar(): void { this.mostrarConfirmar = !this.mostrarConfirmar; }

  registrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.errorMensaje = '';

    const solicitudRegistro = {
      nombreUsuario: this.formulario.value.nombreUsuario,
      correo: this.formulario.value.correo,
      password: this.formulario.value.password,
      confirmarPassword: this.formulario.value.confirmarPassword
    };

    this.authService.registrar(solicitudRegistro).subscribe({
      next: (resp) => {
        this.cargando = false;
        if (resp.exito) {
          // Emitir evento para mostrar verificación de código, pasando el usuarioId recibido
          this.registroExitoso.emit({
            correo: this.formulario.value.correo,
            usuarioId: resp.datos // El UUID del usuario creado
          });
        } else {
          this.esConflictoRegistro = false;
          this.errorMensaje = 'No pudimos crear tu cuenta con estos datos. Si ya tienes una cuenta, inicia sesión o recupera tu contraseña.';
        }
      },
      error: (err) => {
        this.cargando = false;
        // Never reveal whether email/username already exists — generic message prevents user enumeration
        const status = err?.status;
        this.esConflictoRegistro = status === 409 || status === 400;
        this.errorMensaje = 'No pudimos crear tu cuenta con estos datos. Si ya tienes una cuenta, inicia sesión o recupera tu contraseña.';
        console.error('Error Registro:', err);
      }
    });
  }
}
