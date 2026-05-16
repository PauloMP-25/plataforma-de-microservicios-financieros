import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-iniciar-sesion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './iniciar-sesion.html',
  styleUrl: './iniciar-sesion.scss',
})
export class IniciarSesion {
  formulario: FormGroup;
  mostrarPassword = false;
  cargando = false;
  errorMensaje = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.formulario = this.fb.group({
      correo: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  alternarPassword(): void {
    this.mostrarPassword = !this.mostrarPassword;
  }

  iniciarSesion(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.errorMensaje = '';

    const solicitudLogin = {
      correo: this.formulario.value.correo,
      password: this.formulario.value.password
    };

    this.authService.login(solicitudLogin).subscribe({
      next: (resp) => {
        this.cargando = false;
        if (resp.exito) {
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMensaje = resp.mensaje || 'Error al iniciar sesión';
        }
      },
      error: (err) => {
        this.cargando = false;
        this.errorMensaje = err.error?.mensaje || 'Error de conexión con el servidor';
        console.error('Error Login:', err);
      }
    });
  }
}
