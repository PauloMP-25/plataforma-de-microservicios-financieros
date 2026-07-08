import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay, catchError, map } from 'rxjs/operators';
import { environment } from '../../../enviroments/environment';
import { ResultadoApi } from '../../../core/models/auth/user.model';
import { AdminDashboardData, ResumenPagosDTO, PagoAdmin, PaginacionAdmin, AdminServicioEstado } from '../models/admin-dashboard.model';

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

  // Lista mutable de microservicios como fuente de verdad
  private readonly microserviciosList: AdminServicioEstado[] = [
    { nombre: 'API Gateway', puerto: 8080, estado: 'healthy', latencia: '12ms', descripcion: 'Entrada principal y rutas protegidas' },
    { nombre: 'ms-usuario', puerto: 8081, estado: 'healthy', latencia: '24ms', descripcion: 'Auth y usuarios admin' },
    { nombre: 'ms-auditoria', puerto: 8082, estado: 'healthy', latencia: '18ms', descripcion: 'Logs, auditoría y seguridad' },
    { nombre: 'ms-cliente', puerto: 8083, estado: 'healthy', latencia: '31ms', descripcion: 'Perfiles, metas y límites' },
    { nombre: 'ms-mensajeria', puerto: 8084, estado: 'warning', latencia: '187ms', descripcion: 'OTP y canales externos' },
    { nombre: 'ms-nucleo-financiero', puerto: 8085, estado: 'healthy', latencia: '42ms', descripcion: 'Transacciones y categorías' },
    { nombre: 'ms-ia', puerto: 8086, estado: 'healthy', latencia: '55ms', descripcion: 'Módulos inteligentes' },
    { nombre: 'ms-pagos', puerto: 8087, estado: 'healthy', latencia: '29ms', descripcion: 'Checkout y administración de pagos' },
    { nombre: 'ms-suscripciones', puerto: 8088, estado: 'healthy', latencia: '25ms', descripcion: 'Suscripciones y facturación periódica' }
  ];

  obtenerResumen(): Observable<AdminDashboardData> {
    const data = { ...this.mockData, servicios: this.microserviciosList };
    return of(data).pipe(delay(250));
  }

  obtenerServicios(): Observable<AdminServicioEstado[]> {
    return of(this.microserviciosList).pipe(delay(150));
  }

  reiniciarServicio(nombre: string): Observable<boolean> {
    const serv = this.microserviciosList.find(s => s.nombre === nombre);
    if (serv) {
      serv.estado = 'healthy';
      serv.latencia = `${Math.floor(Math.random() * 30) + 10}ms`;
    }
    return of(true).pipe(delay(2000)); // Simular delay de 2 segundos de reinicio
  }

  obtenerLogs(nombre: string): Observable<string[]> {
    const timestamp = () => new Date().toLocaleTimeString();
    const logsMap: Record<string, string[]> = {
      'API Gateway': [
        `[${timestamp()}] [INFO] Gateway routing initialized on port 8080`,
        `[${timestamp()}] [INFO] Route /api/v1/auth/me forwarded to ms-usuario:8081`,
        `[${timestamp()}] [INFO] Route /api/v1/pagos/checkout forwarded to ms-pagos:8087`,
        `[${timestamp()}] [WARN] Potential brute force attack: IP 185.45.23.1 restricted`,
        `[${timestamp()}] [INFO] SSL Handshake succeeded for client browser session`
      ],
      'ms-usuario': [
        `[${timestamp()}] [INFO] Starting Authentication Service on port 8081`,
        `[${timestamp()}] [INFO] Connected to PostgreSQL database 'luka_users_db'`,
        `[${timestamp()}] [INFO] JWT token validation keys initialized successfully`,
        `[${timestamp()}] [INFO] OTP verification handler listening to event bus`
      ],
      'ms-auditoria': [
        `[${timestamp()}] [INFO] Starting ms-auditoria on port 8082`,
        `[${timestamp()}] [INFO] Event consumer started successfully on topic 'auditoria-eventos'`,
        `[${timestamp()}] [INFO] Database connection established to MySQL 'auditoria'`,
        `[${timestamp()}] [INFO] Admin access logging interceptor active`
      ],
      'ms-cliente': [
        `[${timestamp()}] [INFO] Starting Customer Profiles Service on port 8083`,
        `[${timestamp()}] [INFO] Budget checking service initialized`,
        `[${timestamp()}] [INFO] Syncing user budget limits cache with Redis`
      ],
      'ms-mensajeria': [
        `[${timestamp()}] [INFO] Starting ms-mensajeria on port 8084`,
        `[${timestamp()}] [WARN] Twilio API connection latency is high (187ms)`,
        `[${timestamp()}] [INFO] SMTP mail dispatcher initialized successfully`
      ],
      'ms-nucleo-financiero': [
        `[${timestamp()}] [INFO] Starting Financial Core Service on port 8085`,
        `[${timestamp()}] [INFO] Transaction mapping configurations loaded`,
        `[${timestamp()}] [INFO] Database pool connection active`
      ],
      'ms-ia': [
        `[${timestamp()}] [INFO] Starting python FastAPI IA engine on port 8086`,
        `[${timestamp()}] [INFO] TensorFlow / PyTorch models loaded in memory`,
        `[${timestamp()}] [INFO] Warm-up inference complete (took 1.2s)`
      ],
      'ms-pagos': [
        `[${timestamp()}] [INFO] Starting ms-pagos on port 8087`,
        `[${timestamp()}] [INFO] Stripe webhook endpoint active and validated`,
        `[${timestamp()}] [INFO] Webhook signature key loaded successfully`
      ],
      'ms-suscripciones': [
        `[${timestamp()}] [ERROR] Connection refused on port 8088`,
        `[${timestamp()}] [ERROR] Service ms-suscripciones seems offline in local Docker daemon`,
        `[${timestamp()}] [WARN] Redis subscription cache sync timed out`
      ]
    };
    
    const serv = this.microserviciosList.find(s => s.nombre === nombre);
    if (nombre === 'ms-suscripciones' && serv && serv.estado === 'healthy') {
      return of([
        `[${timestamp()}] [INFO] Re-starting Suscripciones service on port 8088`,
        `[${timestamp()}] [INFO] Connected to Database pool successfully`,
        `[${timestamp()}] [INFO] Syncing user subscription states... complete`,
        `[${timestamp()}] [INFO] Service is now ONLINE and healthy`
      ]).pipe(delay(100));
    }

    return of(logsMap[nombre] || [`[${timestamp()}] [INFO] Listening to connections on port ${serv?.puerto || '8080'}`]).pipe(delay(100));
  }

  obtenerResumenPagos(anio?: number): Observable<ResultadoApi<ResumenPagosDTO>> {
    let params = new HttpParams();
    if (anio) params = params.set('anio', anio.toString());

    return this.http.get<ResultadoApi<ResumenPagosDTO>>(`${this.base}/resumen`, { params }).pipe(
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

  obtenerUsuarios(
    habilitado?: boolean,
    rol?: string,
    texto?: string,
    pagina = 0,
    tamanio = 10
  ): Observable<ResultadoApi<PaginacionAdmin<any>>> {
    let params = new HttpParams()
      .set('pagina', pagina.toString())
      .set('tamanio', tamanio.toString());

    if (habilitado !== undefined) params = params.set('habilitado', habilitado.toString());
    if (rol) params = params.set('rol', rol);
    if (texto) params = params.set('texto', texto);

    return this.http.get<ResultadoApi<PaginacionAdmin<any>>>(`${environment.gatewayUrl}/api/v1/admin/usuarios`, { params }).pipe(
      catchError(() => {
        console.warn('Backend offline - usando mock para usuarios admin');
        return of({
          exito: true,
          mensaje: 'Listado de usuarios recuperado (MOCK)',
          estado: 200,
          datos: {
            contenido: [
              { id: '8f7e6d5c-4321-8765-09ba-fedcba987654', nombreUsuario: 'Ana Torres', correo: 'ana@mail.com', planActual: 'PREMIUM', habilitado: true, fechaCreacion: '2024-01-15T12:00:00' },
              { id: '3d2c1b0a-8765-4321-fedc-ba9876543210', nombreUsuario: 'Luis Ramírez', correo: 'luis@mail.com', planActual: 'PRO', habilitado: true, fechaCreacion: '2024-02-03T12:00:00' },
              { id: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d', nombreUsuario: 'Carla Díaz', correo: 'carla@mail.com', planActual: 'FREE', habilitado: false, fechaCreacion: '2024-03-18T12:00:00' },
              { id: 'fedcba98-7654-3210-fedc-ba9876543210', nombreUsuario: 'Pedro Solano', correo: 'pedro@mail.com', planActual: 'PREMIUM', habilitado: true, fechaCreacion: '2024-03-22T12:00:00' },
              { id: 'abcdef01-2345-6789-abcd-ef0123456789', nombreUsuario: 'Sofía Vega', correo: 'sofia@mail.com', planActual: 'PRO', habilitado: true, fechaCreacion: '2024-04-01T12:00:00' },
              { id: 'f0e1d2c3-b4a5-9687-7685-9403b2c1a0e9', nombreUsuario: 'Miguel Oros', correo: 'miguel@mail.com', planActual: 'FREE', habilitado: false, fechaCreacion: '2024-04-10T12:00:00' }
            ],
            numeroPagina: pagina,
            tamañoPagina: tamanio,
            totalElementos: 6,
            totalPaginas: 1,
            esUltima: true
          }
        });
      })
    );
  }

  obtenerListaNegraIp(pagina = 0, tamanio = 10): Observable<ResultadoApi<PaginacionAdmin<any>>> {
    return this.http.get<ResultadoApi<PaginacionAdmin<any>>>(`${environment.gatewayUrl}/api/v1/seguridad/lista-negra`, {
      params: { pagina: pagina.toString(), tamanio: tamanio.toString() }
    }).pipe(
      catchError(() => {
        console.warn('Backend offline - usando mock para lista negra de IPs');
        return of({
          exito: true,
          mensaje: 'Lista negra recuperada (MOCK)',
          estado: 200,
          datos: {
            contenido: [
              { ip: '185.45.23.1', motivo: 'Múltiples intentos fallidos', fechaBloqueo: '2024-06-01T10:00:00', fechaExpiracion: null },
              { ip: '91.234.12.8', motivo: 'Actividad sospechosa', fechaBloqueo: '2024-05-28T14:30:00', fechaExpiracion: '2024-06-02T14:30:00' }
            ],
            numeroPagina: pagina,
            tamañoPagina: tamanio,
            totalElementos: 2,
            totalPaginas: 1,
            esUltima: true
          }
        });
      })
    );
  }

  bloquearIp(ip: string, motivo: string, minutos: number = 60): Observable<ResultadoApi<void>> {
    const params = new HttpParams()
      .set('ip', ip)
      .set('motivo', motivo)
      .set('minutos', minutos.toString());
    return this.http.post<ResultadoApi<void>>(`${environment.gatewayUrl}/api/v1/seguridad/lista-negra/bloquear`, null, { params });
  }

  desbloquearIp(ip: string): Observable<ResultadoApi<void>> {
    const params = new HttpParams().set('ip', ip);
    return this.http.delete<ResultadoApi<void>>(`${environment.gatewayUrl}/api/v1/seguridad/lista-negra/desbloquear`, { params });
  }

  obtenerOtpsBloqueados(): Observable<ResultadoApi<any[]>> {
    return this.http.get<ResultadoApi<any[]>>(`${environment.gatewayUrl}/api/v1/mensajeria/admin/bloqueados`).pipe(
      catchError(() => {
        console.warn('Backend offline - usando mock para OTPs bloqueados');
        return of({
          exito: true,
          mensaje: 'Bloqueados OTP recuperados (MOCK)',
          estado: 200,
          datos: [
            { usuarioId: '8f7e6d5c-4321-8765-09ba-fedcba987654', intentos: 5, bloqueado: true, bloqueadoHasta: '2024-06-02T18:00:00' }
          ]
        });
      })
    );
  }

  desbloquearUsuarioOtp(usuarioId: string): Observable<ResultadoApi<void>> {
    return this.http.delete<ResultadoApi<void>>(`${environment.gatewayUrl}/api/v1/mensajeria/admin/bloqueos/${usuarioId}`);
  }

  verificarSaludServicio(nombre: string): Observable<any> {
    return this.http.get<ResultadoApi<any>>(`${environment.gatewayUrl}/api/v1/admin/monitoreo/salud/${nombre}`).pipe(
      map(res => {
        return res.datos || { status: 'DOWN' };
      }),
      catchError(err => {
        return of({ status: 'DOWN', error: err.message });
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
      { nombre: 'ms-suscripciones', puerto: 8088, estado: 'healthy', latencia: '25ms', descripcion: 'Suscripciones y facturación periódica' }
    ],
    pagos: [
      { id: 'PAG-0091', usuario: 'carlos.mendez', monto: 'S/ 49.90', plan: 'PRO', estado: 'EXITOSO' },
      { id: 'PAG-0090', usuario: 'ana.torres', monto: 'S/ 19.90', plan: 'BASIC', estado: 'PENDIENTE' },
      { id: 'PAG-0089', usuario: 'luis.ramos', monto: 'S/ 99.90', plan: 'ENTERPRISE', estado: 'EXITOSO' },
      { id: 'PAG-0088', usuario: 'sofia.vega', monto: 'S/ 49.90', plan: 'PRO', estado: 'FALLIDO' }
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
