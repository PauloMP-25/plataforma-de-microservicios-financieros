package com.nucleo.financiero.presentacion.manejadores;

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
 * Manejador global de excepciones para el microservicio de núcleo financiero.
 * Extiende de la base de la librería común y permite gestionar errores transaccionales.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    /**
     * Captura errores de validación de negocio específicos del dominio financiero.
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ResultadoApi<Void>> manejarErroresNegocio(RuntimeException ex, WebRequest request) {
        log.error("Error de lógica financiera: {}", ex.getMessage());
        
        String path = request.getDescription(false).replace("uri=", "");
        ResultadoApi<Void> resultado = ResultadoApi.falla(
                CodigoError.ERROR_VALIDACION,
                ex.getMessage(),
                path
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
    }
}
