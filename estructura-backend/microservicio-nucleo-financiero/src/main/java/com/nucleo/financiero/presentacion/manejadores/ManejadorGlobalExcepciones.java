package com.nucleo.financiero.presentacion.manejadores;

import com.nucleo.financiero.aplicacion.dtos.ErrorApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(
            MethodArgumentNotValidException ex, WebRequest request) {
        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe
                        ? String.format("'%s': %s", fe.getField(), fe.getDefaultMessage())
                        : e.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "ERROR_VALIDACION", "Error en los datos enviados.",
                        extraerRuta(request), detalles));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> manejarArgumento(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "SOLICITUD_INCORRECTA", ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorApi> manejarEstado(IllegalStateException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(409, "CONFLICTO", ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> manejarAcceso(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.of(403, "PROHIBIDO",
                        "No tiene permisos para acceder a este recurso.", extraerRuta(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarGeneral(Exception ex, WebRequest request) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApi.of(500, "ERROR_INTERNO",
                        "Ha ocurrido un error interno. Intente nuevamente.", extraerRuta(request)));
    }

    private String extraerRuta(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
