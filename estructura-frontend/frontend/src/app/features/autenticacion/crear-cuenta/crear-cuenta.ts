import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';

@Component({
  selector: 'app-crear-cuenta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './crear-cuenta.html',
  styleUrl: './crear-cuenta.scss',
})
export class CrearCuenta {
  formulario: FormGroup;
  mostrarPassword = false;
  mostrarConfirmar = false;
  cargando = false;
  errorMensaje = '';

  constructor(private fb: FormBuilder, private router: Router) {
    this.formulario = this.fb.group({
      nombreUsuario: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
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

    // Objeto que coincide exactamente con SolicitudRegistro del backend
    const solicitudRegistro = {
      nombreUsuario: this.formulario.value.nombreUsuario,
      correo: this.formulario.value.correo,
      password: this.formulario.value.password,
      confirmarPassword: this.formulario.value.confirmarPassword
    };

    console.log('SolicitudRegistro:', solicitudRegistro);
    // TODO: Conectar con servicio de autenticación del backend
    setTimeout(() => {
      this.cargando = false;
    }, 1500);
  }
}
