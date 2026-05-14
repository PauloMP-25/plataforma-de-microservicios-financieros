import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

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

  constructor(private fb: FormBuilder, private router: Router) {
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

    // Objeto que coincide con SolicitudRecuperacion del backend
    const solicitudRecuperacion = {
      correo: this.formulario.value.correo
    };

    console.log('SolicitudRecuperacion:', solicitudRecuperacion);
    // TODO: Conectar con POST /api/v1/auth/recuperar-password
    setTimeout(() => {
      this.cargando = false;
      this.enviado = true;
      // Guardar correo para pasos siguientes
      sessionStorage.setItem('correo-recuperacion', this.formulario.value.correo);
      setTimeout(() => this.router.navigate(['/recuperar-contrasena/codigo']), 2000);
    }, 1500);
  }
}
