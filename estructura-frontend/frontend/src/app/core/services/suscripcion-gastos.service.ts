import { Injectable, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
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
    const usuarioId = this.auth.usuario()?.id ?? '';
    
    return this.http.get<ResultadoApi<any[]>>(`${this.baseUrl}/usuario/${usuarioId}`).pipe(
      map(res => {
        const list = (res.datos || []).map(item => this.mapToDTO(item));
        this.suscripciones.set(list);
        this.cargando.set(false);
        return list;
      })
    );
  }

  /**
   * Obtener suscripción por ID
   */
  obtenerSuscripcion(id: string): Observable<SuscripcionDTO | undefined> {
    return of(this.suscripciones().find(s => s.id === id));
  }

  /**
   * Crear nueva suscripción
   */
  crearSuscripcion(request: CrearSuscripcionRequest): Observable<SuscripcionDTO> {
    const usuarioId = this.auth.usuario()?.id ?? '';
    const body = {
      usuarioId,
      nombre: request.nombre,
      monto: request.monto,
      metodoPago: 'MANUAL',
      tipoEstrategia: request.frecuencia === 'MENSUAL' ? 'CALENDARIO' : request.frecuencia,
      fechaInicio: request.fechaInicio
    };

    return this.http.post<ResultadoApi<any>>(this.baseUrl, body).pipe(
      map(res => {
        const dto = this.mapToDTO(res.datos);
        this.suscripciones.update(sus => [...sus, dto]);
        return dto;
      })
    );
  }

  /**
   * Actualizar suscripción existente
   */
  actualizarSuscripcion(request: ActualizarSuscripcionRequest): Observable<SuscripcionDTO> {
    const body = {
      monto: request.monto,
      metodoPago: 'MANUAL',
      tipoEstrategia: request.frecuencia === 'MENSUAL' ? 'CALENDARIO' : request.frecuencia
    };

    return this.http.put<ResultadoApi<any>>(`${this.baseUrl}/${request.id}`, body).pipe(
      map(res => {
        const dto = this.mapToDTO(res.datos);
        this.suscripciones.update(sus =>
          sus.map(s => s.id === dto.id ? dto : s)
        );
        return dto;
      })
    );
  }

  /**
   * Eliminar suscripción
   */
  eliminarSuscripcion(id: string): Observable<boolean> {
    return this.http.delete<ResultadoApi<void>>(`${this.baseUrl}/${id}`).pipe(
      map(() => {
        this.suscripciones.update(sus => sus.filter(s => s.id !== id));
        return true;
      })
    );
  }

  /**
   * Cambiar estado de suscripción
   */
  cambiarEstado(id: string, estado: 'ACTIVA' | 'PAUSADA' | 'VENCIDA'): Observable<SuscripcionDTO> {
    if (estado === 'ACTIVA') {
      return this.http.post<ResultadoApi<any>>(`${this.baseUrl}/${id}/pagar`, {}).pipe(
        map(() => {
          this.suscripciones.update(sus =>
            sus.map(s => s.id === id ? { ...s, estado: 'ACTIVA' } : s)
          );
          return this.suscripciones().find(s => s.id === id)!;
        })
      );
    } else if (estado === 'PAUSADA') {
      return this.http.post<ResultadoApi<any>>(`${this.baseUrl}/${id}/cancelar`, {}).pipe(
        map(res => {
          const dto = this.mapToDTO(res.datos);
          this.suscripciones.update(sus =>
            sus.map(s => s.id === id ? dto : s)
          );
          return dto;
        })
      );
    } else {
      this.suscripciones.update(sus =>
        sus.map(s => s.id === id ? { ...s, estado: 'VENCIDA' } : s)
      );
      return of(this.suscripciones().find(s => s.id === id)!);
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
  private mapToDTO(res: any): SuscripcionDTO {
    const frecuencia = (res.tipoEstrategia === 'CALENDARIO' ? 'MENSUAL' : res.tipoEstrategia) as FrecuenciaSuscripcion;
    const proximoVencimiento = res.fechaVencimiento;
    const diasParaVencimiento = this.calcularDiasAlVencimiento(proximoVencimiento);
    
    return {
      id: res.id,
      nombre: res.nombre,
      descripcion: `Pago con ${res.metodoPago || 'tarjeta'}`,
      categoria: this.determinarCategoria(res.nombre),
      monto: res.monto,
      frecuencia: frecuencia || 'MENSUAL',
      fechaInicio: res.fechaInicio,
      proximoVencimiento: proximoVencimiento,
      estado: res.estado === 'CANCELADA' ? 'PAUSADA' : (res.estado as EstadoSuscripcion),
      fechaCreacion: res.fechaInicio,
      ultimaActualizacion: res.fechaInicio,
      diasParaVencimiento,
      vencePronto: diasParaVencimiento <= 5
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
}
