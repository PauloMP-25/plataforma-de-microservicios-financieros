package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO que representa la solicitud para registrar un nuevo movimiento financiero.
 *
 * @param usuarioId     ID del usuario dueño de la transacción.
 * @param nombreCliente Nombre o referencia del cliente (para trazabilidad).
 * @param monto         Monto económico de la operación.
 * @param tipo          Naturaleza de la operación (INGRESO/GASTO).
 * @param categoriaId   ID de la categoría pre-existente.
 * @param metodoPago    Forma en que se realizó el pago.
 * @param etiquetas     Metadatos opcionales separados por comas.
 * @param notas         Detalle adicional de la transacción.
 */
public record SolicitudTransaccion(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombreCliente,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimiento tipo,

        @NotNull(message = "El ID de categoría es obligatorio")
        UUID categoriaId,

        @NotNull(message = "El método de pago es obligatorio")
        MetodoPago metodoPago,

        String etiquetas,
        String notas
) {
}
