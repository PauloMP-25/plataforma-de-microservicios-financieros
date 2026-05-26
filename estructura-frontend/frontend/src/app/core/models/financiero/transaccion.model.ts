import { TipoMovimiento } from './categoria.model';
 

export type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'DIGITAL';

export interface TransaccionDTO {
  id:                string;          
  usuarioId:         string;         
  nombreCliente:     string;         
  monto:             number;          
  tipo:              TipoMovimiento;  
  categoriaId:       string;          
  categoriaNombre:   string;          
  categoriaIcono:    string;          
  fechaTransaccion:  string;          
  metodoPago:        MetodoPago;
  etiquetas:         string | null;   
  notas:             string | null;
  fechaRegistro:     string;          
}
 
// ─── Lo que mandas al backend para crear/editar ───────────────────────────────
export interface TransaccionRequestDTO {
  usuarioId:         string;
  nombreCliente:     string;
  monto:             number;
  tipo:              TipoMovimiento;
  categoriaId:       string;
  fechaTransaccion:  string;          // ISO-8601
  metodoPago:        MetodoPago;
  etiquetas?:        string;          // opcional
  notas?:            string;          // opcional
}
 
// ─── Filtros para listarHistorial ─────────────────────────────────────────────
export interface TransaccionFiltros {
  usuarioId:    string;
  tipo?:        TipoMovimiento;   // sin tipo = todos (ingresos + gastos)
  categoriaId?: string;
  mes?:         number;           // 1–12
  anio?:        number;
  pagina?:      number;           // default 0
  tamanio?:     number;           // default 20, máx 100
}
