import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

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

  constructor(private fb: FormBuilder, private router: Router) {
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

    // TODO: Conectar con servicio de autenticación del backend
    const solicitudLogin = {
      correo: this.formulario.value.correo,
      password: this.formulario.value.password
    };

    console.log('SolicitudLogin:', solicitudLogin);
    // Simular respuesta por ahora
    setTimeout(() => {
      this.cargando = false;
      // this.router.navigate(['/dashboard']);
    }, 1500);
  }
}
