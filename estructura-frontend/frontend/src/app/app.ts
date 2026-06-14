import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AvatarService } from './core/services/avatar.service';
import { ServicioTema } from './core/services/servicio-tema';

@Component({
  selector: 'app-root',
  standalone:true,
  imports: [RouterOutlet],
  template: `<router-outlet></router-outlet>`
  
})
export class App {
  private readonly avatarService = inject(AvatarService);
  private readonly servicioTema = inject(ServicioTema);

  protected readonly title = signal('frontend');

  constructor() {
    this.avatarService.loadAvatar();
  }
}
