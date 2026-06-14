import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { AdminDashboardData } from '../models/admin-dashboard.model';

@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  obtenerResumen(): Observable<AdminDashboardData> {
    return of(this.mockData).pipe(delay(250));
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
