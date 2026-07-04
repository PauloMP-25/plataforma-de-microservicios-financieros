import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay, catchError } from 'rxjs/operators';
import { environment } from '../../../enviroments/environment';
import { ResultadoApi } from '../../../core/models/auth/user.model';
import { AdminDashboardData, ResumenPagosDTO, PagoAdmin, PaginacionAdmin } from '../models/admin-dashboard.model';

@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.gatewayUrl}/api/v1/pagos/admin`;

  // Datos mock para fallback robusto
  private readonly mockResumenPagos: ResumenPagosDTO = {
    totalTransacciones: 148,
    ingresosTotales: 5420.50,
    transaccionesPorEstado: {
      EXITOSO: 124,
      PENDIENTE: 15,
      FALLIDO: 9
    },
    suscripcionesPorPlan: {
      FREE: 140,
      PRO: 78,
      PREMIUM: 42
    }
  };

  private readonly mockHistorialPagos: PagoAdmin[] = [
    {
      id: 'fa7b8c9d-1234-5678-90ab-cdef01234567',
      usuarioId: '8f7e6d5c-4321-8765-09ba-fedcba987654',
      estado: 'EXITOSO',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
      detalles: [{ id: 'd1', planSolicitado: 'PREMIUM', monto: 99.90, moneda: 'PEN', cantidad: 1 }]
    },
    {
      id: 'bc6a5d4e-5678-1234-abcd-ef0123456789',
      usuarioId: '3d2c1b0a-8765-4321-fedc-ba9876543210',
      estado: 'PENDIENTE',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
      detalles: [{ id: 'd2', planSolicitado: 'PRO', monto: 49.90, moneda: 'PEN', cantidad: 1 }]
    },
    {
      id: 'de8f9a0b-9012-3456-7890-abcdef012345',
      usuarioId: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      estado: 'EXITOSO',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 600).toISOString(),
      detalles: [{ id: 'd3', planSolicitado: 'PRO', monto: 49.90, moneda: 'PEN', cantidad: 1 }]
    },
    {
      id: '12345678-abcd-ef01-2345-6789abcdef01',
      usuarioId: 'fedcba98-7654-3210-fedc-ba9876543210',
      estado: 'FALLIDO',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 1440).toISOString(),
      detalles: [{ id: 'd4', planSolicitado: 'PREMIUM', monto: 99.90, moneda: 'PEN', cantidad: 1 }]
    },
    {
      id: '87654321-fedc-ba98-7654-3210fedcba98',
      usuarioId: 'abcdef01-2345-6789-abcd-ef0123456789',
      estado: 'EXITOSO',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 2880).toISOString(),
      detalles: [{ id: 'd5', planSolicitado: 'PRO', monto: 49.90, moneda: 'PEN', cantidad: 1 }]
    },
    {
      id: '98765432-10ab-cdef-0123-456789abcdef',
      usuarioId: 'f0e1d2c3-b4a5-9687-7685-9403b2c1a0e9',
      estado: 'EXITOSO',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 4320).toISOString(),
      detalles: [{ id: 'd6', planSolicitado: 'PRO', monto: 49.90, moneda: 'PEN', cantidad: 1 }]
    },
    {
      id: 'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
      usuarioId: 'c1b2a3f4-d5e6-7b8c-9a0d-1e2f3a4b5c6d',
      estado: 'FALLIDO',
      fechaCreacion: new Date(Date.now() - 1000 * 60 * 5760).toISOString(),
      detalles: [{ id: 'd7', planSolicitado: 'PRO', monto: 49.90, moneda: 'PEN', cantidad: 1 }]
    }
  ];

  obtenerResumen(): Observable<AdminDashboardData> {
    return of(this.mockData).pipe(delay(250));
  }

  obtenerResumenPagos(): Observable<ResultadoApi<ResumenPagosDTO>> {
    return this.http.get<ResultadoApi<ResumenPagosDTO>>(`${this.base}/resumen`).pipe(
      catchError(() => {
        console.warn('Backend offline - usando mock para resumen de pagos');
        return of({
          exito: true,
          mensaje: 'Resumen administrativo generado (MOCK)',
          estado: 200,
          datos: this.mockResumenPagos
        });
      })
    );
  }

  listarPagos(pagina: number = 0, tamanio: number = 10): Observable<ResultadoApi<PaginacionAdmin<PagoAdmin>>> {
    return this.http.get<ResultadoApi<PaginacionAdmin<PagoAdmin>>>(`${this.base}/historial`, {
      params: { pagina: pagina.toString(), tamanio: tamanio.toString() }
    }).pipe(
      catchError(() => {
        console.warn('Backend offline - usando mock para historial de pagos');
        const total = this.mockHistorialPagos.length;
        const contenido = this.mockHistorialPagos.slice(pagina * tamanio, (pagina + 1) * tamanio);
        const paginas = Math.ceil(total / tamanio);
        return of({
          exito: true,
          mensaje: 'Historial de pagos recuperado (MOCK)',
          estado: 200,
          datos: {
            contenido,
            numeroPagina: pagina,
            tamañoPagina: tamanio,
            totalElementos: total,
            totalPaginas: paginas,
            esUltima: pagina >= paginas - 1
          }
        });
      })
    );
  }

  corregirEstadoPago(id: string, nuevoEstado: string): Observable<ResultadoApi<void>> {
    return this.http.patch<ResultadoApi<void>>(`${this.base}/${id}/estado`, null, {
      params: { nuevoEstado }
    }).pipe(
      catchError(() => {
        console.warn('Backend offline - simulando actualización de estado del pago');
        const pago = this.mockHistorialPagos.find(p => p.id === id);
        if (pago) {
          pago.estado = nuevoEstado as any;
        }
        return of({
          exito: true,
          mensaje: 'Estado del pago actualizado manualmente (MOCK)',
          estado: 200,
          datos: undefined as any
        });
      })
    );
  }

  private readonly mockData: AdminDashboardData = {
    kpis: [
      {
        etiqueta: 'Usuarios totales',
        valor: '2,847',
        detalle: 'Mock hasta exponer /api/v1/admin/** en Gateway',
        tendencia: '+8.2%',
        tendenciaTipo: 'up',
        icono: 'fa-solid fa-users',
        tono: 'primary'
      },
      {
        etiqueta: 'Ingresos totales',
        valor: 'S/ 184,320',
        detalle: 'Conectable con pagos admin',
        tendencia: '+12.5%',
        tendenciaTipo: 'up',
        icono: 'fa-solid fa-sack-dollar',
        tono: 'info'
      },
      {
        etiqueta: 'Pagos exitosos',
        valor: '1,239',
        detalle: 'Tasa estimada 96.4%',
        tendencia: '+3.1%',
        tendenciaTipo: 'up',
        icono: 'fa-solid fa-circle-check',
        tono: 'success'
      },
      {
        etiqueta: 'Eventos auditados',
        valor: '9,412',
        detalle: 'Auditoría por módulo',
        tendencia: '-2.0%',
        tendenciaTipo: 'down',
        icono: 'fa-solid fa-shield-halved',
        tono: 'purple'
      }
    ],
    servicios: [
      { nombre: 'API Gateway', puerto: 8080, estado: 'healthy', latencia: '12ms', descripcion: 'Entrada principal y rutas protegidas' },
      { nombre: 'ms-usuario', puerto: 8081, estado: 'healthy', latencia: '24ms', descripcion: 'Auth y usuarios admin' },
      { nombre: 'ms-auditoria', puerto: 8082, estado: 'healthy', latencia: '18ms', descripcion: 'Logs, auditoría y seguridad' },
      { nombre: 'ms-cliente', puerto: 8083, estado: 'healthy', latencia: '31ms', descripcion: 'Perfiles, metas y límites' },
      { nombre: 'ms-mensajeria', puerto: 8084, estado: 'warning', latencia: '187ms', descripcion: 'OTP y canales externos' },
      { nombre: 'ms-nucleo-financiero', puerto: 8085, estado: 'healthy', latencia: '42ms', descripcion: 'Transacciones y categorías' },
      { nombre: 'ms-ia', puerto: 8086, estado: 'healthy', latencia: '55ms', descripcion: 'Módulos inteligentes' },
      { nombre: 'ms-pagos', puerto: 8087, estado: 'healthy', latencia: '29ms', descripcion: 'Checkout y administración de pagos' },
      { nombre: 'ms-suscripciones', puerto: 8088, estado: 'down', latencia: 'timeout', descripcion: 'Existe código, no está en Docker híbrido actual' }
    ],
    pagos: [
      { id: 'PAG-0091', usuario: 'carlos.mendez', monto: 'S/ 49.90', plan: 'PRO', estado: 'EXITOSO' },
      { id: 'PAG-0090', usuario: 'ana.torres', monto: 'S/ 19.90', plan: 'BASIC', estado: 'PENDIENTE' },
      { id: 'PAG-0089', usuario: 'luis.ramos', monto: 'S/ 99.90', plan: 'ENTERPRISE', estado: 'EXITOSO' },
      { id: 'PAG-0088', usuario: 'sofia.vega', monto: 'S/ 49.90', plan: 'PRO', estado: 'FALLIDO' }
    ],
    alertas: [
      { titulo: 'Ruta Gateway faltante', descripcion: 'Agregar /api/v1/admin/** para conectar usuarios admin reales.', severidad: 'media', icono: 'fa-solid fa-route' },
      { titulo: 'IPs bloqueadas', descripcion: '3 direcciones en lista negra pendientes de revisión.', severidad: 'critica', icono: 'fa-solid fa-ban' },
      { titulo: 'OTP bloqueado', descripcion: '1 usuario superó intentos permitidos de verificación.', severidad: 'media', icono: 'fa-solid fa-mobile-screen' }
    ],
    ipsBloqueadas: [
      { ip: '192.168.45.201', motivo: 'Fuerza bruta', tiempo: 'hace 2h' },
      { ip: '103.21.58.12', motivo: 'Múltiples OTP fallidos', tiempo: 'hace 5h' },
      { ip: '77.245.12.88', motivo: 'Actividad sospechosa', tiempo: 'hace 1d' }
    ],
    otpsBloqueados: [
      { usuario: 'marco.silva', estado: 'Bloqueado', intentos: 5 },
      { usuario: 'diana.flores', estado: 'En revisión', intentos: 3 },
      { usuario: 'pedro.castro', estado: 'Bloqueado', intentos: 5 }
    ],
    graficoIngresos: [
      { mes: 'Ene', ingresos: 38, egresos: 18 },
      { mes: 'Feb', ingresos: 47, egresos: 24 },
      { mes: 'Mar', ingresos: 58, egresos: 30 },
      { mes: 'Abr', ingresos: 69, egresos: 38 },
      { mes: 'May', ingresos: 76, egresos: 45 },
      { mes: 'Jun', ingresos: 88, egresos: 53 }
    ],
    secciones: [
      { titulo: 'Usuarios', descripcion: 'Tabla filtrable por rol, estado y fecha.', estado: 'pendiente', endpoint: '/api/v1/admin/usuarios' },
      { titulo: 'Pagos', descripcion: 'Resumen, historial y corrección de estados.', estado: 'conectado', endpoint: '/api/v1/pagos/admin/resumen' },
      { titulo: 'Auditoría', descripcion: 'Eventos por módulo y actividad reciente.', estado: 'conectado', endpoint: '/api/v1/auditoria/registros' },
      { titulo: 'Seguridad', descripcion: 'Lista negra, bloqueo y desbloqueo de IPs.', estado: 'pendiente', endpoint: '/api/v1/seguridad/lista-negra' },
      { titulo: 'OTP & Mensajería', descripcion: 'Códigos emitidos y usuarios bloqueados.', estado: 'pendiente', endpoint: '/api/v1/mensajeria/admin/codigos' },
      { titulo: 'IA & Analítica', descripcion: 'Métricas IA mock y limpieza de caché real.', estado: 'mock', endpoint: '/api/v1/ia/admin/cache/flush' }
    ]
  };
}
