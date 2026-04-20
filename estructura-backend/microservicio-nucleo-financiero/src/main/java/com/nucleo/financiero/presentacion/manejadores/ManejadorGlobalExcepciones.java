package com.nucleo.financiero.presentacion.manejadores;

import com.nucleo.financiero.aplicacion.dtos.ErrorApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> String.format("'%s': %s", fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "ERROR_VALIDACION", "Los datos de la transacción son incorrectos.",
                        extraerRuta(request), detalles));
    }

    // Usado para cuando service.obtenerPorId() no encuentra nada
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorApi> manejarNoEncontrado(NoSuchElementException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorApi.of(404, "RECURSO_NO_ENCONTRADO", ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> manejarArgumento(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "SOLICITUD_INCORRECTA", ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorApi> manejarEstado(IllegalStateException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(409, "CONFLICTO_NEGOCIO", ex.getMessage(), extraerRuta(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarGeneral(Exception ex, WebRequest request) {
        log.error("CRITICAL ERROR en Nucleo Financiero: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApi.of(500, "ERROR_INTERNO_SISTEMA", "Contacte al soporte técnico.", extraerRuta(request)));
    }

    private String extraerRuta(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
