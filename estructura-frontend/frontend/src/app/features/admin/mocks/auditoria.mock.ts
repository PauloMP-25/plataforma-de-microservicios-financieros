import { 
  AuditoriaAccesoDTO, 
  AuditoriaTransaccionalDTO, 
  RegistroAuditoriaDTO 
} from '../../../core/models/auditoria/auditoria.model';

export const mockAccesos: AuditoriaAccesoDTO[] = [
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

export const mockTransacciones: AuditoriaTransaccionalDTO[] = [
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

export const mockRegistros: RegistroAuditoriaDTO[] = [
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
