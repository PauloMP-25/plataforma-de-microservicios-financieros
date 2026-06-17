import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
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
  private router = inject(Router);

  irALogros(): void {
    this.router.navigate(['/perfil/financiero/logros']);
  }
}
