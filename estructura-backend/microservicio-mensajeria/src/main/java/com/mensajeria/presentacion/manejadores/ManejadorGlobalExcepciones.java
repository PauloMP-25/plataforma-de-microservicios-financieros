package com.mensajeria.presentacion.manejadores;

import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.mensajeria.aplicacion.excepciones.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones para el microservicio de mensajería.
 * Extiende de la base de la librería común y especializa errores de OTP y límites de envío.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    @ExceptionHandler({
        CodigoExpiradoException.class,
        CodigoInvalidoException.class,
        CodigoPendienteNotFoundException.class
    })
    public ResponseEntity<ResultadoApi<Void>> manejarErroresCodigo(RuntimeException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.TOKEN_INVALIDO, ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({
        LimiteCodigosExcedidoException.class,
        LimiteIntentosExcedidoException.class
    })
    public ResponseEntity<ResultadoApi<Void>> manejarLimitesExcedidos(RuntimeException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.LIMITE_DIARIO_EXCEDIDO, ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS, request);
    }

    @ExceptionHandler(UsuarioBloqueadoExcepcion.class)
    public ResponseEntity<ResultadoApi<Void>> manejarUsuarioBloqueado(UsuarioBloqueadoExcepcion ex, WebRequest request) {
        return crearRespuestaError(CodigoError.CUENTA_BLOQUEADA, ex.getMessage(), HttpStatus.LOCKED, request);
    }

    @ExceptionHandler(MensajeriaExternaException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarErrorExterno(MensajeriaExternaException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.ERROR_INTERNO, "Error en el proveedor de mensajería: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @SuppressWarnings("null")
    private ResponseEntity<ResultadoApi<Void>> crearRespuestaError(CodigoError cod, String msg, HttpStatus status, WebRequest req) {
        String path = req.getDescription(false).replace("uri=", "");
        return ResponseEntity.status(status).body(ResultadoApi.falla(cod, msg, path));
    }
}
