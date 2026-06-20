import { Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';

export interface SolicitudCambioPasswordDatos {
  passwordActual: string;
  nuevoPassword: string;
  confirmarPassword: string;
}

export interface FortalezaPasswordDatos {
  percent: number;
  label: string;
}

@Component({
  selector: 'app-perfil-seguridad',
  standalone: true,
  imports: [],
  templateUrl: './perfil-seguridad.html',
  styleUrl: '../../perfil-cliente.scss',
  encapsulation: ViewEncapsulation.None,
})
export class PerfilSeguridad {
  @Input() cambioPassword: SolicitudCambioPasswordDatos = { passwordActual: '', nuevoPassword: '', confirmarPassword: '' };
  @Input() fortalezaPassword: FortalezaPasswordDatos = { percent: 10, label: 'Débil' };
  @Input() mostrarPasswordActual = false;
  @Input() mostrarPasswordNueva = false;
  @Input() mostrarPasswordConfirmar = false;
  @Input() guardando = false;

  @Output() campoPasswordChange = new EventEmitter<{ campo: keyof SolicitudCambioPasswordDatos; valor: string }>();
  @Output() togglePasswordVisibility = new EventEmitter<'actual' | 'nueva' | 'confirmar'>();
  @Output() guardar = new EventEmitter<void>();

  onCampoInput(campo: keyof SolicitudCambioPasswordDatos, valor: string): void {
    this.campoPasswordChange.emit({ campo, valor });
  }

  onToggle(campo: 'actual' | 'nueva' | 'confirmar'): void {
    this.togglePasswordVisibility.emit(campo);
  }

  onGuardar(): void {
    this.guardar.emit();
  }
}
