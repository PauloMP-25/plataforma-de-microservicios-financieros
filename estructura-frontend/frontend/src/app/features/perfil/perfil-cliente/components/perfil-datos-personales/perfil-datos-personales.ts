import { Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface PerfilFormDatos {
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  dni: string;
  edad: string;
  correo: string;
  telefonoCodigoPais: string;
  telefonoNumero: string;
  pais: string;
  ciudad: string;
  genero: string;
}

export interface MesCatalogo {
  value: number;
  label: string;
}

export interface PaisCatalogoDatos {
  codigo: string;
  nombre: string;
  banderaClase: string;
  prefijo: string;
  ciudades: string[];
}

export interface FechaPartesDatos {
  dia: string;
  mes: string;
  anio: string;
}

export interface ErroresDatos {
  nombres?: string;
  apellidos?: string;
  dni?: string;
  edad?: string;
  correo?: string;
  telefonoNumero?: string;
  pais?: string;
  ciudad?: string;
  genero?: string;
}

@Component({
  selector: 'app-perfil-datos-personales',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './perfil-datos-personales.html',
  styleUrl: '../../perfil-cliente.scss',
  encapsulation: ViewEncapsulation.None,
})
export class PerfilDatosPersonales {
  @Input() form!: PerfilFormDatos;
  @Input() errores: ErroresDatos = {};
  @Input() loading = false;
  @Input() guardando = false;
  @Input() mensajeError = '';
  @Input() fechaPartes: FechaPartesDatos = { dia: '', mes: '', anio: '' };
  @Input() diasNacimiento: number[] = [];
  @Input() aniosNacimiento: string[] = [];
  @Input() ciudadesDisponibles: string[] = [];
  @Input() longitudMaximaTelefono = 9;
  @Input() paisesCatalogo: PaisCatalogoDatos[] = [];
  @Input() generosCatalogo: string[] = [];
  @Input() mesesCatalogo: MesCatalogo[] = [];
  @Input() selectedPaisCodigo = '';

  @Output() campoChange = new EventEmitter<{ campo: string; valor: string }>();
  @Output() fechaParteChange = new EventEmitter<{ parte: 'dia' | 'mes' | 'anio'; valor: string }>();
  @Output() teclaNumericaFiltro = new EventEmitter<KeyboardEvent>();
  @Output() guardar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();

  onCampoInput(campo: string, valor: string): void {
    this.campoChange.emit({ campo, valor });
  }

  onFechaParte(parte: 'dia' | 'mes' | 'anio', valor: string): void {
    this.fechaParteChange.emit({ parte, valor });
  }

  onTeclaNumerica(event: KeyboardEvent): boolean {
    const key = event.key;
    if (key >= '0' && key <= '9') return true;
    if (['Backspace', 'Tab', 'Delete', 'ArrowLeft', 'ArrowRight', 'Home', 'End', 'Enter'].includes(event.code)) return true;
    event.preventDefault();
    return false;
  }

  onGuardar(): void {
    this.guardar.emit();
  }

  onCancelar(): void {
    this.cancelar.emit();
  }
}
