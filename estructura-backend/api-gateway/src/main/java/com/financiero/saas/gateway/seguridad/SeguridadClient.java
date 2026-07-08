package com.financiero.saas.gateway.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Cliente reactivo para la integración de seguridad perimetral.
 * <p>
 * Este componente es responsable de consultar el estado de una IP (libre o
 * bloqueada).
 * Utiliza un patrón de caché *Cache-Aside* con Redis para minimizar la latencia y
 * evitar saturar el microservicio de auditoría en cada petición entrante.
 * Incorpora resiliencia ante fallos garantizando que el tráfico legítimo no se
 * bloquee si los servicios de seguridad dejan de responder.
 * </p>
 * 
 * @version 2.0.0
 * @since 2026-05-10
 */
@Service
@Slf4j
public class SeguridadClient {

    private static final String PREFIJO_CACHE = "bloqueo:ip:";
    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, Boolean> redisTemplate;
    private final String urlBaseAuditoria;

    public SeguridadClient(WebClient.Builder webClientBuilder, 
                           ReactiveRedisTemplate<String, Boolean> redisTemplate,
                           @org.springframework.beans.factory.annotation.Value("${URL_PROD_AUDITORIA:https://luka-auditoria.onrender.com}") String urlProdAuditoria) {
        this.redisTemplate = redisTemplate;
        
        // Determinar si usamos conexión por nombre de servicio (LoadBalancer interno) o conexión directa (Render)
        if (urlProdAuditoria.startsWith("lb://") || urlProdAuditoria.contains("microservicio-auditoria")) {
            this.webClient = webClientBuilder.build();
            this.urlBaseAuditoria = "http://microservicio-auditoria/api/v1/seguridad/verificar-ip/";
        } else {
            this.webClient = WebClient.create();
            this.urlBaseAuditoria = urlProdAuditoria.endsWith("/") 
                ? urlProdAuditoria + "api/v1/seguridad/verificar-ip/" 
                : urlProdAuditoria + "/api/v1/seguridad/verificar-ip/";
        }
    }

    /**
     * Verifica asíncronamente si una IP está bloqueada por políticas de seguridad.
     * 
     * @param ip Dirección IP a consultar.
     * @return {@link Mono} que emite {@code true} si está bloqueada, o
     *         {@code false} si está libre.
     */
    public Mono<Boolean> estaBloqueada(String ip) {
        String claveCache = PREFIJO_CACHE + ip;

        return redisTemplate.opsForValue().get(claveCache)
                .doOnNext(bloqueada -> log.debug("[SEGURIDAD-CACHE] HIT para IP {}: Bloqueada={}", ip, bloqueada))
                .switchIfEmpty(Mono.defer(() -> consultarAuditoriaYCachear(ip, claveCache)))
                .onErrorResume(error -> {
                    log.error(
                            "[SEGURIDAD-CLIENTE] Error verificando IP {}: {}. Falla segura activada (permitiendo tráfico).",
                            ip, error.getMessage());
                     return Mono.just(false); // Falla segura: permitir si el sistema de seguridad falla
                });
    }

    @SuppressWarnings("null")
    private Mono<Boolean> consultarAuditoriaYCachear(String ip, String claveCache) {
        log.debug("[SEGURIDAD-CACHE] MISS para IP {}. Consultando microservicio...", ip);

        return webClient.get()
                .uri(urlBaseAuditoria + ip)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(respuesta -> {
                    // Esperamos el DTO dentro de data de ResultadoApi: {"exito":true, "data": {"bloqueada": true/false}}
                    JsonNode data = respuesta.get("data");
                    if (data != null && data.has("bloqueada")) {
                        return data.get("bloqueada").asBoolean();
                    }
                    return false;
                })
                .timeout(Duration.ofMillis(1500))
                .onErrorResume(error -> {
                    log.warn("[SEGURIDAD-CLIENTE] Latencia excedida o error en Auditoría. Fallback seguro activado: {}", error.getMessage());
                    return Mono.just(false); 
                })
                .flatMap(bloqueada ->
                        // Guardar en Redis con TTL de 60 segundos
                        redisTemplate.opsForValue().set(claveCache, bloqueada, Duration.ofSeconds(60))
                                .thenReturn(bloqueada));
    }
}
