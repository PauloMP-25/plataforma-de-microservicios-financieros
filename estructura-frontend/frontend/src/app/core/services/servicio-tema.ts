import { Injectable, signal, effect } from '@angular/core';

/**
 * Servicio global para gestionar el tema visual (oscuro/claro) de toda la app.
 * Persiste la preferencia del usuario en localStorage.
 */
@Injectable({ providedIn: 'root' })
export class ServicioTema {
  private readonly STORAGE_KEY = 'luka-theme';
  /** Signal reactivo: true = tema oscuro, false = tema claro */
  temaOscuro = signal(false);

  constructor() {
    // Leer preferencia guardada
    const guardado = localStorage.getItem(this.STORAGE_KEY);
    // Migración de clave legacy
    const legacy = localStorage.getItem('luka-tema');
    if (!guardado && legacy) {
      localStorage.setItem(this.STORAGE_KEY, legacy);
      localStorage.removeItem('luka-tema');
    }

    const temaGuardado = localStorage.getItem(this.STORAGE_KEY);
    console.debug('[TemaDebug][ServicioTema] localStorage luka-theme =', temaGuardado);
    if (temaGuardado) {
      this.temaOscuro.set(temaGuardado === 'oscuro');
    }

    // Efecto reactivo: sincronizar clase CSS del body
    effect(() => {
      const esOscuro = this.temaOscuro();
      document.body.classList.toggle('theme-dark', esOscuro);
      document.body.classList.toggle('dark', esOscuro);
      localStorage.setItem(this.STORAGE_KEY, esOscuro ? 'oscuro' : 'claro');
      console.debug('[TemaDebug][ServicioTema] apply =>', {
        esOscuro,
        bodyThemeDark: document.body.classList.contains('theme-dark'),
        bodyDark: document.body.classList.contains('dark'),
        persisted: localStorage.getItem(this.STORAGE_KEY)
      });
    });
  }

  /** Alternar entre tema oscuro y claro */
  alternar(): void {
    this.temaOscuro.update(v => !v);
  }

  setTema(tema: 'oscuro' | 'claro'): void {
    this.temaOscuro.set(tema === 'oscuro');
  }
}
