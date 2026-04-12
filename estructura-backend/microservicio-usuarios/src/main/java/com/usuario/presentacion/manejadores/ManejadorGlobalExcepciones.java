package com.usuario.presentacion.manejadores;

import com.usuario.aplicacion.dtos.ErrorApi;
import com.usuario.aplicacion.excepciones.ExcepcionIpBloqueada;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    // =========================================================================
    // Excepciones de dominio
    // =========================================================================

    @ExceptionHandler(ExcepcionIpBloqueada.class)
    public ResponseEntity<ErrorApi> manejarIpBloqueada(
            ExcepcionIpBloqueada ex, WebRequest request) {

        log.warn("IP bloqueada: {} — minutos restantes: {}",
                ex.getIpAddress(), ex.getMinutesUntilUnblock());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorApi.of(
                        429,
                        "IP_BLOQUEADA",
                        ex.getMessage(),
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Validaciones (@Valid)
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarErroresValidacion(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fe) {
                        return String.format("'%s': %s",
                                fe.getField(), fe.getDefaultMessage());
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.toList());

        log.debug("Errores de validación: {}", detalles);

        return ResponseEntity.badRequest()
                .body(ErrorApi.of(
                        400,
                        "ERROR_VALIDACION",
                        "Error en los datos enviados.",
                        extraerRuta(request),
                        detalles
                ));
    }

    // =========================================================================
    // Autorización
    // =========================================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Acceso denegado: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.of(
                        403,
                        "PROHIBIDO",
                        "No tiene permisos para acceder a este recurso.",
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Errores de negocio
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> manejarArgumentoInvalido(
            IllegalArgumentException ex, WebRequest request) {

        return ResponseEntity.badRequest()
                .body(ErrorApi.of(
                        400,
                        "SOLICITUD_INCORRECTA",
                        ex.getMessage(),
                        extraerRuta(request)
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorApi> manejarEstadoInvalido(
            IllegalStateException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(
                        409,
                        "CONFLICTO",
                        ex.getMessage(),
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Catch-all
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarErrorGeneral(
            Exception ex, WebRequest request) {

        log.error("Error inesperado: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApi.of(
                        500,
                        "ERROR_INTERNO",
                        "Ha ocurrido un error interno. Intente nuevamente más tarde.",
                        extraerRuta(request)
                ));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String extraerRuta(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
