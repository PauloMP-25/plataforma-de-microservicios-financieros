import { Component, ChangeDetectionStrategy, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PerfilFinancieroService, LogroFinanciero } from '../../services/perfil-financiero.service';

@Component({
  selector: 'app-perfil-logros-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './perfil-logros-page.html',
  styleUrl: './perfil-logros-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerfilLogrosPage implements OnInit {
  public service = inject(PerfilFinancieroService);

  // ── Paginación ──────────────────────────────────────────────
  paginaActual = signal<number>(1);
  itemsPorPagina = signal<number>(8);

  logrosPaginados = computed<LogroFinanciero[]>(() => {
    const todos = this.service.logrosFinancieros();
    const inicio = (this.paginaActual() - 1) * this.itemsPorPagina();
    const fin = inicio + this.itemsPorPagina();
    return todos.slice(inicio, fin);
  });

  totalPaginas = computed<number>(() => {
    const totalItems = this.service.logrosFinancieros().length;
    return Math.ceil(totalItems / this.itemsPorPagina());
  });

  paginasArray = computed<number[]>(() => {
    const total = this.totalPaginas();
    const paginas: number[] = [];
    for (let i = 1; i <= total; i++) {
      paginas.push(i);
    }
    return paginas;
  });

  ngOnInit(): void {
    // Si no hay datos cargados en el servicio, los cargamos
    if (!this.service.resumenActual()) {
      this.service.cargarDatos();
    }
  }

  cambiarPagina(pagina: number): void {
    if (pagina >= 1 && pagina <= this.totalPaginas()) {
      this.paginaActual.set(pagina);
    }
  }

  siguientePagina(): void {
    if (this.paginaActual() < this.totalPaginas()) {
      this.paginaActual.update(p => p + 1);
    }
  }

  anteriorPagina(): void {
    if (this.paginaActual() > 1) {
      this.paginaActual.update(p => p - 1);
    }
  }
}
