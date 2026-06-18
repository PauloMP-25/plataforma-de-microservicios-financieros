import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PerfilFinancieroService } from '../../services/perfil-financiero.service';
import { PerfilLogrosService } from '../../services/perfil-logros.service';

@Component({
  selector: 'app-perfil-kpis',
  imports: [CommonModule],
  templateUrl: './perfil-kpis.html',
  styleUrl: './perfil-kpis.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerfilKpis {
  public service = inject(PerfilFinancieroService);
  public logrosService = inject(PerfilLogrosService);
}
