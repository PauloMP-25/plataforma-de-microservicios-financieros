import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IngresoRegistro } from '../../types/ingresos.interfaces';

@Component({
  selector: 'app-ingresos-table',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ingresos-table.html',
})
export class IngresosTableComponent {
  @Input() rows: IngresoRegistro[] = [];
  modal: 'view' | 'edit' | null = null;
  selected: IngresoRegistro | null = null;

  categoriasDisponibles: string[] = ['Salario', 'Freelance', 'Inversiones', 'Ventas', 'Bonificaciones'];

  openView(row: IngresoRegistro): void {
    this.selected = { ...row, etiquetas: [...row.etiquetas] };
    this.modal = 'view';
  }

  openEdit(row: IngresoRegistro): void {
    this.selected = { ...row, etiquetas: [...row.etiquetas] };
    this.modal = 'edit';
  }

  closeModal(): void {
    this.modal = null;
    this.selected = null;
  }

  saveEdit(): void {
    this.closeModal();
  }

  imprimir(): void {
    if (!this.selected) return;

    const boleta = this.selected;
    const etiquetas = boleta.etiquetas?.join(', ') || '—';
    const html = `
      <!doctype html>
      <html lang="es">
        <head>
          <meta charset="utf-8" />
          <title>Boleta de ingreso</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 24px; color: #0f172a; }
            .ticket { max-width: 480px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; }
            .title { font-size: 20px; font-weight: 800; margin-bottom: 12px; color: #4f46e5; }
            .row { display: flex; justify-content: space-between; gap: 12px; padding: 8px 0; border-bottom: 1px dashed #e2e8f0; }
            .row:last-child { border-bottom: 0; }
            .label { font-weight: 700; color: #334155; }
            .value { text-align: right; color: #0f172a; }
            .amount { font-size: 22px; font-weight: 800; color: #059669; }
            .footer { margin-top: 14px; font-size: 12px; color: #64748b; text-align: center; }
          </style>
        </head>
        <body>
          <section class="ticket">
            <div class="title">Boleta de ingreso</div>
            <div class="row"><span class="label">Fecha</span><span class="value">${boleta.fecha}</span></div>
            <div class="row"><span class="label">Monto</span><span class="value amount">S/ ${Number(boleta.monto).toFixed(2)}</span></div>
            <div class="row"><span class="label">Categoría</span><span class="value">${boleta.categoria}</span></div>
            <div class="row"><span class="label">Método</span><span class="value">${boleta.metodoPago}</span></div>
            <div class="row"><span class="label">Etiquetas</span><span class="value">${etiquetas}</span></div>
            <div class="row"><span class="label">Nota</span><span class="value">${boleta.nota || '—'}</span></div>
            <div class="footer">Luka App · Comprobante generado desde módulo Ingresos</div>
          </section>
          <script>
            window.onload = function() {
              window.print();
              window.onafterprint = function() { window.close(); };
            };
          </script>
        </body>
      </html>
    `;

    const printWindow = window.open('', '_blank', 'width=700,height=900');
    if (!printWindow) return;
    printWindow.document.open();
    printWindow.document.write(html);
    printWindow.document.close();
  }

  categoriaIconClass(categoria: string): string {
    const c = categoria.toLowerCase();
    if (c.includes('salario')) return 'fa-solid fa-briefcase text-emerald-600';
    if (c.includes('freelance')) return 'fa-regular fa-user text-violet-600';
    if (c.includes('invers')) return 'fa-solid fa-arrow-trend-up text-amber-500';
    if (c.includes('venta')) return 'fa-solid fa-cart-shopping text-blue-600';
    if (c.includes('bonif')) return 'fa-solid fa-gift text-cyan-600';
    return 'fa-solid fa-coins text-slate-600';
  }

  metodoIconClass(metodo: string): string {
    const m = metodo.toLowerCase();
    if (m.includes('transfer')) return 'fa-solid fa-building-columns text-slate-500';
    if (m.includes('tarjeta')) return 'fa-regular fa-credit-card text-slate-500';
    if (m.includes('efectivo')) return 'fa-solid fa-money-bill-wave text-slate-500';
    if (m.includes('digital') || m.includes('yape')) return 'fa-solid fa-mobile-screen text-violet-500';
    return 'fa-solid fa-wallet text-slate-500';
  }

  etiquetaClass(tag: string): string {
    const t = tag.toLowerCase();
    if (t.includes('trabajo')) return 'border-emerald-200 bg-emerald-100 text-emerald-700 dark:border-emerald-500/40 dark:bg-emerald-500/20 dark:text-emerald-300';
    if (t.includes('proyecto')) return 'border-violet-200 bg-violet-100 text-violet-700 dark:border-violet-500/40 dark:bg-violet-500/20 dark:text-violet-300';
    if (t.includes('invers')) return 'border-indigo-200 bg-indigo-100 text-indigo-700 dark:border-indigo-500/40 dark:bg-indigo-500/20 dark:text-indigo-300';
    if (t.includes('extra')) return 'border-amber-200 bg-amber-100 text-amber-700 dark:border-amber-500/40 dark:bg-amber-500/20 dark:text-amber-300';
    return 'border-slate-200 bg-slate-100 text-slate-700 dark:border-slate-500/40 dark:bg-slate-500/20 dark:text-slate-300';
  }
}

