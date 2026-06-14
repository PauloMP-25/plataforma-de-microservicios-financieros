import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { IngresosTableComponent } from '../../components/ingresos-table/ingresos-table';
import { IngresosStateService } from '../../../../core/services/ingresos-state.service';
import { IngresoRegistro } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-historial-ingresos-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, IngresosTableComponent],
  templateUrl: './historial-ingresos-page.html',
})
export class HistorialIngresosPage {
  private readonly stateService = inject(IngresosStateService);

  readonly meses = [
    { label: 'Enero', value: 1 },
    { label: 'Febrero', value: 2 },
    { label: 'Marzo', value: 3 },
    { label: 'Abril', value: 4 },
    { label: 'Mayo', value: 5 },
    { label: 'Junio', value: 6 },
    { label: 'Julio', value: 7 },
    { label: 'Agosto', value: 8 },
    { label: 'Septiembre', value: 9 },
    { label: 'Octubre', value: 10 },
    { label: 'Noviembre', value: 11 },
    { label: 'Diciembre', value: 12 },
  ];

  readonly anioActual = new Date().getFullYear();
  readonly aniosDisponibles = Array.from({ length: 5 }, (_, index) => this.anioActual - index);
  filtroMes = new Date().getMonth() + 1;
  filtroAnio = this.anioActual;
  filtroCategoriaId = '';

  readonly tablaSignal = computed<IngresoRegistro[]>(() => {
    const transacciones = this.stateService.ingresos();
    return transacciones.map(t => {
      const fecha = new Date(t.fechaTransaccion);
      return {
        id: t.id,
        fecha: fecha.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' }),
        monto: t.monto || 0,
        categoria: this.nombreCategoria(t.categoria, t.categoriaId),
        metodoPago: t.metodoPago as any || 'DIGITAL',
        etiquetas: t.etiquetas ? t.etiquetas.split(',') : [],
        nota: t.descripcion || t.notas || 'Ingreso registrado'
      };
    });
  });

  get tabla(): IngresoRegistro[] { return this.tablaSignal(); }
  get categoriasIngreso() { return this.stateService.categorias(); }
  get totalFiltrado(): number { return this.tabla.reduce((acc, row) => acc + Number(row.monto || 0), 0); }

  constructor() {
    this.stateService.cargarDatos(true, {
      mes: this.filtroMes,
      anio: this.filtroAnio,
    });
  }

  aplicarFiltrosAutomaticos(): void {
    this.stateService.cargarDatos(true, {
      mes: this.filtroMes,
      anio: this.filtroAnio,
      categoriaId: this.filtroCategoriaId || undefined,
    });
  }

  limpiarFiltros(): void {
    const hoy = new Date();
    this.filtroMes = hoy.getMonth() + 1;
    this.filtroAnio = hoy.getFullYear();
    this.filtroCategoriaId = '';
    this.aplicarFiltrosAutomaticos();
  }

  private nombreCategoria(categoria?: string | null, categoriaId?: string | null): string {
    const nombre = categoria?.trim();
    if (nombre && !this.esUid(nombre)) return nombre;

    const id = categoriaId || nombre;
    const match = this.stateService.categorias().find(cat => cat.id === id);
    return match?.nombre ?? 'Otros';
  }

  private esUid(valor: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(valor);
  }
}
