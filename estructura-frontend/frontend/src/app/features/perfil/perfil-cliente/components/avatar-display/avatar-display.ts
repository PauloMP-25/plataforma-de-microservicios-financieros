import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { AvatarConfig } from '../../../../../core/services';

@Component({
  selector: 'app-avatar-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './avatar-display.html',
  styleUrl: './avatar-display.scss',
})
export class AvatarDisplay {
  readonly avatarConfig = input.required<AvatarConfig>(); //Aqui se necesita extraer el objeto del avatar
  readonly loading = input<boolean>(false);
  readonly size = input<number>(160); 

  readonly figuraSrc = computed(() => //aqui se busca la url de la figura
    this.getAssetPath('figuras', this.avatarConfig().figura) //(figura, Gato Andino)
  );

  readonly accesorioSrc = computed(() =>
    this.getAssetPath('accesorios', this.avatarConfig().accesorio ?? '') //opciona
  );

  readonly avatarSizePx = computed(() => `${this.size()}px`); 

  private getAssetPath(tipo: 'figuras' | 'accesorios', nombre: string): string { 
    return `/assets/avatares/${tipo}/${encodeURIComponent(nombre)}.png`;
  }
}

