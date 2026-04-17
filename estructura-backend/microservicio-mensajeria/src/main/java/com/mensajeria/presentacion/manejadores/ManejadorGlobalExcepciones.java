package com.mensajeria.presentacion.manejadores;

import com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones del Microservicio-Mensajería. Garantiza
 * respuestas de error estructuradas y consistentes.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    // ─── IP / Usuario bloqueado ───────────────────────────────────────────────
    @ExceptionHandler(UsuarioBloqueadoExcepcion.class)
    public ResponseEntity<Map<String, Object>> manejarUsuarioBloqueado(
            UsuarioBloqueadoExcepcion ex, WebRequest request) {

        log.warn("Usuario bloqueado — usuarioId: {}, horasRestantes: {}",
                ex.getUsuarioId(), ex.getHorasRestantes());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(cuerpoError(429, "USUARIO_BLOQUEADO",
                        ex.getMessage(), extraerRuta(request)));
    }

    // ─── Validaciones @Valid ──────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarErroresValidacion(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return String.format("'%s': %s", fe.getField(), fe.getDefaultMessage());
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        log.debug("Errores de validación: {}", detalles);

        Map<String, Object> cuerpo = cuerpoError(400, "ERROR_VALIDACION",
                "Error en los datos enviados.", extraerRuta(request));
        cuerpo.put("detalles", detalles);

        return ResponseEntity.badRequest().body(cuerpo);
    }

    // ─── Errores de negocio ───────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> manejarArgumentoInvalido(
            IllegalArgumentException ex, WebRequest request) {

        return ResponseEntity.badRequest()
                .body(cuerpoError(400, "SOLICITUD_INCORRECTA",
                        ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> manejarEstadoInvalido(
            IllegalStateException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpoError(409, "CONFLICTO",
                        ex.getMessage(), extraerRuta(request)));
    }

    // ─── Catch-all ────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorGeneral(
            Exception ex, WebRequest request) {

        log.error("Error inesperado: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(cuerpoError(500, "ERROR_INTERNO",
                        "Ha ocurrido un error interno. Intente nuevamente más tarde.",
                        extraerRuta(request)));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private Map<String, Object> cuerpoError(int estado, String error,
            String mensaje, String ruta) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("estado", estado);
        body.put("error", error);
        body.put("mensaje", mensaje);
        body.put("ruta", ruta);
        body.put("fechaHora", LocalDateTime.now().toString());
        return body;
    }

    private String extraerRuta(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
