import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-solicitar-correo',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './solicitar-correo.html',
  styleUrl: './solicitar-correo.scss',
})
export class SolicitarCorreo {
  formulario: FormGroup;
  cargando = false;
  enviado = false;
  errorMensaje = '';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.formulario = this.fb.group({
      correo: ['', [Validators.required, Validators.email]]
    });
  }

  enviarSolicitud(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.cargando = true;
    this.errorMensaje = '';

    const solicitudRecuperacion = {
      correo: this.formulario.value.correo
    };

    this.authService.solicitarRecuperacion(solicitudRecuperacion).subscribe({
      next: (resp) => {
        this.cargando = false;
        if (resp.exito) {
          this.enviado = true;
          sessionStorage.setItem('correo-recuperacion', this.formulario.value.correo);
          sessionStorage.setItem('registro-id', resp.datos);
          setTimeout(() => this.router.navigate(['/recuperar-contrasena/codigo']), 2000);
        } else {
          this.errorMensaje = resp.mensaje || 'Error al enviar la solicitud';
        }
      },
      error: (err) => {
        this.cargando = false;
        this.errorMensaje = err.error?.mensaje || 'Error al enviar la solicitud';
      }
    });
  }
}
