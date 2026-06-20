package com.cliente.aplicacion.dtos.respuestas;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de salida para MetaAhorro.
 * Incluye el porcentaje de progreso calculado en dominio.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaMetaAhorro(
                UUID id,
                String nombre,
                BigDecimal montoObjetivo,
                BigDecimal montoActual,
                BigDecimal porcentajeProgreso,
                LocalDate fechaLimite,
                Boolean completada,
                String proposito) {
}
