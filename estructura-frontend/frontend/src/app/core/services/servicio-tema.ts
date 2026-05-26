import { Injectable, signal, effect } from '@angular/core';

/**
 * Servicio global para gestionar el tema visual (oscuro/claro) de toda la app.
 * Persiste la preferencia del usuario en localStorage.
 */
@Injectable({ providedIn: 'root' })
export class ServicioTema {
  /** Signal reactivo: true = tema oscuro (default), false = tema claro */
  temaOscuro = signal(true);

  constructor() {
    // Leer preferencia guardada
    const guardado = localStorage.getItem('luka-tema');
    if (guardado) {
      this.temaOscuro.set(guardado === 'oscuro');
    }

    // Efecto reactivo: sincronizar clase CSS del body
    effect(() => {
      const esOscuro = this.temaOscuro();
      document.body.classList.toggle('tema-claro', !esOscuro);
      localStorage.setItem('luka-tema', esOscuro ? 'oscuro' : 'claro');
    });
  }

  /** Alternar entre tema oscuro y claro */
  alternar(): void {
    this.temaOscuro.update(v => !v);
  }
}
