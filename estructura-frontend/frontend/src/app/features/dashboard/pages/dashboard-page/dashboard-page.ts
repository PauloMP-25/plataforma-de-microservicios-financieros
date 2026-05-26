import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface Transaccion {
  id: number;
  fecha: string;
  descripcion: string;
  categoria: string;
  tipo: 'ingreso' | 'gasto';
  monto: number;
  estado: 'Completed' | 'Pending' | 'Failed';
  icono: string;
}

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard-page.html',
  styleUrls: ['./dashboard-page.scss']
})
export class DashboardPage implements OnInit {
  // Configuración de Filtros
  filtroTiempo: string = '30';
  filtroTipo: string = 'todos';
  terminoBusqueda: string = '';

  // Datos Iniciales
  transacciones: Transaccion[] = [
    { id: 1, fecha: '2026-05-19', descripcion: 'Suscripción Netflix Premium', categoria: 'leisure', tipo: 'gasto', monto: 18.99, estado: 'Completed', icono: 'fa-tv' },
    { id: 2, fecha: '2026-05-18', descripcion: 'Salario mensual Luka Inc', categoria: 'salario', tipo: 'ingreso', monto: 3500.00, estado: 'Completed', icono: 'fa-briefcase' },
    { id: 3, fecha: '2026-05-18', descripcion: 'Compra Supermercado Metro', categoria: 'food', tipo: 'gasto', monto: 145.50, estado: 'Completed', icono: 'fa-shopping-basket' },
    { id: 4, fecha: '2026-05-17', descripcion: 'Suscripción AWS Cloud Hosting', categoria: 'SaaS', tipo: 'gasto', monto: 320.00, estado: 'Pending', icono: 'fa-server' },
    { id: 5, fecha: '2026-05-16', descripcion: 'Dividendo Acciones Apple', categoria: 'inversiones', tipo: 'ingreso', monto: 125.00, estado: 'Completed', icono: 'fa-chart-line' },
    { id: 6, fecha: '2026-05-15', descripcion: 'Mensualidad Gimnasio SmartFit', categoria: 'health', tipo: 'gasto', monto: 45.00, estado: 'Completed', icono: 'fa-dumbbell' },
    { id: 7, fecha: '2026-05-14', descripcion: 'Transferencia fallida Bizum', categoria: 'transferencia', tipo: 'gasto', monto: 50.00, estado: 'Failed', icono: 'fa-exchange-alt' },
    { id: 8, fecha: '2026-05-12', descripcion: 'Freelance Frontend Angular UI', categoria: 'salario', tipo: 'ingreso', monto: 850.00, estado: 'Completed', icono: 'fa-laptop-code' },
    { id: 9, fecha: '2026-05-08', descripcion: 'Cena Restaurante La Mar', categoria: 'food', tipo: 'gasto', monto: 85.00, estado: 'Completed', icono: 'fa-utensils' },
    { id: 10, fecha: '2026-05-04', descripcion: 'Suscripción Spotify Duo', categoria: 'leisure', tipo: 'gasto', monto: 9.99, estado: 'Completed', icono: 'fa-music' }
  ];

  // Datos del Gráfico de Flujo de Caja
  flujoCajaPuntos: { mes: string; ingresos: number; gastos: number }[] = [
    { mes: 'Ene', ingresos: 2800, gastos: 1900 },
    { mes: 'Feb', ingresos: 3200, gastos: 2100 },
    { mes: 'Mar', ingresos: 2900, gastos: 1750 },
    { mes: 'Abr', ingresos: 4100, gastos: 2300 },
    { mes: 'May', ingresos: 4475, gastos: 674 } // basado en datos de la lista
  ];

  ngOnInit(): void {
    // Inicializaciones adicionales si fueran necesarias
  }

  // Filtrado de transacciones
  get transaccionesFiltradas(): Transaccion[] {
    return this.transacciones.filter(t => {
      // 1. Filtro de tipo
      if (this.filtroTipo !== 'todos' && t.tipo !== this.filtroTipo) {
        return false;
      }

      // 2. Filtro de búsqueda por término
      if (this.terminoBusqueda) {
        const query = this.terminoBusqueda.toLowerCase();
        const coincideDesc = t.descripcion.toLowerCase().includes(query);
        const coincideCat = t.categoria.toLowerCase().includes(query);
        if (!coincideDesc && !coincideCat) {
          return false;
        }
      }

      // 3. Filtro por tiempo (días desde hoy)
      if (this.filtroTiempo !== 'todos') {
        const limiteDias = parseInt(this.filtroTiempo, 10);
        const fechaTransaccion = new Date(t.fecha);
        const fechaLimite = new Date();
        fechaLimite.setDate(fechaLimite.getDate() - limiteDias);
        if (fechaTransaccion < fechaLimite) {
          return false;
        }
      }

      return true;
    });
  }

  // Cálculos dinámicos de KPIs basados en la lista de transacciones completa (o filtrada)
  get totalBalance(): number {
    // Asumimos un balance base histórico de $145,000 + la suma de ingresos - gastos visibles
    const base = 145000;
    const balanceTransacciones = this.transacciones.reduce((acc, t) => {
      if (t.estado === 'Failed') return acc;
      return t.tipo === 'ingreso' ? acc + t.monto : acc - t.monto;
    }, 0);
    return base + balanceTransacciones;
  }

  get totalIngresos(): number {
    return this.transacciones
      .filter(t => t.tipo === 'ingreso' && t.estado === 'Completed')
      .reduce((acc, t) => acc + t.monto, 0);
  }

  get totalGastos(): number {
    return this.transacciones
      .filter(t => t.tipo === 'gasto' && t.estado !== 'Failed')
      .reduce((acc, t) => acc + t.monto, 0);
  }

  get tasaAhorro(): number {
    const ingresos = this.totalIngresos;
    const gastos = this.totalGastos;
    if (ingresos === 0) return 0;
    return ((ingresos - gastos) / ingresos) * 100;
  }

  // Distribución de gastos por categoría (agrupado para la dona SVG)
  get distribucionCategorias(): { categoria: string; total: number; porcentaje: number; color: string }[] {
    const totales: { [key: string]: number } = {};
    let totalGastado = 0;

    const gastos = this.transacciones.filter(t => t.tipo === 'gasto' && t.estado !== 'Failed');

    gastos.forEach(g => {
      totales[g.categoria] = (totales[g.categoria] || 0) + g.monto;
      totalGastado += g.monto;
    });

    const colores: { [key: string]: string } = {
      food: '#FF7043',       // Naranja
      transport: '#42A5F5',  // Azul
      leisure: '#AB47BC',    // Púrpura
      health: '#26C6DA',     // Cyan
      SaaS: '#5B6AF0',       // Indigo
      inversiones: '#10B981', // Verde
      transferencia: '#F59E0B' // Amarillo
    };

    return Object.keys(totales).map(cat => {
      const total = totales[cat];
      const porcentaje = totalGastado > 0 ? (total / totalGastado) * 100 : 0;
      return {
        categoria: cat.charAt(0).toUpperCase() + cat.slice(1),
        total,
        porcentaje,
        color: colores[cat] || '#859397'
      };
    }).sort((a, b) => b.total - a.total);
  }

  // Helper para mostrar colores de categoría amigables
  getColorCategoria(cat: string): string {
    const colores: { [key: string]: string } = {
      food: 'var(--color-food)',
      transport: 'var(--color-transport)',
      leisure: 'var(--color-leisure)',
      health: 'var(--color-health)',
      SaaS: 'var(--color-primary)',
      inversiones: 'var(--color-success)',
      transferencia: 'var(--color-warning)',
      salario: 'var(--color-success)'
    };
    return colores[cat] || 'var(--text-muted)';
  }

  // Formateador de moneda
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
  }
}
