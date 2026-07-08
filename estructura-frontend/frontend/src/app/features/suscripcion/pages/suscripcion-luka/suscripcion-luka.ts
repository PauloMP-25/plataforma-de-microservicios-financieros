import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SuscripcionGastosService } from '../../../../core/services/suscripcion-gastos.service';
import { SuscripcionDTO } from '../../../../core/models/financiero/suscripcion-gasto.model';
import { SuscripcionCard } from '../../components/suscripcion-card/suscripcion-card';
import { ModalNuevaSuscripcion } from '../modal-nueva-suscripcion/modal-nueva-suscripcion';

@Component({
  selector: 'app-suscripcion-luka',
  standalone: true,
  imports: [CommonModule, SuscripcionCard, ModalNuevaSuscripcion],
  templateUrl: './suscripcion-luka.html',
  styleUrl: './suscripcion-luka.scss'
})
export class SuscripcionLuka implements OnInit {
  private readonly suscripcionService = inject(SuscripcionGastosService);

  // State
  readonly modalAbierto = signal(false);
  readonly suscripcionAEditar = signal<SuscripcionDTO | null>(null);
  readonly cargando = signal(false);

  // Computed values from service
  readonly resumen = computed(() => this.suscripcionService.resumenSuscripciones());
  readonly suscripcionesProximas = computed(() => this.suscripcionService.suscripcionesProximas());
  readonly suscripcionesActivas = computed(() => this.suscripcionService.suscripcionesActivas());

  ngOnInit(): void {
    // Cargar suscripciones al inicializar
    this.suscripcionService.cargarSuscripciones().subscribe();
  }

  /**
   * Abrir modal de nueva suscripción
   */
  abrirModal(suscripcion: SuscripcionDTO | null = null): void {
    this.suscripcionAEditar.set(suscripcion);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    setTimeout(() => this.suscripcionAEditar.set(null), 300);
  }

  /**
   * Crear nueva suscripción
   */
  onCrearSuscripcion(datosFormulario: any): void {
    this.cargando.set(true);
    
    this.suscripcionService.crearSuscripcion(datosFormulario).subscribe({
      next: () => {
        this.cargando.set(false);
        this.cerrarModal();
      },
      error: (err) => {
        console.error('Error creando suscripción:', err);
        this.cargando.set(false);
      }
    });
  }

  /**
   * Editar suscripción
   */
  onEditarSuscripcion(suscripcion: SuscripcionDTO): void {
    this.abrirModal(suscripcion);
  }

  /**
   * Eliminar suscripción
   */
  onEliminarSuscripcion(id: string): void {
    if (confirm('¿Estás seguro de que deseas eliminar esta suscripción?')) {
      this.suscripcionService.eliminarSuscripcion(id).subscribe({
        next: () => {
          console.log('Suscripción eliminada');
        },
        error: (err) => {
          console.error('Error eliminando suscripción:', err);
        }
      });
    }
  }

  /**
   * Cambiar estado de suscripción
   */
  onCambiarEstado(evento: { id: string; estado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA' }): void {
    this.suscripcionService.cambiarEstado(evento.id, evento.estado).subscribe({
      next: () => {
        console.log('Estado actualizado');
      },
      error: (err) => {
        console.error('Error cambiando estado:', err);
      }
    });
  }

  /**
   * Ver detalle de suscripción
   */
  onVerDetalle(suscripcion: SuscripcionDTO): void {
    console.log('Ver detalle:', suscripcion);
  }

  onEditarSuscripcionGuardar(datosFormulario: any): void {
    this.suscripcionService.actualizarSuscripcion(datosFormulario).subscribe({
      next: () => this.cerrarModal(),
      error: (err) => console.error('Error actualizando:', err)
    });
  }

  onPagarSuscripcionManual(id: string): void {
    this.suscripcionService.cambiarEstado(id, 'ACTIVA').subscribe({
      next: () => this.cerrarModal(),
      error: (err) => console.error('Error pagando:', err)
    });
  }

  /**
   * Formatear moneda
   */
  formatearMoneda(valor: number): string {
    return new Intl.NumberFormat('en-US', { 
      style: 'currency', 
      currency: 'USD' 
    }).format(valor);
  }
}
