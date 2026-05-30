export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'DIGITAL';

export interface OptionItem {
  label: string;
  value: string;
}

export interface IngresoKpi {
  titulo: string;
  valor: string;
  subtitulo: string;
  color: 'emerald' | 'violet' | 'sky' | 'amber';
}

export interface DistribucionCategoria {
  categoria: string;
  monto: number;
  porcentaje: number;
  color: string;
}

export interface IngresoTendenciaPunto {
  periodo: string;
  monto: number;
}

export interface IngresoReciente {
  categoria: string;
  descripcion: string;
  monto: number;
  fecha: string;
}

export interface IngresoRegistro {
  fecha: string;
  monto: number;
  categoria: string;
  metodoPago: MetodoPago;
  etiquetas: string[];
  nota: string;
}

export interface IngresoFormData {
  monto: number;
  fechaTransaccion: string;
  descripcion: string;
  categoria: string;
  metodoPago: MetodoPago;
  etiquetas: string[];
}

