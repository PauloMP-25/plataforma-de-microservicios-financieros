import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService, NotificacionService } from '../../../core/services';

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
    private authService: AuthService,
    private notificacionService: NotificacionService
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
        if (resp.exito) {
          this.notificacionService.mostrarLoginExitoso(resp.datos?.nombreUsuario || 'Usuario');
          // Añadimos un pequeño retraso para permitir que el Header/Dashboard inicialicen sin parpadeo
          setTimeout(() => {
            this.cargando = false;
            if (this.authService.esAdmin()) {
              this.router.navigate(['/admin']);
            } else {
              this.router.navigate(['/dashboard']);
            }
          }, 1000);
        } else {
          this.cargando = false;
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
