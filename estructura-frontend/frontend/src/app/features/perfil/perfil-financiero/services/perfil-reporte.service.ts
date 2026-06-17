import { Injectable, inject } from '@angular/core';
import { PerfilFinancieroService } from './perfil-financiero.service';
import { PerfilLogrosService } from './perfil-logros.service';
import { ResumenFinancieroDTO } from '../../../../core/models/financiero/resumen.model';

@Injectable({
  providedIn: 'root'
})
export class PerfilReporteService {
  private financieroService = inject(PerfilFinancieroService);
  private logrosService = inject(PerfilLogrosService);

  exportarPdf(): void {
    const resumen = this.financieroService.resumenActual();
    const nombresMeses = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    const periodoLabel = `${nombresMeses[this.financieroService.mesSeleccionado() - 1]} ${this.financieroService.anioSeleccionado()}`;
    const salud = this.financieroService.indicesSalud();
    const ahorro = this.financieroService.capacidadAhorro();
    const logros = this.logrosService.progresoLogros();
    const fechaGeneracion = new Date().toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' });
    const html = this.construirReportePdf(resumen, periodoLabel, salud, ahorro, logros, fechaGeneracion);
    const ventana = window.open('', '_blank', 'width=980,height=720');
    if (!ventana) return;

    ventana.document.open();
    ventana.document.write(html);
    ventana.document.close();
    ventana.focus();
    setTimeout(() => ventana.print(), 350);
  }

  formatMoneda(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatMonedaSinDecimales(valor: number): string {
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  }

  private construirReportePdf(
    resumen: ResumenFinancieroDTO | null,
    periodo: string,
    salud: { score: number; etiqueta: string } | null,
    ahorro: number | null,
    logros: { desbloqueados: number; total: number },
    fechaGeneracion: string
  ): string {
    const ingresos = resumen?.totalIngresos ?? 0;
    const gastos = resumen?.totalGastos ?? 0;
    const saldo = ingresos - gastos;
    const rowsTendencia = this.financieroService.tendencia().map(punto => `
      <tr><td>${punto.mes} ${punto.anio}</td><td>S/ ${this.formatMoneda(punto.ingresos)}</td><td>S/ ${this.formatMoneda(punto.gastos)}</td><td>S/ ${this.formatMoneda(punto.ahorro)}</td></tr>
    `).join('');

    return `<!doctype html>
      <html lang="es">
      <head>
        <meta charset="utf-8">
        <title>Perfil financiero - ${periodo}</title>
        <style>
          @page { size: A4; margin: 16mm; }
          body { margin: 0; font-family: Arial, sans-serif; color: #0f172a; background: #fff; }
          .hero { padding: 22px; border-radius: 18px; color: #fff; background: linear-gradient(135deg, #4f46e5, #7c3aed); }
          h1 { margin: 0 0 6px; font-size: 26px; }
          h2 { margin: 22px 0 10px; font-size: 17px; color: #312e81; }
          p { margin: 0; color: inherit; }
          .muted { color: #64748b; font-size: 12px; }
          .grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 18px 0; }
          .card { border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px; background: #f8fafc; }
          .label { display: block; color: #64748b; font-size: 11px; text-transform: uppercase; font-weight: 700; }
          .value { display: block; margin-top: 6px; font-size: 21px; font-weight: 800; }
          table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 12px; }
          th, td { border-bottom: 1px solid #e2e8f0; padding: 8px; text-align: left; }
          th { background: #eef2ff; color: #312e81; }
          .footer { margin-top: 24px; color: #64748b; font-size: 11px; }
        </style>
      </head>
      <body>
        <section class="hero">
          <h1>Reporte de Perfil Financiero</h1>
          <p>Periodo: ${periodo}</p>
          <p>Generado: ${fechaGeneracion}</p>
        </section>
        <section class="grid">
          <article class="card"><span class="label">Ingresos</span><span class="value">S/ ${this.formatMoneda(ingresos)}</span></article>
          <article class="card"><span class="label">Gastos</span><span class="value">S/ ${this.formatMoneda(gastos)}</span></article>
          <article class="card"><span class="label">Saldo operativo</span><span class="value">S/ ${this.formatMoneda(saldo)}</span></article>
          <article class="card"><span class="label">Salud financiera</span><span class="value">${salud ? `${salud.score}/100` : 'Sin datos'}</span><span class="muted">${salud?.etiqueta ?? ''}</span></article>
          <article class="card"><span class="label">Tasa de ahorro</span><span class="value">${ahorro !== null ? `${ahorro.toFixed(1)}%` : 'Sin datos'}</span></article>
          <article class="card"><span class="label">Logros</span><span class="value">${logros.desbloqueados}/${logros.total}</span></article>
        </section>
        <section>
          <h2>Tendencia financiera</h2>
          <table><thead><tr><th>Mes</th><th>Ingresos</th><th>Gastos</th><th>Ahorro</th></tr></thead><tbody>${rowsTendencia}</tbody></table>
        </section>
        <p class="footer">Reporte generado desde Luka App. Para guardar como PDF, selecciona “Guardar como PDF” en el diálogo de impresión.</p>
      </body>
      </html>`;
  }
}
