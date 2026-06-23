package com.libreria.comun.dtos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado por ms-pagos cuando un pago se confirma exitosamente.
 * Consumido por: ms-usuario, ms-auditoria, ms-nucleo-financiero, ms-mensajeria, ms-suscripciones.
 *
 * <p>El campo {@code referenciaPasarela} es agnóstico a la pasarela de pago:
 * <ul>
 *   <li>Para Stripe: contiene el Checkout Session ID (prefijo {@code cs_}).</li>
 *   <li>Para Mercado Pago: contiene el Preapproval ID.</li>
 * </ul>
 * </p>
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

    /**
     * Identificador de referencia de la pasarela de pago externa.
     * Agnóstico al proveedor: Stripe Session ID o Mercado Pago Preapproval ID.
     */
    @JsonProperty("referencia_pasarela")
    @JsonAlias("stripe_session_id")
    String referenciaPasarela
) {}
