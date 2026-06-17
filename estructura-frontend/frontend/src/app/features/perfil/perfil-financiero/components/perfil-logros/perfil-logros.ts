import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

@Component({
  selector: 'app-perfil-logros',
  imports: [CommonModule],
  templateUrl: './perfil-logros.html',
  styleUrl: './perfil-logros.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerfilLogros {
  public service = inject(PerfilFinancieroService);
}
