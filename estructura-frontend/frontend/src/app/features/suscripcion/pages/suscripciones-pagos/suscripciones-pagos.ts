import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SuscripcionGastosService } from '../../../../core/services/suscripcion-gastos.service';
import { SuscripcionDTO, CATEGORIAS_SUSCRIPCION, FRECUENCIAS_SUSCRIPCION, ESTADOS_SUSCRIPCION } from '../../../../core/models/financiero/suscripcion-gasto.model';
import { SuscripcionCard } from '../../components/suscripcion-card/suscripcion-card';
import { ModalNuevaSuscripcion } from '../modal-nueva-suscripcion/modal-nueva-suscripcion';
import { OnboardingTour, TourStep } from '../../components/onboarding-tour/onboarding-tour';

@Component({
  selector: 'app-suscripciones-pagos',
  standalone: true,
  imports: [CommonModule, FormsModule, SuscripcionCard, ModalNuevaSuscripcion, OnboardingTour],
  templateUrl: './suscripciones-pagos.html',
  styleUrl: './suscripciones-pagos.scss'
})
export class SuscripcionesPagos implements OnInit {
  private readonly suscripcionService = inject(SuscripcionGastosService);

  readonly modalAbierto = signal(false);
  readonly suscripcionAEditar = signal<SuscripcionDTO | null>(null);
  readonly mostrarTour = signal(false);

  readonly stepsTour: TourStep[] = [
    {
      targetSelector: '#btn-nueva-suscripcion',
      title: 'Crear Nueva Suscripción',
      description: 'Haz clic aquí para registrar un pago recurrente o suscripción. Podrás definir su costo, categoría, frecuencia y próximo vencimiento.',
      position: 'bottom'
    },
    {
      targetSelector: '#tour-filtros',
      title: 'Filtros y Búsqueda',
      description: 'Utiliza esta barra para buscar por nombre o filtrar por categoría, frecuencia o rango de precios, manteniendo el control de tus gastos.',
      position: 'bottom'
    },
    {
      targetSelector: '#tour-suscripciones-grid app-suscripcion-card:first-child, .suscripciones-pagos-page__empty',
      title: 'Tus Suscripciones',
      description: 'Aquí verás un resumen de cada suscripción con su costo y estado. Puedes editarlas, pausarlas o eliminarlas de forma directa.',
      position: 'top'
    }
  ];

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
  readonly frecuencias = FRECUENCIAS_SUSCRIPCION.filter(f => ['MENSUAL', 'ANUAL', 'QUINCENAL'].includes(f.id));
  readonly estados = ESTADOS_SUSCRIPCION;

  ngOnInit(): void {
    // Cargar suscripciones
    const tourVisto = localStorage.getItem('luka_tour_suscripciones_visto');
    if (!tourVisto) {
      // Small timeout to let DOM render completely so selectors are available
      setTimeout(() => {
        this.mostrarTour.set(true);
      }, 600);
    }
  }

  completarTour(): void {
    localStorage.setItem('luka_tour_suscripciones_visto', 'true');
    this.mostrarTour.set(false);
  }

  iniciarTourManualmente(): void {
    this.mostrarTour.set(true);
  }

  // 👇 4. AGREGAMOS LAS FUNCIONES PARA CONTROLAR EL MODAL
  abrirModal(suscripcion: SuscripcionDTO | null = null): void {
    this.suscripcionAEditar.set(suscripcion);
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    setTimeout(() => this.suscripcionAEditar.set(null), 300); // delay for animation
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

  onEditarSuscripcionGuardar(datosFormulario: any): void {
    this.suscripcionService.actualizarSuscripcion(datosFormulario).subscribe({
      next: () => {
        this.cerrarModal();
      },
      error: (err) => console.error('Error actualizando suscripción:', err)
    });
  }

  onPagarSuscripcionManual(id: string): void {
    this.suscripcionService.cambiarEstado(id, 'ACTIVA').subscribe({
      next: () => this.cerrarModal(),
      error: (err) => console.error('Error al registrar pago manual', err)
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
    this.abrirModal(suscripcion);
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