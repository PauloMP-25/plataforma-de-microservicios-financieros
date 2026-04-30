package com.nucleo.financiero.infraestructura.configuracion;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuración del WebClient para comunicación con microservicio-ia.
 *
 * Timeouts configurados en tres niveles:
 *   1. connectionTimeout: tiempo máximo para establecer la conexión TCP (5s).
 *   2. readTimeout:       tiempo máximo esperando la respuesta (30s — Gemini puede ser lento).
 *   3. writeTimeout:      tiempo máximo enviando el request (10s).
 *
 * El timeout de 30s en lectura es deliberado: Gemini 1.5 Flash puede tardar
 * entre 3-15 segundos dependiendo del tamaño del prompt y la carga de la API.
 *
 * Añadir al pom.xml si no está:
 *   <dependency>
 *       <groupId>org.springframework.boot</groupId>
 *       <artifactId>spring-boot-starter-webflux</artifactId>
 *   </dependency>
 */
@Configuration
public class ConfiguracionWebClient {

    @Value("${ia.service.connection-timeout-ms:5000}")
    private int connectionTimeoutMs;

    @Value("${ia.service.read-timeout-segundos:30}")
    private int readTimeoutSegundos;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .responseTimeout(Duration.ofSeconds(readTimeoutSegundos))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutSegundos, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
