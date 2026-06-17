import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';

@Component({
  selector: 'app-perfil-tendencia',
  imports: [CommonModule],
  templateUrl: './perfil-tendencia.html',
  styleUrl: './perfil-tendencia.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerfilTendencia {
  public service = inject(PerfilFinancieroService);
}
