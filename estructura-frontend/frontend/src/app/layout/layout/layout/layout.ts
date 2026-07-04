import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from '../../sidebar/sidebar/sidebar';
import { Header } from '../../header/header/header';
import { SidebarStateService } from '../../../core/services/sidebar-state.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClientePerfilService } from '../../../core/services/cliente-perfil.service';
import { ModalCompletarPerfil } from '../../../features/perfil/components/completar-perfil';

@Component({
  selector: 'app-layout',
  standalone:true,
  imports: [RouterOutlet,CommonModule, RouterModule,Sidebar,Header,ModalCompletarPerfil],
  templateUrl: './layout.html',
  styleUrls: ['./layout.scss'],
})
export class Layout {
  readonly mostrarCompletarPerfil = signal(false);

  private readonly authService = inject(AuthService);
  private readonly perfilService = inject(ClientePerfilService);

  constructor(public sidebarState: SidebarStateService){
    const user = this.authService.usuario();
    if (user && user.id && !this.authService.esAdmin()) {
      this.perfilService.consultarPerfil(user.id).subscribe({
        next: (perfil) => {
          if (perfil && !perfil.datosCompletos) {
            const hasShownModal = localStorage.getItem(`luka_modal_completar_${user.id}`);
            if (!hasShownModal) {
              this.mostrarCompletarPerfil.set(true);
              localStorage.setItem(`luka_modal_completar_${user.id}`, 'true');
            }
          }
        },
        error: (err) => {
          console.warn('[Layout] Error al consultar perfil para modal de bienvenida:', err);
        }
      });
    }
  }
}
