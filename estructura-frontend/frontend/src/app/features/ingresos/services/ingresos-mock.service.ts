import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import {
  DistribucionCategoria,
  IngresoFormData,
  IngresoKpi,
  IngresoReciente,
  IngresoRegistro,
  IngresoTendenciaPunto,
  MetodoPago,
  OptionItem,
} from '../types/ingresos.interfaces';

@Injectable({ providedIn: 'root' })
export class IngresosMockService {
  private readonly categorias: OptionItem[] = [
    { label: 'Salario', value: 'salario' },
    { label: 'Freelance', value: 'freelance' },
    { label: 'Inversión', value: 'inversion' },
    { label: 'Venta', value: 'venta' },
    { label: 'Bonificación', value: 'bonificacion' },
  ];

  private readonly metodos: OptionItem[] = [
    { label: 'Efectivo', value: 'EFECTIVO' },
    { label: 'Tarjeta', value: 'TARJETA' },
    { label: 'Transferencia', value: 'TRANSFERENCIA' },
    { label: 'Digital', value: 'DIGITAL' },
  ];

  private readonly kpis: IngresoKpi[] = [
    { titulo: 'Total ingresos', valor: 'S/ 3,850.00', subtitulo: 'Este mes', color: 'emerald' },
    { titulo: 'Ingresos registrados', valor: '12', subtitulo: 'Este mes', color: 'violet' },
    { titulo: 'Racha de registros', valor: '5 días', subtitulo: 'Sigue así', color: 'sky' },
    { titulo: 'Categoría principal', valor: 'Salario', subtitulo: '75% del total', color: 'amber' },
  ];

  private readonly distribucion: DistribucionCategoria[] = [
    { categoria: 'Salario', monto: 2695, porcentaje: 70, color: '#22c55e' },
    { categoria: 'Freelance', monto: 770, porcentaje: 20, color: '#7c3aed' },
    { categoria: 'Inversión', monto: 385, porcentaje: 10, color: '#f59e0b' },
  ];

  private readonly tendencia: IngresoTendenciaPunto[] = [
    { periodo: 'Ene', monto: 2100 },
    { periodo: 'Feb', monto: 2450 },
    { periodo: 'Mar', monto: 2300 },
    { periodo: 'Abr', monto: 3250 },
    { periodo: 'May', monto: 3850 },
  ];

  private readonly recientes: IngresoReciente[] = [
    { categoria: 'Salario', descripcion: 'Pago mensual de empresa ABC', monto: 1200, fecha: '31 May 2025' },
    { categoria: 'Freelance', descripcion: 'Diseño de identidad visual', monto: 450, fecha: '28 May 2025' },
    { categoria: 'Inversión', descripcion: 'Intereses de depósito', monto: 300, fecha: '25 May 2025' },
    { categoria: 'Venta', descripcion: 'Venta de artículos usados', monto: 250, fecha: '20 May 2025' },
    { categoria: 'Bonificación', descripcion: 'Bono por desempeño', monto: 200, fecha: '15 May 2025' },
  ];

  private readonly tabla: IngresoRegistro[] = [
    { fecha: '31/05/2025', monto: 1200, categoria: 'Salario', metodoPago: 'TRANSFERENCIA', etiquetas: ['Trabajo'], nota: 'Pago mensual' },
    { fecha: '28/05/2025', monto: 450, categoria: 'Freelance', metodoPago: 'TRANSFERENCIA', etiquetas: ['Proyecto'], nota: 'Diseño de logo' },
    { fecha: '25/05/2025', monto: 300, categoria: 'Inversión', metodoPago: 'TRANSFERENCIA', etiquetas: ['Inversión'], nota: 'Intereses' },
    { fecha: '20/05/2025', monto: 250, categoria: 'Venta', metodoPago: 'DIGITAL', etiquetas: ['Extra'], nota: 'Venta de artículos' },
    { fecha: '15/05/2025', monto: 200, categoria: 'Bonificación', metodoPago: 'TRANSFERENCIA', etiquetas: ['Trabajo'], nota: 'Bono por desempeño' },
  ];

  getKpis(): Observable<IngresoKpi[]> { return of(this.kpis); }
  getDistribucion(): Observable<DistribucionCategoria[]> { return of(this.distribucion); }
  getTendencia(): Observable<IngresoTendenciaPunto[]> { return of(this.tendencia); }
  getRecientes(limit = 5): Observable<IngresoReciente[]> { return of(this.recientes.slice(0, limit)); }
  getTabla(): Observable<IngresoRegistro[]> { return of(this.tabla); }
  getCategorias(): Observable<OptionItem[]> { return of(this.categorias); }
  getMetodosPago(): Observable<OptionItem[]> { return of(this.metodos); }

  guardarIngreso(payload: IngresoFormData): Observable<{ ok: boolean }> {
    console.log('Mock guardar ingreso', payload);
    return of({ ok: true });
  }

  sugerirCategoria(descripcion: string): Observable<{ categoria: string; confianza: number }> {
    const desc = descripcion.toLowerCase();
    const categoria = desc.includes('empresa') || desc.includes('mensual') ? 'Salario' : 'Freelance';
    return of({ categoria, confianza: 0.98 });
  }
}

