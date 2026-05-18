package com.pagos.infraestructura.configuracion;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades centralizadas y validadas para la gestión de Stripe.
 * Falla rápido en el arranque si las variables de entorno no están configuradas correctamente.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "stripe")
public class PropiedadesStripe {

    @NotBlank(message = "La clave secreta de Stripe (stripe.api-key) es obligatoria.")
    private String apiKey;

    @NotBlank(message = "El webhook secret de Stripe (stripe.webhook-secret) es obligatorio.")
    private String webhookSecret;

    @NotBlank(message = "La URL de éxito de Stripe (stripe.success-url) es obligatoria.")
    private String successUrl;

    @NotBlank(message = "La URL de cancelación de Stripe (stripe.cancel-url) es obligatoria.")
    private String cancelUrl;

    @NotBlank(message = "La clave del producto PRO de Stripe (stripe.product-pro-id) es obligatoria.")
    private String productProId;

    @NotBlank(message = "La clave del precio PRO de Stripe (stripe.price-pro-id) es obligatoria.")
    private String priceProId;
}
