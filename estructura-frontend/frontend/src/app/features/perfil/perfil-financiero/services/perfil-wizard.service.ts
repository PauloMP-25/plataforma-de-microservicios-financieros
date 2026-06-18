import { Injectable, inject, signal, computed } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { ClientePerfilService } from '../../../../core/services/cliente-perfil.service';
import { PerfilFinancieroService } from './perfil-financiero.service';
import { SolicitudPerfilFinanciero } from '../../../../core/models/cliente/perfil-cliente.model';

@Injectable({
  providedIn: 'root'
})
export class PerfilWizardService {
  private auth = inject(AuthService);
  private perfilService = inject(ClientePerfilService);
  private financieroService = inject(PerfilFinancieroService);

  // ── Estado ──────────────────────────────────────────────────
  modalConfigAbierto = signal<boolean>(false);
  configurando = signal<boolean>(false);
  pasoActual = signal<number>(1);

  formConfig = signal<SolicitudPerfilFinanciero>({
    ocupacion: '',
    ingresoMensual: 0,
    estiloVida: 'MODERADO',
    tonoIA: 'AMIGABLE'
  });

  erroresConfig = signal<{ [key: string]: string }>({});
  mensajeConfig = signal<{ texto: string, tipo: 'success' | 'error' } | null>(null);

  estiloVidaSliderVal = computed(() => {
    const estilo = this.formConfig().estiloVida;
    if (estilo === 'AHORRATIVO') return 1;
    if (estilo === 'MODERADO') return 2;
    if (estilo === 'GASTADOR') return 3;
    if (estilo === 'INVERSOR') return 4;
    return 2;
  });

  // ── Métodos ──────────────────────────────────────────────────
  abrirModalConfig(): void {
    const usuario = this.auth.usuario();
    if (!usuario) return;

    this.perfilService.consultarPerfilFinanciero(usuario.id).subscribe({
      next: (perfil) => {
        const totalIngresos = this.financieroService.resumenActual()?.totalIngresos ?? 0;
        const backendEstilo = (perfil.estiloVida || '').toUpperCase().trim();
        const backendTono = (perfil.tonoIA || '').toUpperCase().trim();
        
        let estiloVida = 'MODERADO';
        if (['AHORRATIVO', 'MODERADO', 'GASTADOR', 'INVERSOR'].includes(backendEstilo)) {
          estiloVida = backendEstilo;
        }

        let tonoIA = 'AMIGABLE';
        if (['FORMAL', 'AMIGABLE', 'MOTIVADOR', 'DIRECTO'].includes(backendTono)) {
          tonoIA = backendTono;
        }

        this.formConfig.set({
          ocupacion: perfil.ocupacion || '',
          ingresoMensual: totalIngresos,
          estiloVida: estiloVida,
          tonoIA: tonoIA
        });
        this.pasoActual.set(1);
        this.modalConfigAbierto.set(true);
      },
      error: () => {
        const totalIngresos = this.financieroService.resumenActual()?.totalIngresos ?? 0;
        this.formConfig.set({
          ocupacion: '',
          ingresoMensual: totalIngresos,
          estiloVida: 'MODERADO',
          tonoIA: 'AMIGABLE'
        });
        this.pasoActual.set(1);
        this.modalConfigAbierto.set(true);
      }
    });
  }

  cerrarModalConfig(): void {
    this.modalConfigAbierto.set(false);
    this.erroresConfig.set({});
    this.mensajeConfig.set(null);
    this.pasoActual.set(1);
  }

  onEstiloVidaSliderChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    const val = Number(target.value);
    let estilo = 'MODERADO';
    if (val === 1) estilo = 'AHORRATIVO';
    else if (val === 2) estilo = 'MODERADO';
    else if (val === 3) estilo = 'GASTADOR';
    else if (val === 4) estilo = 'INVERSOR';
    this.actualizarCampoConfig('estiloVida', estilo);
  }

  seleccionarTonoIA(tono: string): void {
    this.actualizarCampoConfig('tonoIA', tono);
  }

  avanzarPaso(): void {
    if (this.pasoActual() === 1) {
      const data = this.formConfig();
      const errores = { ...this.erroresConfig() };
      let hasError = false;

      if (!data.ocupacion || data.ocupacion.trim().length < 3) {
        errores['ocupacion'] = 'La ocupación debe tener al menos 3 caracteres.';
        hasError = true;
      } else {
        delete errores['ocupacion'];
      }

      this.erroresConfig.set(errores);
      if (hasError) return;
    }

    if (this.pasoActual() < 3) {
      this.pasoActual.update(p => p + 1);
    }
  }

  retrocederPaso(): void {
    if (this.pasoActual() > 1) {
      this.pasoActual.update(p => p - 1);
    }
  }

  actualizarCampoConfig(campo: keyof SolicitudPerfilFinanciero, valor: string | number): void {
    this.formConfig.update(f => ({ ...f, [campo]: valor }));
    const errs = { ...this.erroresConfig() };
    delete errs[campo];
    this.erroresConfig.set(errs);
  }

  validarFormConfig(): boolean {
    const data = this.formConfig();
    const errores: { [key: string]: string } = {};

    if (!data.ocupacion || data.ocupacion.trim().length < 3) {
      errores['ocupacion'] = 'La ocupación debe tener al menos 3 caracteres.';
    }
    if (data.ingresoMensual === null || data.ingresoMensual === undefined || data.ingresoMensual < 0) {
      errores['ingresoMensual'] = 'El ingreso mensual no puede ser negativo.';
    }
    if (!data.estiloVida) {
      errores['estiloVida'] = 'Selecciona un estilo de vida.';
    }
    if (!data.tonoIA) {
      errores['tonoIA'] = 'Selecciona un tono para la IA.';
    }

    this.erroresConfig.set(errores);
    return Object.keys(errores).length === 0;
  }

  guardarConfiguracion(): void {
    if (!this.validarFormConfig()) return;

    const usuario = this.auth.usuario();
    if (!usuario) return;

    this.configurando.set(true);
    this.mensajeConfig.set(null);

    this.perfilService.guardarPerfilFinanciero(usuario.id, this.formConfig()).subscribe({
      next: () => {
        this.configurando.set(false);
        this.mensajeConfig.set({ texto: 'Perfil financiero actualizado exitosamente.', tipo: 'success' });
        setTimeout(() => this.cerrarModalConfig(), 2000);
      },
      error: () => {
        this.configurando.set(false);
        this.mensajeConfig.set({ texto: 'Ocurrió un error al guardar. Intenta de nuevo.', tipo: 'error' });
      }
    });
  }
}
