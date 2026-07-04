package com.financiero.saas.gateway.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para verificar la salud interna de los microservicios en el backend.
 * Oculta los puertos y endpoints internos exponiendo una ruta unificada en el Gateway.
 */
@RestController
@RequestMapping("/api/v1/admin/monitoreo")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"}, allowedHeaders = "*", allowCredentials = "true")
@Slf4j
public class ControladorMonitoreoSalud {

    @Value("${URL_PROD_USUARIO:http://localhost:8081}")
    private String urlProdUsuario;

    @Value("${URL_PROD_AUDITORIA:http://localhost:8082}")
    private String urlProdAuditoria;

    @Value("${URL_PROD_CLIENTE:http://localhost:8083}")
    private String urlProdCliente;

    @Value("${URL_PROD_MENSAJERIA:http://localhost:8084}")
    private String urlProdMensajeria;

    @Value("${URL_PROD_FINANCIERO:http://localhost:8085}")
    private String urlProdFinanciero;

    @Value("${URL_PROD_IA:http://localhost:8086}")
    private String urlProdIA;

    @Value("${URL_PROD_PAGO:http://localhost:8087}")
    private String urlProdPagos;

    @Value("${URL_PROD_SUSCRIPCIONES:http://localhost:8088}")
    private String urlProdSuscripciones;

    private final WebClient loadBalancedWebClient;

    public ControladorMonitoreoSalud(WebClient.Builder webClientBuilder) {
        this.loadBalancedWebClient = webClientBuilder.build();
    }

    @GetMapping("/salud/{servicio}")
    public Mono<ResponseEntity<ResultadoApi<Map<String, Object>>>> verificarSaludServicio(
            @PathVariable String servicio,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        String urlInterna = obtenerUrlInternaServicio(servicio);
        if (urlInterna == null) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", "DOWN");
            errorBody.put("error", "Servicio no reconocido");

            ResultadoApi<Map<String, Object>> errRes = new ResultadoApi<>(
                    false,
                    400,
                    null,
                    "Servicio no reconocido por el Gateway.",
                    errorBody,
                    null,
                    null,
                    "/api/v1/admin/monitoreo/salud/" + servicio,
                    LocalDateTime.now()
            );
            return Mono.just(ResponseEntity.badRequest().body(errRes));
        }

        log.info("[MONITOREO-SALUD] Verificando salud de servicio={} en url={}", servicio, urlInterna);

        WebClient selectedWebClient;
        if (urlInterna.startsWith("lb://") || urlInterna.contains("microservicio-")) {
            selectedWebClient = this.loadBalancedWebClient;
        } else {
            selectedWebClient = WebClient.create();
        }

        return selectedWebClient.get()
                .uri(urlInterna + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> {
                    Map<String, Object> body = new HashMap<>();
                    String statusVal = "DOWN";
                    if (res != null) {
                        statusVal = res.get("status") != null ? res.get("status").toString() : 
                                    (res.get("estado") != null ? res.get("estado").toString() : "UP");
                    }
                    body.put("status", statusVal);
                    body.put("estado", statusVal);
                    body.put("detalles", res);

                    ResultadoApi<Map<String, Object>> successRes = new ResultadoApi<>(
                            true,
                            200,
                            null,
                            "Estado del servicio recuperado con éxito.",
                            body,
                            null,
                            null,
                            "/api/v1/admin/monitoreo/salud/" + servicio,
                            LocalDateTime.now()
                    );
                    return ResponseEntity.ok(successRes);
                })
                .onErrorResume(err -> {
                    log.error("[MONITOREO-SALUD] Error al verificar salud de {}: {}", servicio, err.getMessage());
                    Map<String, Object> errorBody = new HashMap<>();
                    errorBody.put("status", "DOWN");
                    errorBody.put("estado", "DOWN");
                    errorBody.put("error", err.getMessage());

                    ResultadoApi<Map<String, Object>> errRes = new ResultadoApi<>(
                            true,
                            200,
                            null,
                            "No se pudo conectar con el microservicio.",
                            errorBody,
                            null,
                            null,
                            "/api/v1/admin/monitoreo/salud/" + servicio,
                            LocalDateTime.now()
                    );
                    return Mono.just(ResponseEntity.ok(errRes));
                });
    }

    private String obtenerUrlInternaServicio(String servicio) {
        switch (servicio) {
            case "API Gateway":
                return "http://localhost:8080";
            case "ms-usuario":
                return urlProdUsuario;
            case "ms-auditoria":
                return urlProdAuditoria;
            case "ms-cliente":
                return urlProdCliente;
            case "ms-mensajeria":
                return urlProdMensajeria;
            case "ms-nucleo-financiero":
                return urlProdFinanciero;
            case "ms-ia":
                return urlProdIA;
            case "ms-pagos":
                return urlProdPagos;
            case "ms-suscripciones":
                return urlProdSuscripciones;
            default:
                return null;
        }
    }
}
