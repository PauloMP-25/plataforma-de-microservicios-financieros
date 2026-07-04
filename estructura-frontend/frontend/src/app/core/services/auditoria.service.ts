import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../enviroments/environment';
import {
  AuditoriaAccesoDTO,
  AuditoriaAccesoRequestDTO,
  AuditoriaTransaccionalDTO,
  AuditoriaTransaccionalRequestDTO,
  PaginaAuditoriaAcceso,
  PaginaAuditoriaTransaccional,
  PaginaRegistroAuditoria,
  RegistroAuditoriaDTO,
  RegistroAuditoriaRequestDTO,
  RespuestaVerificacionIpDTO
} from '../models/auditoria/auditoria.model';

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private base = `${environment.gatewayUrl}/api/v1/auditoria`;

  // Mocks de auditoría para resguardo (fallback)
  private readonly mockAccesos: AuditoriaAccesoDTO[] = [
    {
      id: 'acc-001',
      usuarioId: '8f7e6d5c-4321-8765-09ba-fedcba987654',
      ipOrigen: '190.235.12.45',
      navegador: 'Chrome 125 / Windows 11',
      estado: 'EXITO',
      detalleError: '',
      fecha: new Date(Date.now() - 1000 * 60 * 15).toISOString()
    },
    {
      id: 'acc-002',
      usuarioId: '3d2c1b0a-8765-4321-fedc-ba9876543210',
      ipOrigen: '185.45.23.1',
      navegador: 'Firefox 120 / Linux',
      estado: 'FALLO',
      detalleError: 'Contraseña incorrecta (Intento 1)',
      fecha: new Date(Date.now() - 1000 * 60 * 45).toISOString()
    },
    {
      id: 'acc-003',
      usuarioId: '3d2c1b0a-8765-4321-fedc-ba9876543210',
      ipOrigen: '185.45.23.1',
      navegador: 'Firefox 120 / Linux',
      estado: 'FALLO',
      detalleError: 'Contraseña incorrecta (Intento 2)',
      fecha: new Date(Date.now() - 1000 * 60 * 44).toISOString()
    },
    {
      id: 'acc-004',
      usuarioId: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      ipOrigen: '200.48.33.91',
      navegador: 'Safari / iPhone iOS 17',
      estado: 'EXITO',
      detalleError: '',
      fecha: new Date(Date.now() - 1000 * 60 * 180).toISOString()
    }
  ];

  private readonly mockTransacciones: AuditoriaTransaccionalDTO[] = [
    {
      id: 'tx-001',
      usuarioId: '8f7e6d5c-4321-8765-09ba-fedcba987654',
      servicioOrigen: 'ms-pagos',
      entidadAfectada: 'pagos',
      entidadId: 'fa7b8c9d-1234-5678-90ab-cdef01234567',
      valorAnterior: '{"estado": "PENDIENTE", "monto": 99.90, "moneda": "PEN"}',
      valorNuevo: '{"estado": "EXITOSO", "monto": 99.90, "moneda": "PEN"}',
      fecha: new Date(Date.now() - 1000 * 60 * 30).toISOString()
    },
    {
      id: 'tx-002',
      usuarioId: '3d2c1b0a-8765-4321-fedc-ba9876543210',
      servicioOrigen: 'ms-cliente',
      entidadAfectada: 'limites_gasto',
      entidadId: 'lim-9912',
      valorAnterior: '{"limiteMensual": 1500.00, "categoria": "ENTRETENIMIENTO"}',
      valorNuevo: '{"limiteMensual": 800.00, "categoria": "ENTRETENIMIENTO"}',
      fecha: new Date(Date.now() - 1000 * 60 * 150).toISOString()
    },
    {
      id: 'tx-003',
      usuarioId: '1a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d',
      servicioOrigen: 'ms-usuario',
      entidadAfectada: 'usuarios',
      entidadId: 'usr-8891',
      valorAnterior: '{"telefono": "987654321", "dobleFactor": false}',
      valorNuevo: '{"telefono": "987654321", "dobleFactor": true}',
      fecha: new Date(Date.now() - 1000 * 60 * 360).toISOString()
    }
  ];

  private readonly mockRegistros: RegistroAuditoriaDTO[] = [
    {
      id: 'reg-001',
      fechaHora: new Date(Date.now() - 1000 * 60 * 10).toISOString(),
      usuario: 'admin.carlos',
      accion: 'BLOQUEAR_IP',
      modulo: 'SEGURIDAD',
      ipOrigen: '192.168.1.10',
      detalles: 'Se bloqueó manualmente la IP 185.45.23.1 debido a intentos fallidos de OTP'
    },
    {
      id: 'reg-002',
      fechaHora: new Date(Date.now() - 1000 * 60 * 25).toISOString(),
      usuario: 'admin.carlos',
      accion: 'CORREGIR_ESTADO_PAGO',
      modulo: 'PAGOS',
      ipOrigen: '192.168.1.10',
      detalles: 'Corrección manual de estado de transacción fa7b8c9d a EXITOSO'
    },
    {
      id: 'reg-003',
      fechaHora: new Date(Date.now() - 1000 * 60 * 120).toISOString(),
      usuario: 'maria.gomez',
      accion: 'CREAR_META',
      modulo: 'CLIENTE',
      ipOrigen: '200.48.33.91',
      detalles: 'Creación de meta de ahorro: Viaje de vacaciones. Monto objetivo: S/ 5000'
    },
    {
      id: 'reg-004',
      fechaHora: new Date(Date.now() - 1000 * 60 * 400).toISOString(),
      usuario: 'sistema.luka',
      accion: 'REINICIAR_MICROSERVICIO',
      modulo: 'INFRAESTRUCTURA',
      ipOrigen: 'localhost',
      detalles: 'Se ejecutó orden de reinicio del servicio ms-suscripciones exitosamente'
    }
  ];

  constructor(private http: HttpClient) {}

  registrarAcceso(payload: AuditoriaAccesoRequestDTO): Observable<AuditoriaAccesoDTO> {
    return this.http.post<AuditoriaAccesoDTO>(`${this.base}/accesos`, payload);
  }

  listarAccesos(pagina = 0, tamanio = 20): Observable<PaginaAuditoriaAcceso> {
    const params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    return this.http.get<PaginaAuditoriaAcceso>(`${this.base}/accesos`, { params }).pipe(
      catchError(() => {
        console.warn('Auditoria backend offline - usando mock para accesos');
        const total = this.mockAccesos.length;
        return of({
          content: this.mockAccesos.slice(pagina * tamanio, (pagina + 1) * tamanio),
          totalElements: total,
          totalPages: Math.ceil(total / tamanio),
          number: pagina,
          size: tamanio
        });
      })
    );
  }

  verificarIp(ip: string): Observable<RespuestaVerificacionIpDTO> {
    return this.http.get<RespuestaVerificacionIpDTO>(`${this.base}/verificar-ip/${ip}`);
  }

  registrarTransaccion(payload: AuditoriaTransaccionalRequestDTO): Observable<AuditoriaTransaccionalDTO> {
    return this.http.post<AuditoriaTransaccionalDTO>(`${this.base}/transacciones`, payload);
  }

  listarTransacciones(filtros: {
    servicioOrigen?: string;
    desde?: string;
    hasta?: string;
    pagina?: number;
    tamanio?: number;
  } = {}): Observable<PaginaAuditoriaTransaccional> {
    const pagina = filtros.pagina ?? 0;
    const tamanio = filtros.tamanio ?? 20;
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    if (filtros.servicioOrigen) params = params.set('servicioOrigen', filtros.servicioOrigen);
    if (filtros.desde) params = params.set('desde', filtros.desde);
    if (filtros.hasta) params = params.set('hasta', filtros.hasta);

    return this.http.get<PaginaAuditoriaTransaccional>(`${this.base}/transacciones`, { params }).pipe(
      catchError(() => {
        console.warn('Auditoria backend offline - usando mock para transacciones');
        let filtrados = this.mockTransacciones;
        if (filtros.servicioOrigen) {
          filtrados = filtrados.filter(t => t.servicioOrigen.toLowerCase().includes(filtros.servicioOrigen!.toLowerCase()));
        }
        const total = filtrados.length;
        return of({
          content: filtrados.slice(pagina * tamanio, (pagina + 1) * tamanio),
          totalElements: total,
          totalPages: Math.ceil(total / tamanio),
          number: pagina,
          size: tamanio
        });
      })
    );
  }

  registrarEvento(payload: RegistroAuditoriaRequestDTO): Observable<RegistroAuditoriaDTO> {
    return this.http.post<RegistroAuditoriaDTO>(`${this.base}/registrar`, payload);
  }

  listarRegistros(filtros: {
    modulo?: string;
    nivel?: string;
    pagina?: number;
    tamanio?: number;
  } = {}): Observable<PaginaRegistroAuditoria> {
    const pagina = filtros.pagina ?? 0;
    const tamanio = filtros.tamanio ?? 20;
    let params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    if (filtros.modulo) params = params.set('modulo', filtros.modulo);
    if (filtros.nivel) params = params.set('nivel', filtros.nivel);

    return this.http.get<PaginaRegistroAuditoria>(`${this.base}/registros`, { params }).pipe(
      catchError(() => {
        console.warn('Auditoria backend offline - usando mock para registros');
        let filtrados = this.mockRegistros;
        if (filtros.modulo) {
          filtrados = filtrados.filter(r => r.modulo === filtros.modulo);
        }
        const total = filtrados.length;
        return of({
          content: filtrados.slice(pagina * tamanio, (pagina + 1) * tamanio),
          totalElements: total,
          totalPages: Math.ceil(total / tamanio),
          number: pagina,
          size: tamanio
        });
      })
    );
  }
}

