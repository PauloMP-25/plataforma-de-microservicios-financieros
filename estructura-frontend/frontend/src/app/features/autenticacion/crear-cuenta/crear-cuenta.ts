import { Component, Output, EventEmitter } from '@angular/core';
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
  /** Emite cuando el registro es exitoso para pasar al paso de verificación */
  @Output() registroExitoso = new EventEmitter<{ medio: 'correo' | 'celular'; destino: string }>();

  formulario: FormGroup;
  mostrarPassword = false;
  mostrarConfirmar = false;
  usarCelular = false;
  cargando = false;
  errorMensaje = '';

  constructor(private fb: FormBuilder, private router: Router) {
    this.formulario = this.fb.group({
      nombreUsuario: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email]],
      celular: [''],
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

  /** Activa o desactiva el campo de celular con sus validadores */
  alternarCelular(): void {
    this.usarCelular = !this.usarCelular;
    const controlCelular = this.formulario.get('celular');
    if (this.usarCelular) {
      controlCelular?.setValidators([Validators.required, Validators.pattern(/^\+?[0-9]{7,15}$/)]);
    } else {
      controlCelular?.clearValidators();
      controlCelular?.setValue('');
    }
    controlCelular?.updateValueAndValidity();
  }

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
      celular: this.usarCelular ? this.formulario.value.celular : null,
      password: this.formulario.value.password,
      confirmarPassword: this.formulario.value.confirmarPassword,
      verificacionPor: this.usarCelular ? 'celular' : 'correo'
    };

    console.log('SolicitudRegistro:', solicitudRegistro);
    // TODO: Conectar con servicio de autenticación del backend
    setTimeout(() => {
      this.cargando = false;
      // Emitir evento para mostrar verificación de código
      this.registroExitoso.emit({
        medio: this.usarCelular ? 'celular' : 'correo',
        destino: this.usarCelular ? this.formulario.value.celular : this.formulario.value.correo
      });
    }, 1500);
  }
}
