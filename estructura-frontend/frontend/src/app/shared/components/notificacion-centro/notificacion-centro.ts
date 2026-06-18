import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificacionService } from '../../../core/services/notificacion.service';

@Component({
  selector: 'app-notificacion-centro',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificacion-centro.html',
  styleUrl: './notificacion-centro.scss'
})
export class NotificacionCentroComponent {
  protected readonly notificacionService = inject(NotificacionService);
}
