import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AvatarService } from './core/services/avatar.service';
import { ServicioTema } from './core/services/servicio-tema';
import { NotificacionCentroComponent } from './shared/components/notificacion-centro/notificacion-centro';

@Component({
  selector: 'app-root',
  standalone:true,
  imports: [RouterOutlet, NotificacionCentroComponent],
  template: `
    <router-outlet></router-outlet>
    <app-notificacion-centro></app-notificacion-centro>
  `
})
export class App {
  private readonly avatarService = inject(AvatarService);
  private readonly servicioTema = inject(ServicioTema);

  protected readonly title = signal('frontend');

  constructor() {
    this.avatarService.loadAvatar();
  }
}
