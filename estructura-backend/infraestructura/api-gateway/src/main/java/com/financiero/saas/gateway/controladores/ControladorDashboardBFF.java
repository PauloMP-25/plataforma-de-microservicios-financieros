package com.financiero.saas.gateway.controladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador BFF (Backend-For-Frontend) para el Dashboard de LUKA.
 * <p>
 * Agrega de forma reactiva y concurrente el perfil de usuario (ms-cliente)
 * y los KPIs calientes con transacciones recientes (ms-ia/Redis),
 * reduciendo ráfagas de red en el cliente y mejorando los tiempos de respuesta.
 * </p>
 *
 * @author Antigravity
 * @version 1.0.3
 * @since 2026-06-01
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Slf4j
public class ControladorDashboardBFF {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient clientWebClient;
    private final WebClient iaWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${URL_PROD_CLIENTE:http://localhost:8083}")
    private String urlProdCliente;

    @Value("${URL_PROD_IA:http://localhost:8086}")
    private String urlProdIA;

    public ControladorDashboardBFF(
            WebClient.Builder webClientBuilder,
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${URL_PROD_CLIENTE:http://localhost:8083}") String urlProdCliente,
            @Value("${URL_PROD_IA:http://localhost:8086}") String urlProdIA) {
        this.redisTemplate = redisTemplate;
        this.urlProdCliente = urlProdCliente;
        this.urlProdIA = urlProdIA;

        if (urlProdCliente.startsWith("lb://") || urlProdCliente.contains("microservicio-cliente")) {
            this.clientWebClient = webClientBuilder.build();
        } else {
            this.clientWebClient = WebClient.create();
        }

        if (urlProdIA.startsWith("lb://") || urlProdIA.contains("microservicio-ia")) {
            this.iaWebClient = webClientBuilder.build();
        } else {
            this.iaWebClient = WebClient.create();
        }
    }

    /**
     * Endpoint BFF unificado para obtener perfil, KPIs y transacciones recientes.
     */
    @GetMapping("/resumen")
    public Mono<ResponseEntity<ResultadoApi<?>>> getResumen(
            @RequestHeader(value = "X-Usuario-Id") String usuarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {

        String keyPerfil = "dashboard:perfil:" + usuarioId;
        String keyResumen = "dashboard:resumen:" + usuarioId;

        log.info("[BFF-DASHBOARD] Petición unificada /resumen recibida para usuarioId={}", usuarioId);

        // 1. Obtener perfil de usuario (Caché 24h o ms-cliente)
        Mono<JsonNode> perfilMono = redisTemplate.opsForValue().get(keyPerfil)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para perfil de usuarioId={}", usuarioId))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[BFF-DASHBOARD] Cache MISS para perfil de usuarioId={}. Consultando ms-cliente...", usuarioId);
                    return fetchPerfilFromService(usuarioId, authHeader)
                            .flatMap(node -> {
                                if (node != null && !node.isNull() && node.size() > 0) {
                                    return redisTemplate.opsForValue()
                                            .set(keyPerfil, node.toString(), Duration.ofHours(24))
                                            .thenReturn(node);
                                }
                                return Mono.just(node);
                            });
                }));

        // 2. Obtener resumen KPIs y recientes (Caché 15m o ms-ia que escribe en caché)
        Mono<JsonNode> kpiMono = redisTemplate.opsForValue().get(keyResumen)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para KPIs de usuarioId={}", usuarioId))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[BFF-DASHBOARD] Cache MISS para KPIs de usuarioId={}. Consultando ms-ia...", usuarioId);
                    return fetchKPIsFromService(usuarioId, authHeader);
                }));

        // 3. Unificar reactivamente con Mono.zip
        return Mono.zip(perfilMono, kpiMono)
                .map(tuple -> {
                    JsonNode perfilNode = tuple.getT1();
                    JsonNode kpiNode = tuple.getT2();

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("perfil", perfilNode);
                    
                    if (kpiNode != null) {
                        if (kpiNode.has("resumen")) {
                            responseData.put("resumen", kpiNode.get("resumen"));
                        } else {
                            responseData.put("resumen", objectMapper.createObjectNode());
                        }
                        if (kpiNode.has("recientes")) {
                            responseData.put("recientes", kpiNode.get("recientes"));
                        } else {
                            responseData.put("recientes", objectMapper.createArrayNode());
                        }
                    } else {
                        responseData.put("resumen", objectMapper.createObjectNode());
                        responseData.put("recientes", objectMapper.createArrayNode());
                    }

                    ResultadoApi<?> body = new ResultadoApi<>(
                            true,
                            200,
                            null,
                            "Resumen del dashboard recuperado con éxito.",
                            responseData,
                            null,
                            null,
                            "/api/v1/dashboard/resumen",
                            LocalDateTime.now()
                    );
                    ResponseEntity<ResultadoApi<?>> responseEntity = ResponseEntity.ok(body);
                    return responseEntity;
                })
                .onErrorResume(error -> {
                    log.error("[BFF-DASHBOARD] Error crítico al construir resumen para usuarioId={}: {}", 
                            usuarioId, error.getMessage(), error);
                    ResultadoApi<?> errBody = ResultadoApi.falla(
                            CodigoError.ERROR_INTERNO,
                            "Error al procesar la consolidación del dashboard: " + error.getMessage(),
                            "/api/v1/dashboard/resumen"
                    );
                    ResponseEntity<ResultadoApi<?>> errResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errBody);
                    return Mono.just(errResponse);
                });
    }

    /**
     * Endpoint BFF para obtener los datos de gráficos SVG (flujo de caja e ingresos/egresos).
     */
    @GetMapping("/graficos")
    public Mono<ResponseEntity<ResultadoApi<?>>> getGraficos(
            @RequestHeader(value = "X-Usuario-Id") String usuarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {

        String keyGraficos = "dashboard:graficos:" + usuarioId;

        log.info("[BFF-DASHBOARD] Petición /graficos recibida para usuarioId={}", usuarioId);

        return redisTemplate.opsForValue().get(keyGraficos)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para gráficos de usuarioId={}", usuarioId))
                .map(node -> {
                    ResultadoApi<?> body = new ResultadoApi<>(
                            true,
                            200,
                            null,
                            "Datos de gráficos recuperados con éxito (Caché Redis).",
                            node,
                            null,
                            null,
                            "/api/v1/dashboard/graficos",
                            LocalDateTime.now()
                    );
                    ResponseEntity<ResultadoApi<?>> responseEntity = ResponseEntity.ok(body);
                    return responseEntity;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[BFF-DASHBOARD] Cache MISS para gráficos de usuarioId={}. Consultando ms-ia...", usuarioId);
                    return fetchGraficosFromService(usuarioId, authHeader)
                            .map(node -> {
                                ResultadoApi<?> body = new ResultadoApi<>(
                                        true,
                                        200,
                                        null,
                                        "Datos de gráficos recuperados con éxito (Servicio IA).",
                                        node,
                                        null,
                                        null,
                                        "/api/v1/dashboard/graficos",
                                        LocalDateTime.now()
                                );
                                ResponseEntity<ResultadoApi<?>> responseEntity = ResponseEntity.ok(body);
                                return responseEntity;
                            });
                }))
                .onErrorResume(error -> {
                    log.error("[BFF-DASHBOARD] Error al obtener gráficos para usuarioId={}: {}", usuarioId, error.getMessage());
                    ResultadoApi<?> errBody = ResultadoApi.falla(
                            CodigoError.ERROR_INTERNO,
                            "Error al recuperar los datos de gráficos: " + error.getMessage(),
                            "/api/v1/dashboard/graficos"
                    );
                    ResponseEntity<ResultadoApi<?>> errResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errBody);
                    return Mono.just(errResponse);
                });
    }

    private Mono<JsonNode> fetchPerfilFromService(String usuarioId, String authHeader) {
        String url = (urlProdCliente.startsWith("lb://") || urlProdCliente.contains("microservicio-cliente"))
                ? "http://microservicio-cliente/api/v1/clientes/perfil/" + usuarioId
                : urlProdCliente + "/api/v1/clientes/perfil/" + usuarioId;

        return clientWebClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseStr -> {
                    try {
                        JsonNode root = objectMapper.readTree(responseStr);
                        if (root.has("data") && !root.get("data").isNull()) {
                            return root.get("data");
                        }
                        return objectMapper.createObjectNode();
                    } catch (Exception e) {
                        log.error("[BFF-DASHBOARD] Error al deserializar respuesta de perfil para usuarioId={}: {}", usuarioId, e.getMessage());
                        return objectMapper.createObjectNode();
                    }
                })
                .onErrorResume(error -> {
                    log.warn("[BFF-DASHBOARD] No se pudo recuperar perfil de ms-cliente para usuarioId={}: {}", usuarioId, error.getMessage());
                    return Mono.just(objectMapper.createObjectNode());
                });
    }

    private Mono<JsonNode> fetchKPIsFromService(String usuarioId, String authHeader) {
        String url = (urlProdIA.startsWith("lb://") || urlProdIA.contains("microservicio-ia"))
                ? "http://microservicio-ia/api/v1/ia/dashboard/kpis"
                : urlProdIA + "/api/v1/ia/dashboard/kpis";

        return iaWebClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseStr -> {
                    try {
                        JsonNode root = objectMapper.readTree(responseStr);
                        if (root.has("data") && !root.get("data").isNull()) {
                            return root.get("data");
                        }
                        return objectMapper.createObjectNode();
                    } catch (Exception e) {
                        log.error("[BFF-DASHBOARD] Error al deserializar respuesta de KPIs para usuarioId={}: {}", usuarioId, e.getMessage());
                        return objectMapper.createObjectNode();
                    }
                })
                .onErrorResume(error -> {
                    log.warn("[BFF-DASHBOARD] No se pudo recuperar KPIs de ms-ia para usuarioId={}: {}", usuarioId, error.getMessage());
                    return Mono.just(objectMapper.createObjectNode());
                });
    }

    private Mono<JsonNode> fetchGraficosFromService(String usuarioId, String authHeader) {
        String url = (urlProdIA.startsWith("lb://") || urlProdIA.contains("microservicio-ia"))
                ? "http://microservicio-ia/api/v1/ia/dashboard/graficos"
                : urlProdIA + "/api/v1/ia/dashboard/graficos";

        return iaWebClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(String.class)
                .map(responseStr -> {
                    try {
                        JsonNode root = objectMapper.readTree(responseStr);
                        if (root.has("data") && !root.get("data").isNull()) {
                            return root.get("data");
                        }
                        return objectMapper.createObjectNode();
                    } catch (Exception e) {
                        log.error("[BFF-DASHBOARD] Error al deserializar respuesta de gráficos para usuarioId={}: {}", usuarioId, e.getMessage());
                        return objectMapper.createObjectNode();
                    }
                })
                .onErrorResume(error -> {
                    log.warn("[BFF-DASHBOARD] No se pudo recuperar gráficos de ms-ia para usuarioId={}: {}", usuarioId, error.getMessage());
                    return Mono.just(objectMapper.createObjectNode());
                });
    }

    private JsonNode parseJsonSilently(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("[BFF-DASHBOARD] Error parseando JSON de caché: {}", e.getMessage());
            return objectMapper.createObjectNode();
        }
    }
}
