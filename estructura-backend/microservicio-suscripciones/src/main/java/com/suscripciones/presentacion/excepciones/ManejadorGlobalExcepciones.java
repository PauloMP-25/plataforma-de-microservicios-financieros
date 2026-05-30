package com.suscripciones.presentacion.excepciones;

import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones para el microservicio de suscripciones.
 * Extiende la funcionalidad base de la librería común y asegura que todas
 * las excepciones devuelvan una respuesta en el formato unificado {@link ResultadoApi}.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    /**
     * Captura errores de validación de negocio específicos (ej. estrategias inválidas o inconsistencia de fechas).
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ResultadoApi<Void>> manejarErroresNegocioSuscripciones(RuntimeException ex, WebRequest request) {
        log.error("Error de lógica de negocio en suscripciones: {}", ex.getMessage());
        
        String path = request.getDescription(false).replace("uri=", "");
        ResultadoApi<Void> resultado = ResultadoApi.falla(
                CodigoError.ERROR_VALIDACION,
                ex.getMessage(),
                path
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
    }
}
