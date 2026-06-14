package com.cliente.presentacion.manejadores;

import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.cliente.aplicacion.excepciones.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones para el microservicio de cliente.
 * Extiende de la base de la librería común y especializa errores de negocio de perfiles y límites.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    @ExceptionHandler({
        ClienteNoEncontradoException.class, 
        MetaNoEncontradaException.class, 
        LimiteGastoNoEncontradoException.class,
        DatosPersonalesNoEncontradosException.class
    })
    public ResponseEntity<ResultadoApi<Void>> manejarRecursoNoEncontrado(RuntimeException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        return crearRespuestaError(CodigoError.RECURSO_NO_ENCONTRADO, ex.getMessage(), HttpStatus.NOT_FOUND, path);
    }

    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarDniDuplicado(DniDuplicadoException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        return crearRespuestaError(CodigoError.DNI_DUPLICADO, ex.getMessage(), HttpStatus.CONFLICT, path);
    }

    @ExceptionHandler(LimiteGastoException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarExcesoLimite(LimiteGastoException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        return crearRespuestaError(CodigoError.ERROR_VALIDACION, ex.getMessage(), HttpStatus.BAD_REQUEST, path);
    }
}
