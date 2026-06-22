import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-historial',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './historial.html',
  styleUrls: ['./historial.scss']
})
export class Historial {
  // Filtros
  readonly busqueda = signal('');
  readonly filtroCategoria = signal('TODOS');
  readonly orden = signal('fecha_desc'); // Nuevo filtro de ordenamiento

  // Data de prueba enriquecida con más movimientos y categorías
  readonly historial = signal([
    { id: 1, nombre: 'Netflix', categoria: 'Entretenimiento', monto: -15.99, fecha: '2026-06-18', tipo: 'gasto' },
    { id: 2, nombre: 'Salario Luka', categoria: 'Trabajo', monto: 2500, fecha: '2026-06-01', tipo: 'ingreso' },
    { id: 3, nombre: 'Supermercado PlazaVea', categoria: 'Comida', monto: -120.50, fecha: '2026-06-15', tipo: 'gasto' },
    { id: 4, nombre: 'Diseño Freelance', categoria: 'Trabajo', monto: 450.00, fecha: '2026-06-10', tipo: 'ingreso' },
    { id: 5, nombre: 'Uber', categoria: 'Transporte', monto: -12.00, fecha: '2026-06-17', tipo: 'gasto' },
    { id: 6, nombre: 'Cena por aniversario', categoria: 'Comida', monto: -65.00, fecha: '2026-06-12', tipo: 'gasto' },
    { id: 7, nombre: 'Spotify', categoria: 'Entretenimiento', monto: -5.99, fecha: '2026-06-11', tipo: 'gasto' }
  ]);

  // Calcula el balance total
  readonly balance = computed(() => {
    return this.historial().reduce((acc, item) => acc + item.monto, 0);
  });

  // Lógica de filtrado Y ordenamiento (ACTUALIZADA)
  readonly historialFiltrado = computed(() => {
    // 1. Filtrado básico (texto y categoría)
    let filtrados = this.historial().filter(item => {
      const coincideBusqueda = item.nombre.toLowerCase().includes(this.busqueda().toLowerCase());
      const coincideCategoria = this.filtroCategoria() === 'TODOS' || item.categoria === this.filtroCategoria();
      return coincideBusqueda && coincideCategoria;
    });

    const tipoOrden = this.orden();

    // 2. Separar por tipo si el usuario elige un orden específico de monto
    if (tipoOrden.includes('gasto')) {
      filtrados = filtrados.filter(item => item.tipo === 'gasto');
    } else if (tipoOrden.includes('ingreso')) {
      filtrados = filtrados.filter(item => item.tipo === 'ingreso');
    }

    // 3. Ordenamiento matemático correcto
    return filtrados.sort((a, b) => {
      if (tipoOrden === 'fecha_desc') return new Date(b.fecha).getTime() - new Date(a.fecha).getTime();
      if (tipoOrden === 'fecha_asc') return new Date(a.fecha).getTime() - new Date(b.fecha).getTime();

      // Gastos (son números negativos, así que el orden se invierte)
      if (tipoOrden === 'mayor_gasto') return a.monto - b.monto; // Ej: -120 antes que -15
      if (tipoOrden === 'menor_gasto') return b.monto - a.monto; // Ej: -15 antes que -120

      // Ingresos (números positivos normales)
      if (tipoOrden === 'mayor_ingreso') return b.monto - a.monto; // Ej: 2500 antes que 450
      if (tipoOrden === 'menor_ingreso') return a.monto - b.monto; // Ej: 450 antes que 2500

      return 0;
    });
  });
}