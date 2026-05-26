package com.libreria.comun.autoconfiguracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.mensajeria.PublicadorEventosBase;
import com.libreria.comun.seguridad.FiltroJwt;
import com.libreria.comun.seguridad.PuntoEntradaJwt;
import com.libreria.comun.seguridad.ServicioJwt;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Import;

/**
 * Registro automático de los componentes de la librería LUKA COMMONS.
 * <p>
 * Gracias a esta configuración, los microservicios solo necesitan incluir
 * la dependencia para tener acceso a la seguridad, mensajería y manejo de
 * errores.
 * </p>
 */
@AutoConfiguration
@org.springframework.boot.autoconfigure.AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
@Import(ControladorSaludCustom.class)
public class LukaConfiguracion {

    @Value("${spring.application.name:luka-service}")
    private String applicationName;

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitPassword;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String rabbitVirtualHost;

    // --- SEGURIDAD ---



    @org.springframework.context.annotation.Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ServletSecurityConfig {

        @Bean
        @ConditionalOnMissingBean
        public ServicioJwt servicioJwt() {
            return new ServicioJwt();
        }

        @Bean
        @ConditionalOnMissingBean
        public FiltroJwt filtroJwt(ServicioJwt servicioJwt) {
            return new FiltroJwt(servicioJwt);
        }

        @Bean
        @ConditionalOnMissingBean
        public PuntoEntradaJwt puntoEntradaJwt(ObjectMapper objectMapper) {
            return new PuntoEntradaJwt(objectMapper);
        }

        @Bean
        @ConditionalOnMissingBean
        public ManejadorGlobalExcepcionesBase manejadorGlobalExcepciones() {
            return new ManejadorGlobalExcepcionesBase() {
            };
        }
    }

    // --- MENSAJERÍA (RABBITMQ) ---

    @SuppressWarnings("null")
    @Bean
    @ConditionalOnMissingBean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(rabbitHost, rabbitPort);
        factory.setUsername(rabbitUsername);
        factory.setPassword(rabbitPassword);
        factory.setVirtualHost(rabbitVirtualHost);
        factory.setConnectionNameStrategy(cf -> applicationName);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @SuppressWarnings("null")
    @Bean
    @ConditionalOnMissingBean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public PublicadorEventosBase publicadorEventosBase(RabbitTemplate rabbitTemplate) {
        return new PublicadorEventosBase(rabbitTemplate);
    }

    /**
     * Configuración global y automatizada de OpenAPI (Swagger) para todos los microservicios
     * que implementen 'libreria-comun'. Habilita la autorización por token JWT Bearer de forma centralizada
     * y genera dinámicamente el título y descripción basándose en el nombre de cada microservicio.
     * 
     * @return Objeto OpenAPI configurado.
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI customOpenAPI() {
        final String nombreEsquemaSeguridad = "BearerToken";
        
        // Formatear el nombre de la aplicación para que se vea legible en Swagger
        String prettyAppName = "Servicio LUKA";
        if (applicationName != null && !applicationName.isEmpty()) {
            prettyAppName = applicationName.substring(0, 1).toUpperCase() + applicationName.substring(1).replace("-", " ");
        }

        return new OpenAPI()
                .info(new Info()
                        .title("LUKA - " + prettyAppName)
                        .version("1.0.0")
                        .description("API auto-documentada del componente **" + prettyAppName + "** para el ecosistema SaaS de gestión financiera LUKA.")
                        .contact(new Contact()
                                .name("Paulo Moron - Cloud Architect")
                                .email("paulo@luka-financial.com")
                                .url("https://luka-financial.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                // Aplica de forma global a todos los endpoints el botón Authorize para testing
                .addSecurityItem(new SecurityRequirement().addList(nombreEsquemaSeguridad))
                .components(new Components()
                        .addSecuritySchemes(nombreEsquemaSeguridad, new SecurityScheme()
                                .name(nombreEsquemaSeguridad)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingresa tu token JWT en formato Bearer (sin prefijos) para habilitar el acceso a los endpoints protegidos de este microservicio.")));
    }
}
