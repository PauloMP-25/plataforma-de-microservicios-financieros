/**
 * Modelo de Suscripción de Gastos
 * Representa suscripciones recurrentes como Netflix, Spotify, Gimnasio, etc.
 */

export type EstadoSuscripcion = 'ACTIVA' | 'PAUSADA' | 'VENCIDA';
export type FrecuenciaSuscripcion = 'DIARIO' | 'SEMANAL' | 'QUINCENAL' | 'MENSUAL' | 'TRIMESTRAL' | 'SEMESTRAL' | 'ANUAL';

export interface SuscripcionGasto {
  id: string;
  nombre: string;
  descripcion: string;
  categoria: string; // food, transport, health, home, leisure, study
  monto: number;
  frecuencia: FrecuenciaSuscripcion;
  fechaInicio: string; // ISO 8601
  proximoVencimiento: string; // ISO 8601
  estado: EstadoSuscripcion;
  fechaCreacion: string;
  ultimaActualizacion: string;
}

export interface SuscripcionDTO extends SuscripcionGasto {
  // Propiedades derivadas para UI
  diasParaVencimiento?: number;
  vencePronto?: boolean;
  gastosEsteAno?: number;
  gastosEsteMes?: number;
}

/**
 * Request para crear o actualizar suscripción
 */
export interface CrearSuscripcionRequest {
  nombre: string;
  descripcion: string;
  categoria: string;
  monto: number;
  frecuencia: FrecuenciaSuscripcion;
  fechaInicio: string;
}

export interface ActualizarSuscripcionRequest extends CrearSuscripcionRequest {
  id: string;
  estado?: EstadoSuscripcion;
}

/**
 * Response del servidor
 */
export interface SuscripcionApiResponse {
  datos: SuscripcionDTO[];
  total: number;
  pagina: number;
  tamano: number;
}

/**
 * Resumen de suscripciones para dashboard
 */
export interface ResumenSuscripciones {
  totalActivas: number;
  gastoMensualEstimado: number;
  proximoPago: SuscripcionDTO | null;
  cantidadTotal: number;
  proximasFechas: SuscripcionDTO[];
}

/**
 * Categorías disponibles con sus colores
 */
export const CATEGORIAS_SUSCRIPCION = [
  { id: 'food', nombre: 'Alimentación', color: '#FF7043', icon: 'fa-utensils' },
  { id: 'transport', nombre: 'Transporte', color: '#42A5F5', icon: 'fa-car' },
  { id: 'health', nombre: 'Salud', color: '#26C6DA', icon: 'fa-heart-pulse' },
  { id: 'home', nombre: 'Vivienda', color: '#FFA726', icon: 'fa-house' },
  { id: 'leisure', nombre: 'Entretenimiento', color: '#AB47BC', icon: 'fa-gamepad' },
  { id: 'study', nombre: 'Educación', color: '#66BB6A', icon: 'fa-graduation-cap' }
];

/**
 * Frecuencias disponibles
 */
export const FRECUENCIAS_SUSCRIPCION: Array<{ id: FrecuenciaSuscripcion; nombre: string; diasAproximado: number }> = [
  { id: 'DIARIO', nombre: 'Diario', diasAproximado: 1 },
  { id: 'SEMANAL', nombre: 'Semanal', diasAproximado: 7 },
  { id: 'QUINCENAL', nombre: 'Quincenal', diasAproximado: 15 },
  { id: 'MENSUAL', nombre: 'Mensual', diasAproximado: 30 },
  { id: 'TRIMESTRAL', nombre: 'Trimestral', diasAproximado: 90 },
  { id: 'SEMESTRAL', nombre: 'Semestral', diasAproximado: 180 },
  { id: 'ANUAL', nombre: 'Anual', diasAproximado: 365 }
];

/**
 * Estados disponibles
 */
export const ESTADOS_SUSCRIPCION = [
  { id: 'ACTIVA', nombre: 'Activa', color: '#22C55E' },
  { id: 'PAUSADA', nombre: 'Pausada', color: '#F59E0B' },
  { id: 'VENCIDA', nombre: 'Vencida', color: '#EF4444' }
];
