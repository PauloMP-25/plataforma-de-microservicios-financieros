package com.libreria.comun.manejadores;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;
import com.libreria.comun.excepciones.ExcepcionNoAutorizado;
import com.libreria.comun.respuesta.ResultadoApi;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Interceptor centralizado para la gestión de excepciones en LUKA APP.
 * <p>
 * Utiliza {@link RestControllerAdvice} para capturar errores de todos los
 * controladores y transformarlos en un formato {@link ResultadoApi}
 * estandarizado en español.
 * </p>
 *
 * @author Paulo Moron
 */
@Slf4j
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Maneja excepciones de comunicación entre microservicios (Feign).
     * <p>
     * Intenta extraer el mensaje de error original enviado por el microservicio
     * destino.
     * </p>
     *
     * @param ex La excepción capturada.
     * @param req Información de la petición HTTP.
     * @return Respuesta estructurada con el código HTTP correspondiente.
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
     * <p>
     * Utiliza polimorfismo para extraer el código semántico y el mensaje
     * configurado en cada excepción concreta.
     * </p>
     *
     * @param ex La excepción capturada.
     * @param req Información de la petición HTTP.
     * @return Respuesta estructurada con el código HTTP correspondiente.
     */
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
     * Traduce los errores de validación de Bean Validation (@Valid) a mensajes
     * amigables.
     *
     * @param ex Excepción de argumentos no válidos.
     * @param req Información de la petición HTTP.
     * @return Respuesta con estado 400 y lista detallada de errores de campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultadoApi<?>> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> String.format("Campo '%s': %s", e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseEntity.badRequest()
                .body(ResultadoApi.fallaConDetalles(CodigoError.ERROR_VALIDACION, "Datos de entrada inválidos.", req.getRequestURI(), errores));
    }

    /**
     * Captura cualquier excepción no controlada para evitar fugas de
     * información técnica.
     * <p>
     * Loguea el stacktrace completo para depuración interna pero devuelve un
     * mensaje genérico y seguro al cliente.
     * </p>
     *
     * @param ex Excepción general.
     * @param req Información de la petición HTTP.
     * @return Respuesta con estado 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultadoApi<?>> manejarErrorInesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado en {}: {}", req.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultadoApi.falla(CodigoError.ERROR_INTERNO, "Ocurrió un fallo inesperado en el servidor.", req.getRequestURI()));
    }
}
