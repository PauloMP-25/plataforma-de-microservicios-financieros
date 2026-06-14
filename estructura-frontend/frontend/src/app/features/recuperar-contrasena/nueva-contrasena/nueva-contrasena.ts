import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-nueva-contrasena',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './nueva-contrasena.html',
  styleUrl: './nueva-contrasena.scss',
})
export class NuevaContrasena {
  formulario: FormGroup;
  mostrarPassword = false;
  mostrarConfirmar = false;
  cargando = false;
  exitoso = false;
  errorMensaje = '';
  codigoOtp = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.codigoOtp = sessionStorage.getItem('codigo-otp') || '';

    this.formulario = this.fb.group({
      nuevoPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmarPassword: ['', [Validators.required]]
    }, { validators: this.validarCoincidencia });
  }

  validarCoincidencia(grupo: AbstractControl): ValidationErrors | null {
    const password = grupo.get('nuevoPassword')?.value;
    const confirmar = grupo.get('confirmarPassword')?.value;
    if (password && confirmar && password !== confirmar) {
      return { noCoinciden: true };
    }
    return null;
  }

  alternarPassword(): void { this.mostrarPassword = !this.mostrarPassword; }
  alternarConfirmar(): void { this.mostrarConfirmar = !this.mostrarConfirmar; }

  restablecerPassword(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const registroId = sessionStorage.getItem('registro-id') || '';
    const codigoOtp = sessionStorage.getItem('codigo-otp') || '';

    if (!registroId || !codigoOtp) {
      this.errorMensaje = 'No se encontraron las credenciales temporales de recuperación. Por favor, vuelva a solicitar el código.';
      return;
    }

    this.cargando = true;
    this.errorMensaje = '';

    const solicitudRestablecer = {
      nuevoPassword: this.formulario.value.nuevoPassword,
      confirmarPassword: this.formulario.value.confirmarPassword
    };

    this.authService.resetPassword(registroId, codigoOtp, solicitudRestablecer).subscribe({
      next: (resp) => {
        this.cargando = false;
        if (resp.exito) {
          this.exitoso = true;
          sessionStorage.removeItem('correo-recuperacion');
          sessionStorage.removeItem('registro-id');
          sessionStorage.removeItem('codigo-otp');
          setTimeout(() => this.router.navigate(['/autenticacion/iniciar-sesion']), 3000);
        } else {
          this.errorMensaje = resp.mensaje || 'Error al restablecer la contraseña';
        }
      },
      error: (err) => {
        this.cargando = false;
        this.errorMensaje = err.error?.mensaje || 'Error al restablecer la contraseña';
      }
    });
  }
}
