// =============================================
// LUKA - Sidebar State Service
// Maneja el estado collapsed/expanded del sidebar.
// Lo leen tanto Sidebar como Layout para sincronizarse.
// =============================================

import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SidebarStateService {

  private readonly STORAGE_KEY = 'luka_sidebar_collapsed';

  // ── Estado interno ──
  private _collapsed = signal<boolean>(this.loadFromStorage());
  private _mobileOpen = signal<boolean>(false);

  // ── Público (solo lectura) ──
  collapsed  = this._collapsed.asReadonly();
  expanded   = computed(() => !this._collapsed());
  mobileOpen = this._mobileOpen.asReadonly();

  // ── Acciones ──
  toggle(): void {
    this._collapsed.update(v => !v);
    localStorage.setItem(this.STORAGE_KEY, String(this._collapsed()));
  }

  collapse(): void {
    this._collapsed.set(true);
    localStorage.setItem(this.STORAGE_KEY, 'true');
  }

  expand(): void {
    this._collapsed.set(false);
    localStorage.setItem(this.STORAGE_KEY, 'false');
  }

  toggleMobile(): void {
    this._mobileOpen.update(v => !v);
  }

  openMobile(): void {
    this._mobileOpen.set(true);
  }

  closeMobile(): void {
    this._mobileOpen.set(false);
  }

  // ── Privado ──
  private loadFromStorage(): boolean {
    return localStorage.getItem(this.STORAGE_KEY) === 'true';
  }
}
