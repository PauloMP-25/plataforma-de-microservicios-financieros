package com.nucleo.financiero.presentacion.manejadores;

import com.nucleo.financiero.aplicacion.dtos.auditoria.ErrorApi;
import feign.FeignException;
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

    /**
     * 1. Errores de Validación: Se dispara cuando los @Valid en los
     *
     * @param ex
     * @param request
     * @return
     * @RequestBody fallan. Captura campos obligatorios, formatos de email,
     * montos negativos, etc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> String.format("'%s': %s", fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "ERROR_VALIDACION", "Los datos de la transacción son incorrectos.",
                        extraerRuta(request), detalles));
    }

    /**
     * 2. Errores de Comunicación Feign: Se dispara cuando falla la llamada a
     * microservicio-ia o microservicio-cliente (ej. error 500 en el destino o
     * timeout).
     *
     * @param ex
     * @param request
     * @return
     */
    
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorApi> manejarErrorComunicacion(FeignException ex, WebRequest request) {
        log.error("Error de comunicación Feign: Status {}, Body: {}", ex.status(), ex.contentUTF8());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorApi.of(503, "ERROR_COMUNICACION",
                        "No se pudo completar la operación con el módulo externo.",
                        extraerRuta(request)));
    }

    /**
     * 3. Recurso No Encontrado: Se dispara cuando service.obtenerPorId() lanza
     * NoSuchElementException.
     *
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorApi> manejarNoEncontrado(NoSuchElementException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorApi.of(404, "RECURSO_NO_ENCONTRADO", ex.getMessage(), extraerRuta(request)));
    }

    /**
     * 4. Solicitud Incorrecta: Captura errores de lógica de negocio o
     * parámetros inválidos enviados por el cliente.
     *
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> manejarArgumento(IllegalArgumentException ex, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorApi.of(400, "SOLICITUD_INCORRECTA", ex.getMessage(), extraerRuta(request)));
    }

    /**
     * 5. Conflicto de Negocio: Para estados ilegales (ej. intentar gastar en
     * una cuenta bloqueada).
     *
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorApi> manejarEstado(IllegalStateException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(409, "CONFLICTO_NEGOCIO", ex.getMessage(), extraerRuta(request)));
    }

    /**
     * 6. Error General: El último recurso para capturar cualquier excepción no
     * controlada y no exponer el stacktrace.
     * @param ex
     * @param request
     * @return 
     */
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
