package com.cliente.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida para LimiteGasto.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaLimiteGasto(
        UUID id,
        UUID usuarioId,
        BigDecimal montoLimite,
        Integer porcentajeAlerta,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo,
        LocalDateTime fechaCreacion) {

}
