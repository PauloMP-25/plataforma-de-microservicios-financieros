package com.libreria.comun.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando el pago de una suscripción activa se confirma.
 */
public record EventoSuscripcionPagadaDTO(
    @JsonProperty("suscripcion_id")
    UUID suscripcionId,

    @JsonProperty("usuario_id")
    UUID usuarioId,

    @JsonProperty("plan")
    String plan,

    BigDecimal monto,

    @JsonProperty("fecha_vencimiento")
    LocalDateTime fechaVencimiento,

    @JsonProperty("fecha_pago")
    LocalDateTime fechaPago,

    @JsonProperty("correlation_id")
    String correlationId
) {}
