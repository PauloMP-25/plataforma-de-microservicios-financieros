package com.libreria.comun.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado por ms-pagos cuando un pago se confirma exitosamente.
 * Consumido por: ms-usuario, ms-auditoria, ms-nucleo-financiero, ms-mensajeria.
 */
public record EventoPagoExitosoDTO(

    @JsonProperty("pago_id")
    UUID pagoId,

    @JsonProperty("usuario_id")
    UUID usuarioId,

    @JsonProperty("email_usuario")
    String emailUsuario,

    @JsonProperty("plan_nuevo")
    String planNuevo,

    BigDecimal monto,
    String moneda,

    @JsonProperty("fecha_inicio_plan")
    LocalDateTime fechaInicioPlan,

    @JsonProperty("fecha_fin_plan")
    LocalDateTime fechaFinPlan,

    @JsonProperty("stripe_session_id")
    String stripeSessionId
) {}
