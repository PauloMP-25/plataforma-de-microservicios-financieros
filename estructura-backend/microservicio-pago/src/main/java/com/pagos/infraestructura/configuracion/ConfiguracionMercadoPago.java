package com.pagos.infraestructura.configuracion;

import com.mercadopago.MercadoPagoConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura para el SDK oficial de Mercado Pago.
 *
 * <p>Inicializa el SDK al arranque de la aplicación con el Access Token
 * inyectado desde {@link PropiedadesMercadoPago}. El SDK de Mercado Pago
 * es estático por diseño (singleton global), por lo que esta configuración
 * se ejecuta una sola vez al levantar el contexto de Spring.</p>
 *
 * <p>El SDK es compatible con Java 21 y Spring Boot 3.x, tanto en entorno
 * local (sandbox) como desplegado en producción (access token de producción).</p>
 *
 * @author LUKA APP Team
 * @see PropiedadesMercadoPago
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(PropiedadesMercadoPago.class)
public class ConfiguracionMercadoPago {

    /**
     * Inicializa el SDK de Mercado Pago con el access token configurado.
     *
     * @param propiedades Propiedades de Mercado Pago inyectadas desde el entorno.
     * @return El bean de configuración global del SDK.
     */
    @Bean
    public MercadoPagoConfig mercadoPagoConfig(PropiedadesMercadoPago propiedades) {
        MercadoPagoConfig.setAccessToken(propiedades.getAccessToken());
        log.info("[MERCADOPAGO] SDK inicializado correctamente. Modo: {}",
                propiedades.getAccessToken().startsWith("TEST-") ? "SANDBOX" : "PRODUCCION");
        return new MercadoPagoConfig();
    }
}
