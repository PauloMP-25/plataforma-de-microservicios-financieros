package com.mensajeria.presentacion.excepciones;

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
        CodigoPendienteNotFoundException.class,
        TelefonoInvalidoException.class
    })
    public ResponseEntity<ResultadoApi<Void>> manejarErroresCodigo(RuntimeException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.TOKEN_INVALIDO, ex.getMessage(), HttpStatus.BAD_REQUEST, getPath(request));
    }

    @ExceptionHandler({
        LimiteCodigosExcedidoException.class,
        LimiteIntentosExcedidoException.class
    })
    public ResponseEntity<ResultadoApi<Void>> manejarLimitesExcedidos(RuntimeException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.LIMITE_DIARIO_EXCEDIDO, ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS, getPath(request));
    }

    @ExceptionHandler(UsuarioBloqueadoExcepcion.class)
    public ResponseEntity<ResultadoApi<Void>> manejarUsuarioBloqueado(UsuarioBloqueadoExcepcion ex, WebRequest request) {
        return crearRespuestaError(CodigoError.CUENTA_BLOQUEADA, ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS, getPath(request));
    }

    @ExceptionHandler(MensajeriaExternaException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarErrorExterno(MensajeriaExternaException ex, WebRequest request) {
        return crearRespuestaError(CodigoError.ERROR_INTERNO, "Error en el proveedor de mensajería: " + ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, getPath(request));
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
