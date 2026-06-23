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

  usarMock(): void {
    this.formulario.patchValue({
      correo: 'prueba@gmail.com',
      password: '12345'
    });
    this.iniciarSesion();
  }

  usarMockAdmin(): void {
    this.formulario.patchValue({
      correo: 'admin@gmail.com',
      password: '12345'
    });
    this.iniciarSesion();
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

    // [F-29] Intercepción de Usuario Mock para Pruebas (Frontend Only)
    if (solicitudLogin.correo === 'prueba@gmail.com' && solicitudLogin.password === '12345') {
      setTimeout(() => {
        this.cargando = false;
        this.authService.actualizarSesion({
          idUsuario: 'mock-12345',
          nombreUsuario: 'Usuario Prueba',
          roles: ['ROLE_PREMIUM', 'USER'],
          tokenAcceso: 'mock-jwt-token-valido-solo-frontend',
          expiraEn: new Date(Date.now() + 86400000).toISOString()
        } as any);
        this.notificacionService.mostrarLoginExitoso('Usuario Prueba');
        this.router.navigate(['/dashboard']);
      }, 600);
      return;
    }

    if (solicitudLogin.correo === 'admin@gmail.com' && solicitudLogin.password === '12345') {
      setTimeout(() => {
        this.cargando = false;
        this.authService.actualizarSesion({
          idUsuario: 'mock-admin',
          nombreUsuario: 'Administrador Prueba',
          roles: ['ROLE_ADMIN', 'ADMIN', 'ROLE_PREMIUM', 'USER'],
          tokenAcceso: 'mock-jwt-token-valido-solo-frontend-admin',
          expiraEn: new Date(Date.now() + 86400000).toISOString()
        } as any);
        this.notificacionService.mostrarLoginExitoso('Administrador Prueba');
        this.router.navigate(['/admin']);
      }, 600);
      return;
    }

    this.authService.login(solicitudLogin).subscribe({
      next: (resp) => {
        this.cargando = false;
        if (resp.exito) {
          this.notificacionService.mostrarLoginExitoso(resp.datos?.nombreUsuario || 'Usuario');
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
