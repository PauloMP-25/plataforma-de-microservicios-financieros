import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SuscripcionGastosService } from '../../../../core/services/suscripcion-gastos.service';
import { SuscripcionDTO, CATEGORIAS_SUSCRIPCION, FRECUENCIAS_SUSCRIPCION, ESTADOS_SUSCRIPCION } from '../../../../core/models/financiero/suscripcion-gasto.model';
import { SuscripcionCard } from '../../components/suscripcion-card/suscripcion-card';
import { ModalNuevaSuscripcion } from '../modal-nueva-suscripcion/modal-nueva-suscripcion';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-suscripciones-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule, SuscripcionCard, ModalNuevaSuscripcion],
  templateUrl: './suscripciones-pagos.html',
  styleUrl: './suscripciones-pagos.scss'
})
export class SuscripcionesPagos implements OnInit {
  private readonly suscripcionService = inject(SuscripcionGastosService);
  public readonly authService = inject(AuthService);

  readonly modalAbierto = signal(false);

  // Filtros
  readonly busqueda = signal('');
  readonly categoriaFiltro = signal('');
  readonly frecuenciaFiltro = signal('');
  readonly estadoFiltro = signal('');
  readonly montoMin = signal('');
  readonly montoMax = signal('');

  // Ordenamiento
  readonly ordenarPor = signal<'nombre' | 'monto' | 'vencimiento'>('nombre');
  readonly ordenAscendente = signal(true);

  // Datos
  readonly suscripciones = computed(() => {
    let resultado = this.suscripcionService.filtrarSuscripciones({
      busqueda: this.busqueda(),
      categoria: this.categoriaFiltro(),
      frecuencia: this.frecuenciaFiltro() as any,
      estado: this.estadoFiltro(),
      montoMin: this.montoMin() ? parseFloat(this.montoMin()) : undefined,
      montoMax: this.montoMax() ? parseFloat(this.montoMax()) : undefined
    });

    // Ordenar
    resultado = this.ordenarSuscripciones(resultado);
    return resultado;
  });

  readonly totalResultados = computed(() => this.suscripciones().length);

  // Data para selects
  readonly categorias = CATEGORIAS_SUSCRIPCION;
  readonly frecuencias = FRECUENCIAS_SUSCRIPCION;
  readonly estados = ESTADOS_SUSCRIPCION;

  ngOnInit(): void {
    // Cargar suscripciones
  }

  // 👇 4. AGREGAMOS LAS FUNCIONES PARA CONTROLAR EL MODAL
  abrirModal(): void {
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
  }

  onCrearSuscripcion(datosFormulario: any): void {
    this.suscripcionService.crearSuscripcion(datosFormulario).subscribe({
      next: () => {
        this.cerrarModal(); // Cerramos el modal cuando se crea con éxito
      },
      error: (err) => {
        console.error('Error creando suscripción:', err);
      }
    });
  }

  /**
   * Limpiar todos los filtros
   */
  limpiarFiltros(): void {
    this.busqueda.set('');
    this.categoriaFiltro.set('');
    this.frecuenciaFiltro.set('');
    this.estadoFiltro.set('');
    this.montoMin.set('');
    this.montoMax.set('');
  }

  /**
   * Cambiar orden
   */
  cambiarOrden(campo: 'nombre' | 'monto' | 'vencimiento'): void {
    if (this.ordenarPor() === campo) {
      this.ordenAscendente.set(!this.ordenAscendente());
    } else {
      this.ordenarPor.set(campo);
      this.ordenAscendente.set(true);
    }
  }

  /**
   * Ordenar suscripciones
   */
  private ordenarSuscripciones(suscripciones: SuscripcionDTO[]): SuscripcionDTO[] {
    const copia = [...suscripciones];
    const ascendente = this.ordenAscendente();

    copia.sort((a, b) => {
      let comparacion = 0;

      switch (this.ordenarPor()) {
        case 'nombre':
          comparacion = a.nombre.localeCompare(b.nombre);
          break;
        case 'monto':
          comparacion = a.monto - b.monto;
          break;
        case 'vencimiento':
          comparacion = new Date(a.proximoVencimiento).getTime() - new Date(b.proximoVencimiento).getTime();
          break;
      }

      return ascendente ? comparacion : -comparacion;
    });

    return copia;
  }

  /**
   * Obtener categoría por ID
   */
  obtenerCategoria(id: string) {
    return this.categorias.find(c => c.id === id);
  }

  /**
   * Obtener estado por ID
   */
  obtenerEstado(id: string) {
    return this.estados.find(e => e.id === id);
  }

  /**
   * Eventos de tarjeta
   */
  onEditarSuscripcion(suscripcion: SuscripcionDTO): void {
    console.log('Editar:', suscripcion);
  }

  onEliminarSuscripcion(id: string): void {
    if (confirm('¿Eliminar esta suscripción?')) {
      this.suscripcionService.eliminarSuscripcion(id).subscribe();
    }
  }

  onCambiarEstado(evento: { id: string; estado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA' }): void {
    this.suscripcionService.cambiarEstado(evento.id, evento.estado).subscribe();
  }

  onVerDetalle(suscripcion: SuscripcionDTO): void {
    console.log('Ver detalle:', suscripcion);
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