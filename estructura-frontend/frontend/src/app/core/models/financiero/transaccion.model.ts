
// ── Transacción ──
export interface TransaccionDTO {
  id:          string;
  usuarioId:   string;
  categoriaId: string;
  monto:       number;
  tipo:        String;
  descripcion: string;
  fecha:       string;
}
