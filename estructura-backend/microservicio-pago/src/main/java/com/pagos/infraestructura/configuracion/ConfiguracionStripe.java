package com.pagos.infraestructura.configuracion;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configura el SDK de Stripe con la clave de API al arrancar el microservicio.
 */
@Slf4j
@Configuration
public class ConfiguracionStripe {

    @Value("${stripe.api-key}")
    private String claveApiStripe;

    @PostConstruct
    public void inicializar() {
        if (claveApiStripe == null || claveApiStripe.isEmpty()) {
            log.warn("La clave de API de Stripe no está configurada. Algunas funcionalidades pueden fallar.");
        } else {
            Stripe.apiKey = claveApiStripe;
            log.info("Stripe SDK inicializado correctamente.");
        }
    }
}
