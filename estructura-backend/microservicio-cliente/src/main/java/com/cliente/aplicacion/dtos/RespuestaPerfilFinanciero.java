package com.cliente.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida para PerfilFinanciero.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaPerfilFinanciero(
        UUID          id,
        UUID          usuarioId,
        String        ocupacion,
        BigDecimal    ingresoMensual,
        String        estiloVida,
        String        tonoIA,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}