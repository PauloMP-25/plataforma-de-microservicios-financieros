import { Injectable } from '@angular/core';
import { RespuestaMetaAhorro, SolicitudMetaAhorro } from '../../../core/models/cliente/meta-limite.model';

export interface CategoriaMeta {
  id: string;
  nombre: string;
  icono: string;
}

@Injectable({
  providedIn: 'root'
})
export class MetasUtilityService {
  // Lista de categorías con sus íconos
  readonly categorias: CategoriaMeta[] = [
    { id: 'Viaje', nombre: 'Viaje', icono: 'fa-solid fa-plane' },
    { id: 'Vivienda', nombre: 'Vivienda', icono: 'fa-solid fa-house' },
    { id: 'Auto', nombre: 'Auto', icono: 'fa-solid fa-car' },
    { id: 'Estudios', nombre: 'Estudios', icono: 'fa-solid fa-graduation-cap' },
    { id: 'Tecnología', nombre: 'Tecnología', icono: 'fa-solid fa-laptop' },
    { id: 'Emergencia', nombre: 'Emergencia', icono: 'fa-solid fa-piggy-bank' },
    { id: 'Salud', nombre: 'Salud', icono: 'fa-solid fa-kit-medical' },
    { id: 'Inversión', nombre: 'Inversión', icono: 'fa-solid fa-chart-line' },
    { id: 'Negocio', nombre: 'Negocio', icono: 'fa-solid fa-briefcase' },
    { id: 'Otros', nombre: 'Otros', icono: 'fa-solid fa-bullseye' }
  ];

  // Mocks por defecto (sincronizados con la lista principal)
  readonly mockMetasIniciales: RespuestaMetaAhorro[] = [
    {
      id: 'mock-meta-1',
      nombre: '[Viaje] Viaje a Cancún',
      montoObjetivo: 2000,
      montoActual: 2000,
      porcentajeProgreso: 100,
      fechaLimite: '2026-11-29',
      completada: true,
      fechaCreacion: '2025-01-15',
      fechaActualizacion: '2026-11-29'
    },
    {
      id: 'mock-meta-2',
      nombre: '[Tecnología] Laptop',
      montoObjetivo: 300,
      montoActual: 300,
      porcentajeProgreso: 100,
      fechaLimite: '2025-08-15',
      completada: true,
      fechaCreacion: '2024-10-10',
      fechaActualizacion: '2025-08-15'
    },
    {
      id: 'mock-meta-3',
      nombre: '[Auto] Auto',
      montoObjetivo: 5000,
      montoActual: 1700,
      porcentajeProgreso: 34,
      fechaLimite: '2026-03-10',
      completada: false,
      fechaCreacion: '2025-02-01',
      fechaActualizacion: '2025-02-01'
    },
    {
      id: 'mock-meta-4',
      nombre: '[Estudios] Estudios',
      montoObjetivo: 5100,
      montoActual: 1700,
      porcentajeProgreso: 33,
      fechaLimite: '2027-04-20',
      completada: false,
      fechaCreacion: '2025-01-20',
      fechaActualizacion: '2025-01-20'
    },
    {
      id: 'mock-meta-5',
      nombre: '[Tecnología] Nuevo Celular',
      montoObjetivo: 1500,
      montoActual: 850,
      porcentajeProgreso: 57,
      fechaLimite: '2026-05-05',
      completada: false,
      fechaCreacion: '2025-03-01',
      fechaActualizacion: '2025-03-01'
    },
    {
      id: 'mock-meta-6',
      nombre: '[Otros] Muebles',
      montoObjetivo: 2500,
      montoActual: 250,
      porcentajeProgreso: 10,
      fechaLimite: '2025-09-20',
      completada: false,
      fechaCreacion: '2024-12-01',
      fechaActualizacion: '2024-12-01'
    }
  ];

  // Descomponer prefijo del nombre
  obtenerCategoriaYNombre(metaNombre: string): { categoria: string; nombre: string; icono: string } {
    const match = metaNombre.match(/^\[(.*?)\] (.*)$/);
    if (match) {
      const cat = match[1];
      const nom = match[2];
      return {
        categoria: cat,
        nombre: nom,
        icono: this.obtenerIconoCategoria(cat)
      };
    }
    return {
      categoria: 'Otros',
      nombre: metaNombre,
      icono: this.obtenerIconoCategoria('Otros')
    };
  }

  obtenerIconoCategoria(catId: string): string {
    const cat = this.categorias.find(c => c.id === catId);
    return cat ? cat.icono : 'fa-solid fa-bullseye';
  }

  // --- Operaciones de mock local (localStorage) ---
  obtenerListaMockActual(): RespuestaMetaAhorro[] {
    const localMetasStr = localStorage.getItem('luka_mock_metas');
    if (localMetasStr) {
      try {
        return JSON.parse(localMetasStr);
      } catch (e) {
        console.error('Error parsing mock metas', e);
      }
    }
    return [...this.mockMetasIniciales];
  }

  guardarListaMockActual(lista: RespuestaMetaAhorro[]): void {
    localStorage.setItem('luka_mock_metas', JSON.stringify(lista));
  }

  crearMockLocalmente(payload: SolicitudMetaAhorro): RespuestaMetaAhorro {
    const lista = this.obtenerListaMockActual();
    const nuevoMock: RespuestaMetaAhorro = {
      id: 'mock-meta-' + (lista.length + 1) + '-' + Math.random().toString(36).substring(2, 6),
      nombre: payload.nombre,
      montoObjetivo: payload.montoObjetivo,
      montoActual: payload.montoActual || 0,
      porcentajeProgreso: payload.montoObjetivo > 0 ? ((payload.montoActual || 0) / payload.montoObjetivo) * 100 : 0,
      fechaLimite: payload.fechaLimite,
      completada: false,
      proposito: payload.proposito,
      fechaCreacion: new Date().toISOString(),
      fechaActualizacion: new Date().toISOString()
    };
    lista.unshift(nuevoMock);
    this.guardarListaMockActual(lista);
    return nuevoMock;
  }

  actualizarMockLocalmente(id: string, payload: SolicitudMetaAhorro): void {
    let lista = this.obtenerListaMockActual();
    lista = lista.map(i => {
      if (i.id === id) {
        return {
          ...i,
          montoObjetivo: payload.montoObjetivo,
          montoActual: payload.montoActual ?? 0,
          porcentajeProgreso: payload.montoObjetivo > 0 ? ((payload.montoActual ?? 0) / payload.montoObjetivo) * 100 : 0,
          fechaLimite: payload.fechaLimite,
          fechaActualizacion: new Date().toISOString()
        };
      }
      return i;
    });
    this.guardarListaMockActual(lista);
  }

  removerMockLocalmente(id: string): void {
    let lista = this.obtenerListaMockActual();
    lista = lista.filter(m => m.id !== id);
    this.guardarListaMockActual(lista);
  }

  marcarMockComoCompletadoLocalmente(id: string, montoObjetivo: number): void {
    let lista = this.obtenerListaMockActual();
    lista = lista.map(m => {
      if (m.id === id) {
        return {
          ...m,
          montoActual: montoObjetivo,
          completada: true,
          fechaActualizacion: new Date().toISOString()
        };
      }
      return m;
    });
    this.guardarListaMockActual(lista);
  }
}
