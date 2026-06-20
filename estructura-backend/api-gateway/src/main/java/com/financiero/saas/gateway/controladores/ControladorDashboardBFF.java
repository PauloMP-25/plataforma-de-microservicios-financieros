package com.financiero.saas.gateway.controladores;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.financiero.saas.gateway.seguridad.ServicioJwtGateway;
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
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"}, allowedHeaders = "*", allowCredentials = "true")
@Slf4j
public class ControladorDashboardBFF {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient clientWebClient;
    private final WebClient iaWebClient;
    private final ServicioJwtGateway servicioJwt;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${URL_PROD_CLIENTE:http://localhost:8083}")
    private String urlProdCliente;

    @Value("${URL_PROD_IA:http://localhost:8086}")
    private String urlProdIA;

    public ControladorDashboardBFF(
            WebClient.Builder webClientBuilder,
            ReactiveRedisTemplate<String, String> redisTemplate,
            ServicioJwtGateway servicioJwt,
            @Value("${URL_PROD_CLIENTE:http://localhost:8083}") String urlProdCliente,
            @Value("${URL_PROD_IA:http://localhost:8086}") String urlProdIA) {
        this.redisTemplate = redisTemplate;
        this.servicioJwt = servicioJwt;
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
            @RequestParam(required = false, defaultValue = "false") boolean refresh,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {

        String resolvedUsuarioId = usuarioId;
        if (resolvedUsuarioId == null || resolvedUsuarioId.isEmpty()) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    resolvedUsuarioId = servicioJwt.extraerUsuarioId(token);
                } catch (Exception e) {
                    log.error("[BFF-DASHBOARD] Error al extraer usuarioId del token JWT: {}", e.getMessage());
                }
            }
        }

        if (resolvedUsuarioId == null || resolvedUsuarioId.isEmpty()) {
            ResultadoApi<?> errBody = ResultadoApi.falla(
                    CodigoError.ACCESO_NO_AUTORIZADO,
                    "Identidad de usuario no provista o token inválido.",
                    "/api/v1/dashboard/resumen"
            );
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errBody));
        }

        final String finalUsuarioId = resolvedUsuarioId;
        String keyPerfil = "dashboard:perfil:" + finalUsuarioId;
        String keyResumen = "dashboard:resumen:" + finalUsuarioId;

        log.info("[BFF-DASHBOARD] Petición unificada /resumen recibida para usuarioId={}, refresh={}", finalUsuarioId, refresh);

        Mono<Void> deleteCacheMono = refresh ? 
            redisTemplate.delete(keyPerfil, keyResumen).then() : 
            Mono.empty();

        // 1. Obtener perfil de usuario (Caché 24h o ms-cliente)
        Mono<JsonNode> perfilMono = deleteCacheMono.then(Mono.defer(() -> 
            redisTemplate.opsForValue().get(keyPerfil)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para perfil de usuarioId={}", finalUsuarioId))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[BFF-DASHBOARD] Cache MISS para perfil de usuarioId={}. Consultando ms-cliente...", finalUsuarioId);
                    return fetchPerfilFromService(finalUsuarioId, authHeader)
                            .flatMap(node -> {
                                if (node != null && !node.isNull() && node.size() > 0) {
                                    return redisTemplate.opsForValue()
                                            .set(keyPerfil, node.toString(), Duration.ofHours(24))
                                            .thenReturn(node);
                                }
                                return Mono.just(node);
                            });
                }))));

        // 2. Obtener resumen KPIs y recientes (Caché 15m o ms-ia que escribe en caché)
        Mono<JsonNode> kpiMono = deleteCacheMono.then(Mono.defer(() ->
            redisTemplate.opsForValue().get(keyResumen)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para KPIs de usuarioId={}", finalUsuarioId))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[BFF-DASHBOARD] Cache MISS para KPIs de usuarioId={}. Consultando ms-ia...", finalUsuarioId);
                    return fetchKPIsFromService(finalUsuarioId, authHeader);
                }))));

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
                            finalUsuarioId, error.getMessage(), error);
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
            @RequestParam(required = false, defaultValue = "false") boolean refresh,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {

        String resolvedUsuarioId = usuarioId;
        if (resolvedUsuarioId == null || resolvedUsuarioId.isEmpty()) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    resolvedUsuarioId = servicioJwt.extraerUsuarioId(token);
                } catch (Exception e) {
                    log.error("[BFF-DASHBOARD] Error al extraer usuarioId del token JWT en /graficos: {}", e.getMessage());
                }
            }
        }

        if (resolvedUsuarioId == null || resolvedUsuarioId.isEmpty()) {
            ResultadoApi<?> errBody = ResultadoApi.falla(
                    CodigoError.ACCESO_NO_AUTORIZADO,
                    "Identidad de usuario no provista o token inválido.",
                    "/api/v1/dashboard/graficos"
            );
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errBody));
        }

        final String finalUsuarioId = resolvedUsuarioId;
        String keyGraficos = "dashboard:graficos:" + finalUsuarioId;

        log.info("[BFF-DASHBOARD] Petición /graficos recibida para usuarioId={}, refresh={}", finalUsuarioId, refresh);

        Mono<Void> deleteCacheMono = refresh ? 
            redisTemplate.delete(keyGraficos).then() : 
            Mono.empty();

        return deleteCacheMono.then(Mono.defer(() -> 
            redisTemplate.opsForValue().get(keyGraficos)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para gráficos de usuarioId={}", finalUsuarioId))
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
                    log.debug("[BFF-DASHBOARD] Cache MISS para gráficos de usuarioId={}. Consultando ms-ia...", finalUsuarioId);
                    return fetchGraficosFromService(finalUsuarioId, authHeader)
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
                })) // Cierra el switchIfEmpty inner defer
        )) // Cierra outer Mono.defer y deleteCacheMono.then
        .onErrorResume(error -> {
            log.error("[BFF-DASHBOARD] Error al obtener gráficos para usuarioId={}: {}", finalUsuarioId, error.getMessage());
                    ResultadoApi<?> errBody = ResultadoApi.falla(
                            CodigoError.ERROR_INTERNO,
                            "Error al recuperar los datos de gráficos: " + error.getMessage(),
                            "/api/v1/dashboard/graficos"
                    );
                    ResponseEntity<ResultadoApi<?>> errResponse = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errBody);
                    return Mono.just(errResponse);
                });
    }

    /**
     * Endpoint BFF para Analítica Avanzada (Dashboard V2).
     * Soporta filtros dinámicos y consolida los 6 nuevos gráficos y KPIs enriquecidos.
     */
    @GetMapping("/analitica-avanzada")
    public Mono<ResponseEntity<ResultadoApi<?>>> getAnaliticaAvanzada(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) String tipoMovimiento,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {

        String resolvedUsuarioId = usuarioId;
        if (resolvedUsuarioId == null || resolvedUsuarioId.isEmpty()) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    resolvedUsuarioId = servicioJwt.extraerUsuarioId(token);
                } catch (Exception e) {
                    log.error("[BFF-DASHBOARD] Error al extraer usuarioId del token JWT en /analitica-avanzada: {}", e.getMessage());
                }
            }
        }

        if (resolvedUsuarioId == null || resolvedUsuarioId.isEmpty()) {
            ResultadoApi<?> errBody = ResultadoApi.falla(
                    CodigoError.ACCESO_NO_AUTORIZADO,
                    "Identidad de usuario no provista o token inválido.",
                    "/api/v1/dashboard/analitica-avanzada"
            );
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errBody));
        }

        final String finalUsuarioId = resolvedUsuarioId;
        
        // Construir clave de caché única basada en los filtros
        String filtrosHash = String.format("%s_%s_%s_%s", 
            fechaInicio != null ? fechaInicio : "all",
            fechaFin != null ? fechaFin : "all",
            metodoPago != null ? metodoPago : "all",
            tipoMovimiento != null ? tipoMovimiento : "all"
        );
        String keyAnalitica = "dashboard:analitica:" + finalUsuarioId + ":" + filtrosHash;

        log.info("[BFF-DASHBOARD] Petición /analitica-avanzada recibida para usuarioId={}, filtros={}", finalUsuarioId, filtrosHash);

        return redisTemplate.opsForValue().get(keyAnalitica)
                .map(this::parseJsonSilently)
                .doOnNext(node -> log.debug("[BFF-DASHBOARD] Cache HIT para analítica avanzada de usuarioId={}", finalUsuarioId))
                .map(node -> {
                    ResultadoApi<?> body = new ResultadoApi<>(
                            true,
                            200,
                            null,
                            "Analítica avanzada recuperada con éxito (Caché Redis).",
                            node,
                            null,
                            null,
                            "/api/v1/dashboard/analitica-avanzada",
                            LocalDateTime.now()
                    );
                    return ResponseEntity.ok(body);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("[BFF-DASHBOARD] Cache MISS para analítica avanzada. Consultando y generando agregación...");
                    
                    // Aquí simularemos temporalmente el llamado a ms-nucleo-financiero / ms-ia
                    // En la fase 2 se conectará con el WebClient real
                    JsonNode dataNode = mockAnaliticaAvanzada();
                    
                    // Guardar en caché por 5 minutos
                    return redisTemplate.opsForValue()
                            .set(keyAnalitica, dataNode.toString(), Duration.ofMinutes(5))
                            .thenReturn(dataNode)
                            .map(node -> {
                                ResultadoApi<?> body = new ResultadoApi<>(
                                        true,
                                        200,
                                        null,
                                        "Analítica avanzada generada con éxito.",
                                        node,
                                        null,
                                        null,
                                        "/api/v1/dashboard/analitica-avanzada",
                                        LocalDateTime.now()
                                );
                                return ResponseEntity.ok(body);
                            });
                }))
                .onErrorResume(error -> {
                    log.error("[BFF-DASHBOARD] Error al obtener analítica avanzada para usuarioId={}: {}", finalUsuarioId, error.getMessage());
                    ResultadoApi<?> errBody = ResultadoApi.falla(
                            CodigoError.ERROR_INTERNO,
                            "Error al recuperar los datos analíticos: " + error.getMessage(),
                            "/api/v1/dashboard/analitica-avanzada"
                    );
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errBody));
                });
    }

    private JsonNode mockAnaliticaAvanzada() {
        try {
            String json = """
            {
              "resumen": {
                "desde": "2026-06-01T00:00:00Z",
                "hasta": "2026-06-30T23:59:59Z",
                "tasaAhorro": 22.5,
                "gastoPromedioDiario": 65.20,
                "cumplimientoPresupuesto": 68.4,
                "proyeccionFinDeMes": 1850.00
              },
              "flujoCaja": [
                { "mes": "Ene", "ingresos": 3000, "gastos": 2500 },
                { "mes": "Feb", "ingresos": 3200, "gastos": 2600 },
                { "mes": "Mar", "ingresos": 3100, "gastos": 2800 },
                { "mes": "Abr", "ingresos": 3500, "gastos": 2400 },
                { "mes": "May", "ingresos": 3400, "gastos": 2700 },
                { "mes": "Jun", "ingresos": 3600, "gastos": 2900 },
                { "mes": "Jul", "ingresos": 3800, "gastos": 3100 },
                { "mes": "Ago", "ingresos": 3700, "gastos": 3000 },
                { "mes": "Sep", "ingresos": 3900, "gastos": 3200 },
                { "mes": "Oct", "ingresos": 4000, "gastos": 3300 },
                { "mes": "Nov", "ingresos": 4200, "gastos": 3400 },
                { "mes": "Dic", "ingresos": 4500, "gastos": 3600 }
              ],
              "distribucionGastos": [
                { "categoria": "Alimentación", "total": 800, "porcentaje": 35, "color": "#f59e0b" },
                { "categoria": "Vivienda", "total": 600, "porcentaje": 25, "color": "#3b82f6" },
                { "categoria": "Transporte", "total": 400, "porcentaje": 15, "color": "#10b981" },
                { "categoria": "Entretenimiento", "total": 300, "porcentaje": 15, "color": "#8b5cf6" },
                { "categoria": "Otros", "total": 200, "porcentaje": 10, "color": "#64748b" }
              ],
              "heatmap": [
                { "dia": "Lunes", "intensidad": 3 },
                { "dia": "Martes", "intensidad": 5 },
                { "dia": "Miércoles", "intensidad": 4 },
                { "dia": "Jueves", "intensidad": 6 },
                { "dia": "Viernes", "intensidad": 8 },
                { "dia": "Sábado", "intensidad": 10 },
                { "dia": "Domingo", "intensidad": 7 }
              ],
              "metas": [
                { "nombre": "Fondo de Emergencia", "objetivo": 10000, "actual": 6500, "porcentaje": 65, "color": "#10b981" }
              ],
              "comparativa": [
                { "mes": "Ene", "actual": 2500, "anterior": 2300 },
                { "mes": "Feb", "actual": 2600, "anterior": 2400 },
                { "mes": "Mar", "actual": 2800, "anterior": 2500 },
                { "mes": "Abr", "actual": 2400, "anterior": 2200 },
                { "mes": "May", "actual": 2700, "anterior": 2600 },
                { "mes": "Jun", "actual": 2900, "anterior": 2700 },
                { "mes": "Jul", "actual": 3100, "anterior": 2900 },
                { "mes": "Ago", "actual": 3000, "anterior": 2800 },
                { "mes": "Sep", "actual": 3200, "anterior": 3000 },
                { "mes": "Oct", "actual": 3300, "anterior": 3100 },
                { "mes": "Nov", "actual": 3400, "anterior": 3200 },
                { "mes": "Dic", "actual": 3600, "anterior": 3300 }
              ],
              "transaccionesMetodo": [
                { "metodo": "Tarjeta", "cantidad": 25, "color": "#3b82f6" },
                { "metodo": "Efectivo", "cantidad": 15, "color": "#10b981" },
                { "metodo": "Transferencia", "cantidad": 18, "color": "#a855f7" },
                { "metodo": "Digital", "cantidad": 12, "color": "#f59e0b" }
              ]
            }
            """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
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
                        if (root.has("datos") && !root.get("datos").isNull()) {
                            return root.get("datos");
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
                        if (root.has("datos") && !root.get("datos").isNull()) {
                            return root.get("datos");
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
                        if (root.has("datos") && !root.get("datos").isNull()) {
                            return root.get("datos");
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
