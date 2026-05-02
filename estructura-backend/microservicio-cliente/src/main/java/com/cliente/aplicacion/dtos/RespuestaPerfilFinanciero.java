package com.cliente.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de salida para PerfilFinanciero.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaPerfilFinanciero(
        String        ocupacion,
        BigDecimal    ingresoMensual,
        String        estiloVida,
        String        tonoIA,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}