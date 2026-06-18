import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PerfilWizardService } from '../../services/perfil-wizard.service';

@Component({
  selector: 'app-perfil-wizard-modal',
  imports: [CommonModule, FormsModule],
  templateUrl: './perfil-wizard-modal.html',
  styleUrl: './perfil-wizard-modal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerfilWizardModal {
  public service = inject(PerfilWizardService);
}
