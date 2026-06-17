import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppEventBus } from '../../../core/services/app-event-bus.service';
import { PerfilFinancieroService } from './services/perfil-financiero.service';

// Subcomponentes Standalone
import { PerfilKpis } from './components/perfil-kpis/perfil-kpis';
import { PerfilLogros } from './components/perfil-logros/perfil-logros';
import { PerfilTendencia } from './components/perfil-tendencia/perfil-tendencia';
import { PerfilWizardModal } from './components/perfil-wizard-modal/perfil-wizard-modal';
import { PerfilPlanesModal } from './components/perfil-planes-modal/perfil-planes-modal';

@Component({
  selector: 'app-perfil-financiero',
  imports: [
    CommonModule,
    FormsModule,
    PerfilKpis,
    PerfilLogros,
    PerfilTendencia,
    PerfilWizardModal,
    PerfilPlanesModal
  ],
  providers: [PerfilFinancieroService],
  templateUrl: './perfil-financiero.html',
  styleUrl: './perfil-financiero.scss',
})
export class PerfilFinanciero implements OnInit {
  public service = inject(PerfilFinancieroService);
  private eventBus = inject(AppEventBus);
  private destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.service.cargarDatos();

    // Escuchar cambios en transacciones para actualizar datos
    this.eventBus.on('TRANSACTION_MODIFIED')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.service.cargarDatos());
  }

  onMesChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.service.mesSeleccionado.set(Number(select.value));
    this.service.cargarDatos();
  }

  onAnioChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.service.anioSeleccionado.set(Number(select.value));
    this.service.cargarDatos();
  }
}
