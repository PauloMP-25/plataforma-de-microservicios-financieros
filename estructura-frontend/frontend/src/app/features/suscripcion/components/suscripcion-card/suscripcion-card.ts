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

  get estiloMarca() {
    const nombre = this.suscripcion.nombre.toLowerCase();
    
    // Paletas oficiales (Fondo con opacidad, Color sólido, Borde con opacidad)
    if (nombre.includes('netflix')) return { '--brand-bg': 'rgba(229, 9, 20, 0.1)', '--brand-color': '#e50914', '--brand-border': 'rgba(229, 9, 20, 0.3)' };
    if (nombre.includes('spotify')) return { '--brand-bg': 'rgba(29, 185, 84, 0.1)', '--brand-color': '#1db954', '--brand-border': 'rgba(29, 185, 84, 0.3)' };
    if (nombre.includes('youtube')) return { '--brand-bg': 'rgba(255, 0, 0, 0.1)', '--brand-color': '#ff0000', '--brand-border': 'rgba(255, 0, 0, 0.3)' };
    if (nombre.includes('prime') || nombre.includes('amazon')) return { '--brand-bg': 'rgba(0, 168, 225, 0.1)', '--brand-color': '#00a8e1', '--brand-border': 'rgba(0, 168, 225, 0.3)' };
    if (nombre.includes('disney')) return { '--brand-bg': 'rgba(17, 60, 207, 0.1)', '--brand-color': '#113ccf', '--brand-border': 'rgba(17, 60, 207, 0.3)' };
    if (nombre.includes('max') || nombre.includes('hbo')) return { '--brand-bg': 'rgba(92, 10, 166, 0.1)', '--brand-color': '#5c0aa6', '--brand-border': 'rgba(92, 10, 166, 0.3)' };
    if (nombre.includes('apple')) return { '--brand-bg': 'rgba(250, 36, 60, 0.1)', '--brand-color': '#fa243c', '--brand-border': 'rgba(250, 36, 60, 0.3)' };
    if (nombre.includes('xbox')) return { '--brand-bg': 'rgba(16, 124, 16, 0.1)', '--brand-color': '#107c10', '--brand-border': 'rgba(16, 124, 16, 0.3)' };
    if (nombre.includes('playstation') || nombre.includes('ps plus')) return { '--brand-bg': 'rgba(0, 67, 156, 0.1)', '--brand-color': '#00439c', '--brand-border': 'rgba(0, 67, 156, 0.3)' };
    if (nombre.includes('canva')) return { '--brand-bg': 'rgba(0, 196, 204, 0.1)', '--brand-color': '#00c4cc', '--brand-border': 'rgba(0, 196, 204, 0.3)' };
    if (nombre.includes('chatgpt') || nombre.includes('openai')) return { '--brand-bg': 'rgba(16, 163, 127, 0.1)', '--brand-color': '#10a37f', '--brand-border': 'rgba(16, 163, 127, 0.3)' };
    if (nombre.includes('github')) return { '--brand-bg': 'rgba(110, 84, 148, 0.1)', '--brand-color': '#6e5494', '--brand-border': 'rgba(110, 84, 148, 0.3)' };
    if (nombre.includes('adobe')) return { '--brand-bg': 'rgba(255, 0, 0, 0.1)', '--brand-color': '#ff0000', '--brand-border': 'rgba(255, 0, 0, 0.3)' };

    // Estilo genérico premium por defecto si no es una marca conocida
    return { '--brand-bg': 'var(--bg-surface-soft)', '--brand-color': 'var(--text-primary)', '--brand-border': 'var(--border-color)' };
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