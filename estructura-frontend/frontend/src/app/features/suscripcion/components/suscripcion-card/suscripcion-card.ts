import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SuscripcionDTO, CATEGORIAS_SUSCRIPCION } from '../../../../core/models/financiero/suscripcion-gasto.model';

@Component({
  selector: 'app-suscripcion-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './suscripcion-card.html',
  styleUrl: './suscripcion-card.scss'
})
export class SuscripcionCard {
  @Input() suscripcion!: SuscripcionDTO;
  @Output() editar = new EventEmitter<SuscripcionDTO>();
  @Output() eliminar = new EventEmitter<string>();
  @Output() cambiarEstado = new EventEmitter<{ id: string; estado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA' }>();
  @Output() verDetalle = new EventEmitter<SuscripcionDTO>();

  /**
   * [F-76] Detecta la marca y devuelve su paleta de colores para inyectarla en CSS
   */
  get estiloMarca() {
    const nombre = this.suscripcion.nombre.toLowerCase();
    
    // Paletas oficiales (Fondo con opacidad, Color sólido, Borde con opacidad)
    if (nombre.includes('netflix')) return { bg: 'rgba(229, 9, 20, 0.05)', color: '#E50914', border: 'rgba(229, 9, 20, 0.2)' };
    if (nombre.includes('spotify')) return { bg: 'rgba(29, 185, 84, 0.05)', color: '#1DB954', border: 'rgba(29, 185, 84, 0.2)' };
    if (nombre.includes('youtube')) return { bg: 'rgba(255, 0, 0, 0.05)', color: '#FF0000', border: 'rgba(255, 0, 0, 0.2)' };
    if (nombre.includes('prime') || nombre.includes('amazon')) return { bg: 'rgba(0, 168, 225, 0.05)', color: '#00A8E1', border: 'rgba(0, 168, 225, 0.2)' };
    if (nombre.includes('disney')) return { bg: 'rgba(17, 60, 207, 0.05)', color: '#113CCF', border: 'rgba(17, 60, 207, 0.2)' };
    if (nombre.includes('max') || nombre.includes('hbo')) return { bg: 'rgba(92, 10, 166, 0.05)', color: '#5C0AA6', border: 'rgba(92, 10, 166, 0.2)' };
    if (nombre.includes('apple')) return { bg: 'rgba(250, 36, 60, 0.05)', color: '#FA243C', border: 'rgba(250, 36, 60, 0.2)' };
    if (nombre.includes('xbox')) return { bg: 'rgba(16, 124, 16, 0.05)', color: '#107C10', border: 'rgba(16, 124, 16, 0.2)' };
    if (nombre.includes('playstation') || nombre.includes('ps plus')) return { bg: 'rgba(0, 67, 156, 0.05)', color: '#00439C', border: 'rgba(0, 67, 156, 0.2)' };
    if (nombre.includes('canva')) return { bg: 'rgba(0, 196, 204, 0.05)', color: '#00C4CC', border: 'rgba(0, 196, 204, 0.2)' };
    if (nombre.includes('chatgpt') || nombre.includes('openai')) return { bg: 'rgba(16, 163, 127, 0.05)', color: '#10A37F', border: 'rgba(16, 163, 127, 0.2)' };
    if (nombre.includes('github')) return { bg: 'rgba(110, 84, 148, 0.05)', color: '#6E5494', border: 'rgba(110, 84, 148, 0.2)' };
    if (nombre.includes('adobe')) return { bg: 'rgba(255, 0, 0, 0.05)', color: '#FF0000', border: 'rgba(255, 0, 0, 0.2)' };

    // Estilo genérico premium por defecto si no es una marca conocida
    return { bg: 'var(--bg-surface-soft)', color: 'var(--text-primary)', border: 'var(--border-color)' };
  }

  // ... (Deja el resto de tus funciones obtenerCategoriaInfo, obtenerClaseEstado, etc., exactamente igual)

  obtenerCategoriaInfo() { return CATEGORIAS_SUSCRIPCION.find(c => c.id === this.suscripcion.categoria); }
  obtenerClaseEstado(estado: string): string { switch (estado) { case 'ACTIVA': return 'is-success'; case 'PAUSADA': return 'is-warning'; case 'VENCIDA': return 'is-danger'; default: return ''; } }
  obtenerIconoEstado(estado: string): string { switch (estado) { case 'ACTIVA': return 'fa-check-circle'; case 'PAUSADA': return 'fa-pause-circle'; case 'VENCIDA': return 'fa-times-circle'; default: return ''; } }
  formatearMoneda(valor: number): string { return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', minimumFractionDigits: 2 }).format(valor); }
  onEditar() { this.editar.emit(this.suscripcion); }
  onEliminar() { this.eliminar.emit(this.suscripcion.id); }
  onVerDetalle() { this.verDetalle.emit(this.suscripcion); }
  onCambiarEstado(nuevoEstado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA') { this.cambiarEstado.emit({ id: this.suscripcion.id, estado: nuevoEstado }); }
}