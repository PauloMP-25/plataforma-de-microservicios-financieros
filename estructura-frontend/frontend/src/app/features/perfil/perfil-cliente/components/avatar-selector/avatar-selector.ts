import { CommonModule } from '@angular/common';
import { Component, computed, effect, input, output, signal } from '@angular/core';
import { AvatarConfig } from '../../../../../core/services';

type AvatarTab = 'figura' | 'accesorio';

@Component({
  selector: 'app-avatar-selector',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './avatar-selector.html',
  styleUrl: './avatar-selector.scss',
})
export class AvatarSelector {
  readonly avatarConfig = input<AvatarConfig>({ figura: 'GATO ANDINO', accesorio: '' });
  readonly loading = input<boolean>(false);

  readonly save = output<AvatarConfig>();

  readonly tabActiva = signal<AvatarTab>('figura');
  readonly figuraSeleccionada = signal<string>('');
  readonly accesorioSeleccionado = signal<string>('');
  readonly mensajeError = signal<string>('');

  readonly figuras = signal<string[]>([
    'COLIBRI MARAVILLOSO',
    'CONDOR ANDINO',
    'DELFIN ROSADO',
    'GATO ANDINO',
    'MONO DE COLA AMARILLA',
    'OSO ANDINO',
    'PAVA ALIBLANCA',
    'RANA GIGANTE',
  ]);

  readonly accesorios = signal<string[]>(['CORBATA', 'GORRO', 'LENTES']);

  constructor() {
    effect(() => {
      const current = this.avatarConfig();
      this.figuraSeleccionada.set(current.figura);
      this.accesorioSeleccionado.set(current.accesorio ?? '');
    });
  }

  readonly configPreview = computed<AvatarConfig>(() => ({
    figura: this.figuraSeleccionada() || this.avatarConfig().figura,
    accesorio: this.accesorioSeleccionado() || this.avatarConfig().accesorio,
  }));

  cambiarTab(tab: AvatarTab): void {
    this.tabActiva.set(tab);
    this.mensajeError.set('');
  }

  seleccionarFigura(nombre: string): void {
    this.figuraSeleccionada.set(nombre);
  }

  seleccionarAccesorio(nombre: string): void {
    this.accesorioSeleccionado.set(nombre);
    this.mensajeError.set('');
  }

  getFiguraSrc(nombre: string): string {
    return `/assets/avatares/figuras/${encodeURIComponent(nombre)}.png`;
  }

  getAccesorioSrc(nombre: string): string {
    return `/assets/avatares/accesorios/${encodeURIComponent(nombre)}.png`;
  }

  guardarCambios(): void {
    if (!this.accesorioSeleccionado()) {
      this.mensajeError.set('Debes elegir un accesorio para continuar.');
      this.tabActiva.set('accesorio');
      return;
    }

    this.mensajeError.set('');
    this.save.emit({
      figura: this.figuraSeleccionada(),
      accesorio: this.accesorioSeleccionado(),
    });
  }
}

