package com.pagos.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas para la integración con Mercado Pago.
 *
 * <p>Usa el patrón {@code @ConfigurationProperties} para cargar las variables de entorno
 * tipadas. La anotación {@code @Validated} + {@code @NotBlank} garantiza un arranque
 * "fail-fast": si alguna variable crítica no está configurada, la aplicación falla
 * en el inicio con un mensaje descriptivo en lugar de fallar en tiempo de ejecución.</p>
 *
 * <p>Aplica el mismo patrón que {@link PropiedadesStripe} para consistencia.</p>
 *
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "mercadopago")
public class PropiedadesMercadoPago {

    /**
     * Access Token de Mercado Pago (TEST-xxx para sandbox, APP_USR-xxx para producción).
     * Variable de entorno: {@code MERCADOPAGO_ACCESS_TOKEN}.
     */
    @NotBlank(message = "El access token de Mercado Pago (mercadopago.access-token) es obligatorio.")
    private String accessToken;

    /**
     * Secreto del Webhook de Mercado Pago para validación de firmas HMAC-SHA256.
     * Variable de entorno: {@code MERCADOPAGO_WEBHOOK_SECRET}.
     */
    @NotBlank(message = "El webhook secret de Mercado Pago (mercadopago.webhook-secret) es obligatorio.")
    private String webhookSecret;

    /**
     * URL de redirección tras un pago autorizado exitosamente.
     * Variable de entorno: {@code FRONTEND_URL}/suscripcion/exito.
     */
    @NotBlank(message = "La URL de éxito de Mercado Pago (mercadopago.success-url) es obligatoria.")
    private String successUrl;

    /**
     * URL de redirección si el usuario cancela el proceso de pago.
     * Variable de entorno: {@code FRONTEND_URL}/suscripcion/cancelado.
     */
    @NotBlank(message = "La URL de cancelación de Mercado Pago (mercadopago.cancel-url) es obligatoria.")
    private String cancelUrl;
}
