package com.auditoria.presentacion.manejadores;

import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.auditoria.aplicacion.excepciones.IpBloqueadaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones para el microservicio de auditoría.
 * Extiende de la base de la librería común y especializa errores de bloqueo de IP.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    @ExceptionHandler(IpBloqueadaException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarIpBloqueada(IpBloqueadaException ex, WebRequest request) {
        log.warn("Acceso denegado: IP Bloqueada - {}", ex.getMessage());
        
        String path = request.getDescription(false).replace("uri=", "");
        ResultadoApi<Void> resultado = ResultadoApi.falla(
                CodigoError.CUENTA_BLOQUEADA, // Usamos un código similar o uno específico si existe
                ex.getMessage(),
                path
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resultado);
    }
}
