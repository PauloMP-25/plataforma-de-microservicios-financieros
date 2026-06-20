package com.cliente.aplicacion.dtos.respuestas;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * DTO de salida para PerfilFinanciero.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaPerfilFinanciero(
        String        ocupacion,
        BigDecimal    ingresoMensual,
        String        estiloVida,
        String        tonoIA
) {}
