package com.libreria.comun.manejadores;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;
import com.libreria.comun.respuesta.ResultadoApi;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

/**
 * Clase base para la gestión de excepciones en el ecosistema LUKA APP.
 * <p>
 * Proporciona implementaciones estándar para errores comunes (Feign, Validaciones, Errores Globales).
 * Los microservicios DEBEN extender de esta clase y anotar su implementación local con 
 * {@code @RestControllerAdvice}.
 * </p>
 *
 * @author Paulo Moron
 */
@Slf4j
public abstract class ManejadorGlobalExcepcionesBase {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Maneja excepciones de comunicación entre microservicios (Feign).
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ResultadoApi<?>> manejarFeignException(FeignException ex, HttpServletRequest req) {
        String mensajeFinal = "Error en comunicación con servicio externo.";

        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                JsonNode root = objectMapper.readTree(ex.contentUTF8());
                if (root.has("mensaje")) {
                    mensajeFinal = root.get("mensaje").asText();
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("No se pudo parsear el error de Feign en {}: {}", req.getRequestURI(), e.getMessage());
        }

        log.error("Error Feign en {}: status={}, body={}", req.getRequestURI(), ex.status(), ex.contentUTF8());

        int status = ex.status() > 0 ? ex.status() : 500;
        return ResponseEntity.status(status)
                .body(ResultadoApi.falla(status, "ERROR_MICROSERVICIO_EXTERNO", mensajeFinal, req.getRequestURI()));
    }

    /**
     * Captura toda la jerarquía de excepciones propias de LUKA APP.
     */
    @SuppressWarnings("null")
    @ExceptionHandler(ExcepcionGlobal.class)
    public ResponseEntity<ResultadoApi<?>> manejarExcepcionLuka(ExcepcionGlobal ex, HttpServletRequest req) {
        CodigoError cod = ex.getError();
        HttpStatus status = cod.getStatus();

        log.warn("Excepción controlada [{}]: {}", cod.name(), ex.getMensaje());

        @SuppressWarnings("unchecked")
        List<String> detalles = (ex.getDetalles() instanceof List) ? (List<String>) ex.getDetalles() : null;

        return ResponseEntity.status(status)
                .body(ResultadoApi.fallaConDetalles(
                        cod,
                        ex.getMensaje(),
                        req.getRequestURI(),
                        detalles));
    }

    /**
     * Traduce los errores de validación de Bean Validation (@Valid) a mensajes amigables.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultadoApi<?>> manejarValidacion(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> String.format("Campo '%s': %s", e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ResultadoApi.fallaConDetalles(CodigoError.ERROR_VALIDACION, "Datos de entrada inválidos.",
                        req.getRequestURI(), errores));
    }

    /**
     * Captura cualquier excepción no controlada.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultadoApi<?>> manejarErrorInesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}: {}", req.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultadoApi.falla(CodigoError.ERROR_INTERNO, "Ocurrió un fallo inesperado en el servidor.",
                        req.getRequestURI()));
    }
}
