import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MetaBubbleComponent } from '../meta-bubble/meta-bubble.component';

@Component({
  selector: 'app-meta-details-sidebar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MetaBubbleComponent],
  styleUrl: './meta-details-sidebar.component.scss',
  template: `
    <div class="metas-page__sidebar-backdrop" (click)="close.emit()">
      <aside class="metas-page__sidebar card" (click)="$event.stopPropagation()">
        <div class="metas-page__sidebar-header">
          <h2>Detalle de la meta</h2>
          <button class="btn-close" (click)="close.emit()">
            <i class="fa-solid fa-xmark"></i>
          </button>
        </div>

        <div class="metas-page__sidebar-content">
          <!-- Icono y Nombre -->
          <div class="metas-page__sidebar-title-section">
            <div class="metas-page__sidebar-icon-circle" [ngClass]="'metas-page__sidebar-icon-circle--' + obtenerColorEstado()">
              <i [class]="meta().iconoVisual"></i>
            </div>
            <div>
              <h3>{{ meta().nombreVisual }}</h3>
              <span class="badge"
                    [class.badge-success]="meta().completada"
                    [class.badge-danger]="esVencida() && !meta().completada"
                    [class.badge-primary]="!meta().completada && !esVencida()">
                {{ obtenerTextoEstado() }}
              </span>
            </div>
          </div>

          <!-- Gráfico de Burbuja Grande -->
          <div class="metas-page__sidebar-bubble-section">
            <app-meta-bubble 
              [icono]="meta().iconoVisual" 
              [porcentaje]="meta().porcentajeProgreso" 
              [estado]="obtenerColorEstado()" 
              [color]="obtenerColorProgreso()" 
              [size]="'large'">
            </app-meta-bubble>
          </div>

          <!-- Valores Financieros y Fechas -->
          <div class="metas-page__sidebar-details-grid">
            <div class="detail-item">
              <span class="label">Monto Aplicado</span>
              <span class="value text-success">S/ {{ formatMoneda(meta().montoAplicado) }}</span>
            </div>
            <div class="detail-item">
              <span class="label">Monto Objetivo</span>
              <span class="value">S/ {{ formatMoneda(meta().montoObjetivo) }}</span>
            </div>

            <div class="detail-item-wide border-t border-b border-color py-3 my-2">
              @if (!meta().completada) {
                <div class="flex-between">
                  <span class="label">Faltan ahorrar</span>
                  <span class="value text-warning">S/ {{ formatMoneda(meta().montoObjetivo - meta().montoAplicado) }}</span>
                </div>
              }
            </div>

            @if (meta().completada) {
              <div class="detail-item">
                <span class="label"><i class="fa-solid fa-award text-success"></i> Completada el</span>
                <span class="value">{{ obtenerFechaActualizacionOCreacion() | date:'dd/MM/yyyy' }}</span>
              </div>
            }
            <div class="detail-item">
              <span class="label"><i class="fa-regular fa-calendar-days"></i> Fecha de inicio</span>
              <span class="value">{{ meta().fechaCreacion | date:'dd/MM/yyyy' }}</span>
            </div>
            <div class="detail-item">
              <span class="label"><i class="fa-regular fa-calendar-check"></i> Fecha objetivo</span>
              <span class="value">{{ meta().fechaLimite | date:'dd/MM/yyyy' }}</span>
            </div>
            <div class="detail-item">
              <span class="label"><i class="fa-regular fa-clock"></i> Categoría</span>
              <span class="value">{{ meta().categoriaVisual }}</span>
            </div>
            @if (meta().completada) {
              <div class="detail-item">
                <span class="label"><i class="fa-solid fa-hourglass-end"></i> Tiempo empleado</span>
                <span class="value">{{ obtenerTiempoEmpleadoMeta() }}</span>
              </div>
            }
          </div>

          <!-- Acciones del Sidebar -->
          <div class="metas-page__sidebar-actions">
            @if (mostrarBotonCompletar()) {
              <button type="button" class="btn-sidebar btn-sidebar--complete" (click)="complete.emit()">
                <i class="fa-solid fa-circle-check"></i> Completar Meta
              </button>
            }
            @if (mostrarBotonProgreso()) {
              <button type="button" class="btn-sidebar btn-sidebar--progress" disabled>
                <i class="fa-solid fa-spinner fa-spin"></i> En Progreso
              </button>
            }
            @if (mostrarBotonEditar()) {
              <button type="button" class="btn-sidebar btn-sidebar--edit" (click)="edit.emit()">
                <i class="fa-regular fa-pen-to-square"></i> Editar Meta
              </button>
            }
            <button type="button" class="btn-sidebar btn-sidebar--delete" (click)="delete.emit()">
              <i class="fa-regular fa-trash-can"></i> Eliminar Meta
            </button>
          </div>

          <!-- Caja de Impacto Financiero -->
          <div class="metas-page__sidebar-impact">
            <h4><i class="fa-solid fa-chart-line"></i> Impacto en tus finanzas</h4>
            @if (mostrarCajaExito()) {
              <p>Esta meta ya fue completada. El monto de S/ <strong>{{ formatMoneda(meta().montoObjetivo) }}</strong> se descontó de tu ingreso disponible y se registró como un gasto en tu historial financiero.</p>
            }
            @if (mostrarCajaProgreso()) {
              <p>Al completar esta meta se descontarán S/ <strong>{{ formatMoneda(meta().montoObjetivo) }}</strong> de tu balance disponible global y se guardará la transacción correspondiente en la categoría de gastos.</p>
            }
          </div>
        </div>
      </aside>
    </div>
  `
})
export class MetaDetailsSidebarComponent {
  meta = input.required<any>();
  disponible = input.required<number>();

  close = output<void>();
  edit = output<void>();
  delete = output<void>();
  complete = output<void>();

  esVencida(): boolean {
    const meta = this.meta();
    if (!meta.fechaLimite) return false;
    const limite = new Date(meta.fechaLimite + 'T00:00:00');
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    return limite < hoy;
  }

  obtenerColorEstado(): string {
    const meta = this.meta();
    if (meta.completada) return 'success';
    if (this.esVencida()) return 'danger';
    return 'active';
  }

  obtenerTextoEstado(): string {
    const meta = this.meta();
    if (meta.completada) return 'Cumplida';
    if (this.esVencida()) return 'Vencida';
    return 'Activa';
  }

  obtenerColorProgreso(): string {
    const meta = this.meta();
    if (meta.completada) return '#22c55e';
    if (this.esVencida()) return '#ef4444';
    return '#5b6af0';
  }

  formatMoneda(valor: number): string {
    if (valor === null || valor === undefined) return '0.00';
    return valor.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  obtenerFechaActualizacionOCreacion(): string {
    const meta = this.meta();
    return meta.fechaActualizacion || meta.fechaCreacion;
  }

  calcularTiempoEmpleado(creacion: string, actualizacion: string): string {
    const inicio = new Date(creacion);
    const fin = new Date(actualizacion);
    const difAnios = fin.getFullYear() - inicio.getFullYear();
    const difMeses = fin.getMonth() - inicio.getMonth() + (difAnios * 12);
    
    if (difMeses <= 0) {
      const difDias = Math.ceil((fin.getTime() - inicio.getTime()) / (1000 * 60 * 60 * 24));
      return `${difDias} días`;
    }
    return `${difMeses} meses`;
  }

  obtenerTiempoEmpleadoMeta(): string {
    const meta = this.meta();
    const fin = meta.fechaActualizacion || meta.fechaCreacion;
    return this.calcularTiempoEmpleado(meta.fechaCreacion, fin);
  }

  mostrarCajaExito(): boolean {
    return !!(this.meta() && this.meta().completada);
  }

  mostrarCajaProgreso(): boolean {
    return !!(this.meta() && !this.meta().completada);
  }

  mostrarBotonCompletar(): boolean {
    const meta = this.meta();
    return !!(meta && !meta.completada && meta.puedeCompletar);
  }

  mostrarBotonProgreso(): boolean {
    const meta = this.meta();
    return !!(meta && !meta.completada && !meta.puedeCompletar);
  }

  mostrarBotonEditar(): boolean {
    const meta = this.meta();
    return !!(meta && !meta.completada);
  }
}
