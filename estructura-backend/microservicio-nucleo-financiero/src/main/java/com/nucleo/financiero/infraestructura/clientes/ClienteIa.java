package com.nucleo.financiero.infraestructura.clientes;

import com.nucleo.financiero.aplicacion.dtos.ia.RespuestaIaDTO;
import com.nucleo.financiero.aplicacion.dtos.ia.SolicitudIaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Cliente HTTP para el microservicio-ia (Python/FastAPI).
 *
 * Usa WebClient (reactor) en modo bloqueante con .block():
 *   - El microservicio financiero es bloqueante (MVC), no reactivo.
 *   - .block() con timeout garantiza que nunca cuelga un hilo indefinidamente.
 *   - Si se migra a WebFlux, eliminar el .block() y propagar Mono<RespuestaIaDTO>.
 *
 * Manejo de errores:
 *   - 4xx: error del cliente (payload inválido) → lanza IllegalArgumentException
 *   - 5xx: error del servidor IA → lanza RuntimeException (el flujo hace NACK)
 *   - Timeout: si Gemini tarda más de gemini.timeout.segundos → Optional.empty()
 *   - Conexión rechazada: microservicio-ia no disponible → Optional.empty()
 *
 * Principio: un fallo de IA NUNCA debe bloquear el registro de una transacción.
 * El servicio que llama a este cliente debe manejar el Optional vacío.
 */
@Component
@Slf4j
public class ClienteIa {

    private final WebClient webClient;
    private final int timeoutSegundos;

    public ClienteIa(
            WebClient.Builder webClientBuilder,
            @Value("${ia.service.url:http://localhost:8086}") String urlBase,
            @Value("${ia.service.timeout-segundos:30}") int timeoutSegundos) {

        this.webClient = webClientBuilder
                .baseUrl(urlBase)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept",       MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.timeoutSegundos = timeoutSegundos;

        log.info("[CLIENTE-IA] Inicializado → baseUrl={}, timeout={}s", urlBase, timeoutSegundos);
    }

    // ── Método principal ──────────────────────────────────────────────────────

    /**
     * Envía una solicitud de análisis al microservicio-ia y retorna el resultado.
     *
     * @param solicitud DTO construido por ServicioIa con historial y contexto.
     * @return Optional con la respuesta de Gemini, o empty() si el servicio falla.
     */
    public Optional<RespuestaIaDTO> analizar(SolicitudIaDTO solicitud) {
        log.info(
            "[CLIENTE-IA] Enviando solicitud | usuario={} | tipo={} | módulo={} | meses={}",
            solicitud.getIdUsuario(),
            solicitud.getTipoSolicitud(),
            solicitud.getModuloSolicitado(),
            solicitud.getHistorialMensual() != null ? solicitud.getHistorialMensual().size() : 0
        );

        try {
            RespuestaIaDTO respuesta = webClient
                    .post()
                    .uri("/api/v1/ia/analizar")
                    .bodyValue(solicitud)
                    .retrieve()
                    .onStatus(
                        HttpStatusCode::is4xxClientError,
                        response -> response.bodyToMono(String.class).flatMap(body -> {
                            log.error("[CLIENTE-IA] Error 4xx — el payload es inválido: {}", body);
                            return Mono.error(new IllegalArgumentException(
                                "Solicitud inválida para microservicio-ia: " + body
                            ));
                        })
                    )
                    .onStatus(
                        HttpStatusCode::is5xxServerError,
                        response -> response.bodyToMono(String.class).flatMap(body -> {
                            log.error("[CLIENTE-IA] Error 5xx en microservicio-ia: {}", body);
                            return Mono.error(new RuntimeException(
                                "Error interno en microservicio-ia: " + body
                            ));
                        })
                    )
                    .bodyToMono(RespuestaIaDTO.class)
                    .timeout(Duration.ofSeconds(timeoutSegundos))
                    .block();

            if (respuesta != null) {
                log.info(
                    "[CLIENTE-IA] Respuesta recibida | módulo={} | kpi={} {}",
                    respuesta.getTipoModulo(),
                    respuesta.getKpiPrincipal(),
                    respuesta.getKpiLabel() != null ? respuesta.getKpiLabel() : ""
                );
            }
            return Optional.ofNullable(respuesta);

        } catch (WebClientResponseException ex) {
            log.error(
                "[CLIENTE-IA] HTTP {} al llamar microservicio-ia: {}",
                ex.getStatusCode(), ex.getResponseBodyAsString()
            );
            return Optional.empty();

        } catch (Exception ex) {
            // Timeout, conexión rechazada, etc.
            log.error(
                "[CLIENTE-IA] Error de comunicación con microservicio-ia (no bloqueante): {}",
                ex.getMessage()
            );
            return Optional.empty();
        }
    }
}
