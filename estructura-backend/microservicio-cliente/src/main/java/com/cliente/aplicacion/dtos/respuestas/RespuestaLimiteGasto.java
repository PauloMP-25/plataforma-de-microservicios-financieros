package com.cliente.aplicacion.dtos.respuestas;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida para LimiteGasto.
 *
 * @param id Identificador único del límite.
 * @param usuarioId Identificador del usuario propietario.
 * @param nombre Nombre descriptivo del presupuesto.
 * @param montoLimite Monto máximo permitido.
 * @param porcentajeAlerta Porcentaje de consumo para la alerta.
 * @param fechaInicio Fecha de inicio del periodo.
 * @param fechaFin Fecha de fin del periodo.
 * @param activo Estado activo del límite.
 * @param fechaCreacion Fecha y hora de creación en base de datos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaLimiteGasto(
        UUID id,
        UUID usuarioId,
        String nombre,
        BigDecimal montoLimite,
        Integer porcentajeAlerta,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo,
        LocalDateTime fechaCreacion) {

}
