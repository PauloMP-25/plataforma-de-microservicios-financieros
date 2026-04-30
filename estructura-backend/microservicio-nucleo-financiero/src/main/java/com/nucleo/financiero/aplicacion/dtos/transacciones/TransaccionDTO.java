package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import com.nucleo.financiero.dominio.entidades.Transaccion.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransaccionDTO(
    UUID id,
    UUID usuarioId,
    String nombreCliente,
    BigDecimal monto,
    TipoMovimiento tipo,
    UUID categoriaId,
    String categoriaNombre,
    String categoriaIcono,
    LocalDateTime fechaTransaccion,
    MetodoPago metodoPago,
    String etiquetas,
    String notas,
    LocalDateTime fechaRegistro
) {
    public static TransaccionDTO desde(Transaccion entidad) {
        return new TransaccionDTO(
            entidad.getId(),
            entidad.getUsuarioId(),
            entidad.getNombreCliente(),
            entidad.getMonto(),
            entidad.getTipo(),
            entidad.getCategoria().getId(),
            entidad.getCategoria().getNombre(),
            entidad.getCategoria().getIcono(),
            entidad.getFechaTransaccion(),
            entidad.getMetodoPago(),
            entidad.getEtiquetas(),
            entidad.getNotas(),
            entidad.getFechaRegistro()
        );
    }
}
