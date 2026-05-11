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
        return crearRespuestaError(CodigoError.RECURSO_NO_ENCONTRADO, ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarDniDuplicado(DniDuplicadoException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.DNI_DUPLICADO, ex.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(LimiteGastoException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarExcesoLimite(LimiteGastoException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.ERROR_VALIDACION, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @SuppressWarnings("null")
    private ResponseEntity<ResultadoApi<Void>> crearRespuestaError(CodigoError cod, String msg, HttpStatus status, WebRequest req) {
        String path = req.getDescription(false).replace("uri=", "");
        return ResponseEntity.status(status).body(ResultadoApi.falla(cod, msg, path));
    }
}
