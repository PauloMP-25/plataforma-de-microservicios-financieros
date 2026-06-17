import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

@Component({
  selector: 'app-perfil-planes-modal',
  imports: [CommonModule],
  templateUrl: './perfil-planes-modal.html',
  styleUrl: './perfil-planes-modal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerfilPlanesModal {
  public service = inject(PerfilFinancieroService);
}
