
// ── Categoría (microservicio-nucleo-financiero) ──
export type TipoMovimiento = 'INGRESO' | 'GASTO';

export interface CategoriaDTO {
  id:          string;   // UUID
  nombre:      string;
  descripcion: string;
  icono:       string;
  tipo:        TipoMovimiento;
}

export interface CategoriaRequestDTO {
  nombre:      string;
  descripcion: string;
  icono:       string;
  tipo:        TipoMovimiento;
}
