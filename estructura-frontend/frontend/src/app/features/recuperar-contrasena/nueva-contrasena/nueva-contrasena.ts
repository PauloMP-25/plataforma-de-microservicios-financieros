import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

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

  constructor(private fb: FormBuilder, private router: Router) {
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
    this.cargando = true;
    this.errorMensaje = '';

    // Objeto que coincide con SolicitudRestablecerPassword del backend
    // Se envía junto con codigoOtp como query param
    const solicitudRestablecer = {
      nuevoPassword: this.formulario.value.nuevoPassword,
      confirmarPassword: this.formulario.value.confirmarPassword
    };

    console.log('SolicitudRestablecerPassword:', solicitudRestablecer);
    console.log('codigoOtp (query param):', this.codigoOtp);
    // TODO: Conectar con POST /api/v1/auth/reset-password?codigoOtp=XXXXXX

    setTimeout(() => {
      this.cargando = false;
      this.exitoso = true;
      // Limpiar datos de sesión
      sessionStorage.removeItem('correo-recuperacion');
      sessionStorage.removeItem('codigo-otp');
      setTimeout(() => this.router.navigate(['/autenticacion/iniciar-sesion']), 3000);
    }, 1500);
  }
}
