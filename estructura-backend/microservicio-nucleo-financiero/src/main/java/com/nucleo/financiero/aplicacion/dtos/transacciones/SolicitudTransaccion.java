package com.nucleo.financiero.aplicacion.dtos.transacciones;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion.MetodoPago;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SolicitudTransaccion(

    @NotNull(message = "El ID de usuario es obligatorio")
    UUID usuarioId,

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    String nombreCliente,

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    @Digits(integer = 13, fraction = 2, message = "Máximo 13 enteros y 2 decimales")
    BigDecimal monto,

    @NotNull(message = "El tipo (INGRESO/GASTO) es obligatorio")
    TipoMovimiento tipo,

    @NotNull(message = "El ID de la categoría es obligatorio")
    UUID categoriaId,

    LocalDateTime fechaTransaccion,

    @NotNull(message = "El método de pago es obligatorio")
    MetodoPago metodoPago,

    @Size(max = 300, message = "Las etiquetas no pueden superar 300 caracteres")
    String etiquetas,

    String notas
) {}
