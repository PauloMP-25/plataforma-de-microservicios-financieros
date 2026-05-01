package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Categoria;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import java.util.UUID;

public record CategoriaDTO(
    UUID id,
    String nombre,
    String descripcion,
    String icono,
    TipoMovimiento tipo
) {
    public static CategoriaDTO desde(Categoria entidad) {
        return new CategoriaDTO(
            entidad.getId(),
            entidad.getNombre(),
            entidad.getDescripcion(),
            entidad.getIcono(),
            entidad.getTipo()
        );
    }
}
