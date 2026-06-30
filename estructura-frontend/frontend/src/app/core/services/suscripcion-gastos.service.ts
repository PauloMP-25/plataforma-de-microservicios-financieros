import { Injectable, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import { AuthService } from './auth.service';
import { ResultadoApi } from '../models/auth/user.model';
import { 
  SuscripcionDTO, 
  SuscripcionGasto, 
  CrearSuscripcionRequest,
  ActualizarSuscripcionRequest,
  ResumenSuscripciones,
  FrecuenciaSuscripcion,
  EstadoSuscripcion
} from '../models/financiero/suscripcion-gasto.model';

/**
 * ServicioSuscripcionGastos
 * Gestiona las suscripciones recurrentes (Netflix, Spotify, Gimnasio, etc.)
 */
@Injectable({ providedIn: 'root' })
export class SuscripcionGastosService {
  private readonly baseUrl = `${environment.gatewayUrl}/api/v1/suscripciones`;

  // Estado local con signals
  private suscripciones = signal<SuscripcionDTO[]>([]);
  private cargando = signal(false);
  private error = signal<string | null>(null);

  // Computed signals
  readonly suscripcionesActivas = computed(() =>
    this.suscripciones().filter(s => s.estado === 'ACTIVA')
  );

  readonly suscripcionesPausadas = computed(() =>
    this.suscripciones().filter(s => s.estado === 'PAUSADA')
  );

  readonly suscripcionesProximas = computed(() => {
    const hoy = new Date();
    const proximas30 = new Date(hoy.getTime() + 30 * 24 * 60 * 60 * 1000);
    
    return this.suscripciones()
      .filter(s => {
        const vencimiento = new Date(s.proximoVencimiento);
        return vencimiento >= hoy && vencimiento <= proximas30 && s.estado === 'ACTIVA';
      })
      .sort((a, b) => new Date(a.proximoVencimiento).getTime() - new Date(b.proximoVencimiento).getTime());
  });

  readonly proximoPago = computed(() => this.suscripcionesProximas()[0] || null);

  readonly gastoMensualEstimado = computed(() => {
    return this.suscripcionesActivas().reduce((total, suscripcion) => {
      const gastosAlMes = this.calcularGastosAlMes(suscripcion.monto, suscripcion.frecuencia);
      return total + gastosAlMes;
    }, 0);
  });

  readonly resumenSuscripciones = computed<ResumenSuscripciones>(() => ({
    totalActivas: this.suscripcionesActivas().length,
    gastoMensualEstimado: this.gastoMensualEstimado(),
    proximoPago: this.proximoPago(),
    cantidadTotal: this.suscripciones().length,
    proximasFechas: this.suscripcionesProximas()
  }));

  constructor(private http: HttpClient, private auth: AuthService) {
    if (this.auth.usuario()?.id) {
      this.cargarSuscripciones().subscribe();
    }
  }

  /**
   * Cargar todas las suscripciones
   */
  cargarSuscripciones(): Observable<SuscripcionDTO[]> {
    this.cargando.set(true);
    this.error.set(null);
    const usuarioId = this.auth.usuario()?.id;
    if (!usuarioId) {
      this.cargando.set(false);
      return of(this.suscripciones());
    }

    return this.http.get<ResultadoApi<any[]>>(`${this.baseUrl}/usuario/${usuarioId}`).pipe(
      map(res => {
        const backendData = res.datos || [];
        const mapped = backendData.map(s => this.mapearDesdeBackend(s));
        this.suscripciones.set(mapped);
        this.cargando.set(false);
        return mapped;
      }),
      catchError(err => {
        console.warn('[SuscripcionService] Error cargando suscripciones desde el backend, usando fallback local:', err);
        this.cargando.set(false);
        if (this.suscripciones().length === 0) {
          this.cargarSuscripcionesMock();
        }
        return of(this.suscripciones());
      })
    );
  }

  /**
   * Obtener suscripción por ID
   */
  obtenerSuscripcion(id: string): Observable<SuscripcionDTO | undefined> {
    return this.http.get<ResultadoApi<any>>(`${this.baseUrl}/${id}`).pipe(
      map(res => this.mapearDesdeBackend(res.datos)),
      catchError(err => {
        console.warn(`[SuscripcionService] Error cargando suscripción ${id} desde el backend, buscando en local:`, err);
        return of(this.suscripciones().find(s => s.id === id));
      })
    );
  }

  /**
   * Crear nueva suscripción
   */
  crearSuscripcion(request: CrearSuscripcionRequest): Observable<SuscripcionDTO> {
    const usuarioId = this.auth.usuario()?.id;
    const proximoVencimiento = this.calcularProximoVencimiento(request.fechaInicio, request.frecuencia);
    
    if (!usuarioId) {
      return this.crearSuscripcionMock(request);
    }

    const body = {
      usuarioId,
      nombre: request.nombre,
      monto: request.monto,
      metodoPago: 'MANUAL',
      tipoEstrategia: request.frecuencia === 'MENSUAL' ? 'CALENDARIO' : request.frecuencia,
      fechaInicio: request.fechaInicio,
      fechaVencimiento: proximoVencimiento
    };

    return this.http.post<ResultadoApi<any>>(`${this.baseUrl}`, body).pipe(
      map(res => {
        const creada = this.mapearDesdeBackend(res.datos);
        this.suscripciones.update(sus => [...sus, creada]);
        return creada;
      }),
      catchError(err => {
        console.warn('[SuscripcionService] Error al crear en backend, usando mock:', err);
        return this.crearSuscripcionMock(request);
      })
    );
  }

  /**
   * Actualizar suscripción existente
   */
  actualizarSuscripcion(request: ActualizarSuscripcionRequest): Observable<SuscripcionDTO> {
    const usuarioId = this.auth.usuario()?.id;
    if (!usuarioId) {
      return this.actualizarSuscripcionMock(request);
    }

    const body = {
      monto: request.monto,
      metodoPago: 'MANUAL',
      tipoEstrategia: request.frecuencia === 'MENSUAL' ? 'CALENDARIO' : request.frecuencia
    };

    return this.http.put<ResultadoApi<any>>(`${this.baseUrl}/${request.id}`, body).pipe(
      map(res => {
        const actualizadaBackend = this.mapearDesdeBackend(res.datos);
        
        this.suscripciones.update(sus =>
          sus.map(s => {
            if (s.id === request.id) {
              const proximoVencimiento = this.calcularProximoVencimiento(
                request.fechaInicio,
                request.frecuencia
              );
              return {
                ...actualizadaBackend,
                nombre: request.nombre,
                descripcion: request.descripcion,
                categoria: request.categoria,
                frecuencia: request.frecuencia,
                fechaInicio: request.fechaInicio,
                proximoVencimiento,
                estado: request.estado || actualizadaBackend.estado,
                diasParaVencimiento: this.calcularDiasAlVencimiento(proximoVencimiento),
                vencePronto: this.calcularDiasAlVencimiento(proximoVencimiento) <= 5
              };
            }
            return s;
          })
        );
        
        return this.suscripciones().find(s => s.id === request.id)!;
      }),
      catchError(err => {
        console.warn('[SuscripcionService] Error al actualizar en backend, usando mock:', err);
        return this.actualizarSuscripcionMock(request);
      })
    );
  }

  /**
   * Eliminar suscripción
   */
  eliminarSuscripcion(id: string): Observable<boolean> {
    const usuarioId = this.auth.usuario()?.id;
    if (!usuarioId) {
      return this.eliminarSuscripcionMock(id);
    }

    return this.http.delete<ResultadoApi<void>>(`${this.baseUrl}/${id}`).pipe(
      map(() => {
        this.suscripciones.update(sus => sus.filter(s => s.id !== id));
        return true;
      }),
      catchError(err => {
        console.warn(`[SuscripcionService] Error al eliminar suscripción ${id} en backend, usando mock:`, err);
        return this.eliminarSuscripcionMock(id);
      })
    );
  }

  /**
   * Cambiar estado de suscripción
   */
  cambiarEstado(id: string, estado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA'): Observable<SuscripcionDTO> {
    const usuarioId = this.auth.usuario()?.id;
    if (!usuarioId) {
      return this.cambiarEstadoMock(id, estado);
    }

    if (estado === 'ACTIVA') {
      return this.http.post<ResultadoApi<any>>(`${this.baseUrl}/${id}/pagar`, {}).pipe(
        map(() => {
          this.suscripciones.update(sus =>
            sus.map(s => s.id === id ? { ...s, estado: 'ACTIVA', ultimaActualizacion: new Date().toISOString() } : s)
          );
          return this.suscripciones().find(s => s.id === id)!;
        }),
        catchError(err => {
          console.warn(`[SuscripcionService] Error al cambiar estado a ACTIVA en backend, usando mock:`, err);
          return this.cambiarEstadoMock(id, estado);
        })
      );
    } else if (estado === 'PAUSADA') {
      return this.http.post<ResultadoApi<any>>(`${this.baseUrl}/${id}/cancelar`, {}).pipe(
        map(res => {
          const dto = this.mapearDesdeBackend(res.datos);
          this.suscripciones.update(sus =>
            sus.map(s => s.id === id ? dto : s)
          );
          return dto;
        }),
        catchError(err => {
          console.warn(`[SuscripcionService] Error al cambiar estado a PAUSADA en backend, usando mock:`, err);
          return this.cambiarEstadoMock(id, estado);
        })
      );
    } else {
      this.suscripciones.update(sus =>
        sus.map(s => s.id === id ? { ...s, estado: 'VENCIDA', ultimaActualizacion: new Date().toISOString() } : s)
      );
      return of(this.suscripciones().find(s => s.id === id)!).pipe(
        catchError(err => {
          return this.cambiarEstadoMock(id, estado);
        })
      );
    }
  }

  /**
   * Filtrar suscripciones
   */
  filtrarSuscripciones(filtros: {
    busqueda?: string;
    categoria?: string;
    frecuencia?: FrecuenciaSuscripcion;
    estado?: string;
    montoMin?: number;
    montoMax?: number;
  }): SuscripcionDTO[] {
    let resultado = this.suscripciones();

    if (filtros.busqueda) {
      const busqueda = filtros.busqueda.toLowerCase();
      resultado = resultado.filter(s =>
        s.nombre.toLowerCase().includes(busqueda) ||
        s.descripcion.toLowerCase().includes(busqueda)
      );
    }

    if (filtros.categoria) {
      resultado = resultado.filter(s => s.categoria === filtros.categoria);
    }

    if (filtros.frecuencia) {
      resultado = resultado.filter(s => s.frecuencia === filtros.frecuencia);
    }

    if (filtros.estado) {
      resultado = resultado.filter(s => s.estado === filtros.estado);
    }

    if (filtros.montoMin !== undefined) {
      resultado = resultado.filter(s => s.monto >= filtros.montoMin!);
    }

    if (filtros.montoMax !== undefined) {
      resultado = resultado.filter(s => s.monto <= filtros.montoMax!);
    }

    return resultado;
  }

  /**
   * ── MÉTODOS PRIVADOS ──
   */
  private mapearDesdeBackend(s: any): SuscripcionDTO {
    const frecuencia = (s.tipoEstrategia === 'CALENDARIO' ? 'MENSUAL' : s.tipoEstrategia) as FrecuenciaSuscripcion;
    const proximoVencimiento = s.fechaVencimiento || new Date().toISOString().split('T')[0];
    const diasParaVencimiento = this.calcularDiasAlVencimiento(proximoVencimiento);
    const vencePronto = diasParaVencimiento <= 5;
    
    return {
      id: s.id,
      nombre: s.nombre,
      descripcion: s.descripcion || `Suscripción de ${s.nombre} (${s.metodoPago || 'Manual'})`,
      categoria: this.determinarCategoria(s.nombre),
      monto: s.monto,
      frecuencia: frecuencia || 'MENSUAL',
      fechaInicio: s.fechaInicio || new Date().toISOString().split('T')[0],
      proximoVencimiento,
      estado: s.estado === 'CANCELADA' ? 'PAUSADA' : (s.estado as EstadoSuscripcion),
      fechaCreacion: s.fechaCreacion || s.fechaInicio || new Date().toISOString(),
      ultimaActualizacion: s.fechaActualizacion || s.fechaInicio || new Date().toISOString(),
      diasParaVencimiento,
      vencePronto
    };
  }

  private determinarCategoria(nombre: string): string {
    const n = nombre.toLowerCase();
    if (n.includes('netflix') || n.includes('spotify') || n.includes('youtube') || n.includes('prime') || n.includes('amazon') || n.includes('disney') || n.includes('max') || n.includes('hbo') || n.includes('apple') || n.includes('playstation') || n.includes('xbox') || n.includes('ps plus')) {
      return 'leisure';
    }
    if (n.includes('gimnasio') || n.includes('fit') || n.includes('salud') || n.includes('gym')) {
      return 'health';
    }
    if (n.includes('internet') || n.includes('hogar') || n.includes('fibra') || n.includes('luz') || n.includes('agua') || n.includes('cable')) {
      return 'home';
    }
    if (n.includes('canva') || n.includes('chatgpt') || n.includes('openai') || n.includes('github') || n.includes('adobe') || n.includes('estudio') || n.includes('curso')) {
      return 'study';
    }
    if (n.includes('seguro') || n.includes('auto') || n.includes('taxi') || n.includes('uber') || n.includes('cabify')) {
      return 'transport';
    }
    if (n.includes('comida') || n.includes('restaurant') || n.includes('snack') || n.includes('pedidosya') || n.includes('rappi')) {
      return 'food';
    }
    return 'leisure'; // Default
  }

  private calcularDiasAlVencimiento(fechaVencimiento: string): number {
    const hoy = new Date();
    const vencimiento = new Date(fechaVencimiento);
    const diferenciaTiempo = vencimiento.getTime() - hoy.getTime();
    return Math.ceil(diferenciaTiempo / (1000 * 60 * 60 * 24));
  }

  private calcularProximoVencimiento(fechaInicio: string, frecuencia: FrecuenciaSuscripcion): string {
    const fecha = new Date(fechaInicio);
    
    switch (frecuencia) {
      case 'DIARIO':
        fecha.setDate(fecha.getDate() + 1);
        break;
      case 'SEMANAL':
        fecha.setDate(fecha.getDate() + 7);
        break;
      case 'QUINCENAL':
        fecha.setDate(fecha.getDate() + 15);
        break;
      case 'MENSUAL':
        fecha.setMonth(fecha.getMonth() + 1);
        break;
      case 'TRIMESTRAL':
        fecha.setMonth(fecha.getMonth() + 3);
        break;
      case 'SEMESTRAL':
        fecha.setMonth(fecha.getMonth() + 6);
        break;
      case 'ANUAL':
        fecha.setFullYear(fecha.getFullYear() + 1);
        break;
    }
    
    return fecha.toISOString().split('T')[0];
  }

  private calcularGastosAlMes(monto: number, frecuencia: FrecuenciaSuscripcion): number {
    switch (frecuencia) {
      case 'DIARIO':
        return monto * 30;
      case 'SEMANAL':
        return (monto / 7) * 30;
      case 'QUINCENAL':
        return (monto / 15) * 30;
      case 'MENSUAL':
        return monto;
      case 'TRIMESTRAL':
        return monto / 3;
      case 'SEMESTRAL':
        return monto / 6;
      case 'ANUAL':
        return monto / 12;
      default:
        return 0;
    }
  }

  private generarId(): string {
    return 'sus_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  private cargarSuscripcionesMock(): void {
    const mockData: SuscripcionDTO[] = [
      {
        id: '1',
        nombre: 'Netflix',
        descripcion: 'Servicio de streaming de películas y series',
        categoria: 'leisure',
        monto: 99,
        frecuencia: 'MENSUAL',
        fechaInicio: '2026-06-15',
        proximoVencimiento: '2026-07-15',
        estado: 'ACTIVA',
        fechaCreacion: '2026-01-15T00:00:00Z',
        ultimaActualizacion: '2026-01-15T00:00:00Z',
        diasParaVencimiento: this.calcularDiasAlVencimiento('2026-07-15'),
        vencePronto: this.calcularDiasAlVencimiento('2026-07-15') <= 5
      },
      {
        id: '2',
        nombre: 'Spotify',
        descripcion: 'Servicio de streaming de música',
        categoria: 'leisure',
        monto: 119,
        frecuencia: 'MENSUAL',
        fechaInicio: '2026-06-20',
        proximoVencimiento: '2026-07-20',
        estado: 'ACTIVA',
        fechaCreacion: '2026-02-20T00:00:00Z',
        ultimaActualizacion: '2026-02-20T00:00:00Z',
        diasParaVencimiento: this.calcularDiasAlVencimiento('2026-07-20'),
        vencePronto: this.calcularDiasAlVencimiento('2026-07-20') <= 5
      },
      {
        id: '3',
        nombre: 'Internet Fibra Óptica',
        descripcion: 'Servicio de internet residencial',
        categoria: 'home',
        monto: 149,
        frecuencia: 'MENSUAL',
        fechaInicio: '2026-06-01',
        proximoVencimiento: '2026-07-01',
        estado: 'ACTIVA',
        fechaCreacion: '2025-12-01T00:00:00Z',
        ultimaActualizacion: '2025-12-01T00:00:00Z',
        diasParaVencimiento: this.calcularDiasAlVencimiento('2026-07-01'),
        vencePronto: this.calcularDiasAlVencimiento('2026-07-01') <= 5
      },
      {
        id: '4',
        nombre: 'Gimnasio PowerFit',
        descripcion: 'Membresía mensual al gimnasio',
        categoria: 'health',
        monto: 89,
        frecuencia: 'MENSUAL',
        fechaInicio: '2026-06-10',
        proximoVencimiento: '2026-07-10',
        estado: 'ACTIVA',
        fechaCreacion: '2026-03-10T00:00:00Z',
        ultimaActualizacion: '2026-03-10T00:00:00Z',
        diasParaVencimiento: this.calcularDiasAlVencimiento('2026-07-10'),
        vencePronto: this.calcularDiasAlVencimiento('2026-07-10') <= 5
      },
      {
        id: '5',
        nombre: 'Seguro de Auto',
        descripcion: 'Póliza anual de seguros',
        categoria: 'transport',
        monto: 599,
        frecuencia: 'ANUAL',
        fechaInicio: '2026-06-15',
        proximoVencimiento: '2027-06-15',
        estado: 'ACTIVA',
        fechaCreacion: '2025-06-15T00:00:00Z',
        ultimaActualizacion: '2025-06-15T00:00:00Z',
        diasParaVencimiento: this.calcularDiasAlVencimiento('2027-06-15'),
        vencePronto: this.calcularDiasAlVencimiento('2027-06-15') <= 5
      },
      {
        id: '6',
        nombre: 'Software Adobe',
        descripcion: 'Suscripción Creative Cloud',
        categoria: 'study',
        monto: 299,
        frecuencia: 'MENSUAL',
        fechaInicio: '2026-06-01',
        proximoVencimiento: '2026-07-01',
        estado: 'PAUSADA',
        fechaCreacion: '2026-04-01T00:00:00Z',
        ultimaActualizacion: '2026-05-10T00:00:00Z',
        diasParaVencimiento: this.calcularDiasAlVencimiento('2026-07-01'),
        vencePronto: false
      }
    ];

    this.suscripciones.set(mockData);
  }

  private crearSuscripcionMock(request: CrearSuscripcionRequest): Observable<SuscripcionDTO> {
    const ahora = new Date().toISOString();
    const proximoVencimiento = this.calcularProximoVencimiento(request.fechaInicio, request.frecuencia);
    
    const nuevaSuscripcion: SuscripcionDTO = {
      id: this.generarId(),
      ...request,
      proximoVencimiento,
      estado: 'ACTIVA',
      fechaCreacion: ahora,
      ultimaActualizacion: ahora,
      diasParaVencimiento: this.calcularDiasAlVencimiento(proximoVencimiento),
      vencePronto: this.calcularDiasAlVencimiento(proximoVencimiento) <= 5
    };

    this.suscripciones.update(sus => [...sus, nuevaSuscripcion]);
    return of(nuevaSuscripcion);
  }

  private actualizarSuscripcionMock(request: ActualizarSuscripcionRequest): Observable<SuscripcionDTO> {
    const ahora = new Date().toISOString();
    
    this.suscripciones.update(sus =>
      sus.map(s => {
        if (s.id === request.id) {
          const proximoVencimiento = this.calcularProximoVencimiento(
            request.fechaInicio,
            request.frecuencia
          );
          return {
            ...s,
            nombre: request.nombre,
            descripcion: request.descripcion,
            categoria: request.categoria,
            monto: request.monto,
            frecuencia: request.frecuencia,
            fechaInicio: request.fechaInicio,
            proximoVencimiento,
            estado: request.estado || s.estado,
            ultimaActualizacion: ahora,
            diasParaVencimiento: this.calcularDiasAlVencimiento(proximoVencimiento),
            vencePronto: this.calcularDiasAlVencimiento(proximoVencimiento) <= 5
          };
        }
        return s;
      })
    );

    const actualizada = this.suscripciones().find(s => s.id === request.id)!;
    return of(actualizada);
  }

  private eliminarSuscripcionMock(id: string): Observable<boolean> {
    this.suscripciones.update(sus => sus.filter(s => s.id !== id));
    return of(true);
  }

  private cambiarEstadoMock(id: string, estado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA'): Observable<SuscripcionDTO> {
    const ahora = new Date().toISOString();
    
    this.suscripciones.update(sus =>
      sus.map(s => s.id === id ? { ...s, estado, ultimaActualizacion: ahora } : s)
    );

    const actualizada = this.suscripciones().find(s => s.id === id)!;
    return of(actualizada);
  }
}
