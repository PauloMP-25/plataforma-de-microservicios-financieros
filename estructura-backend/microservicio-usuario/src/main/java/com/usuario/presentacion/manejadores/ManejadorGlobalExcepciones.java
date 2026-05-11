package com.usuario.presentacion.manejadores;

import com.libreria.comun.manejadores.ManejadorGlobalExcepcionesBase;
import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.excepciones.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Manejador global de excepciones para el microservicio de usuario.
 * Extiende de la base de la librería común para asegurar consistencia en las
 * respuestas de error.
 */
@RestControllerAdvice
@Slf4j
public class ManejadorGlobalExcepciones extends ManejadorGlobalExcepcionesBase {

    /**
     * Captura errores de credenciales inválidas (Login fallido).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarCredencialesInvalidas(BadCredentialsException ex,
            WebRequest request) {
        log.warn("Intento de login con credenciales inválidas: {}", ex.getMessage());
        return crearRespuestaError(
                CodigoError.CREDENCIALES_INVALIDAS,
                "El correo o la contraseña son incorrectos.",
                HttpStatus.UNAUTHORIZED,
                request);
    }

    /**
             * Captura errores de cuentas bloqueadas.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarCuentaBloqueada(LockedException ex, WebRequest request) {
        log.warn("Intento de acceso a cuenta bloqueada: {}", ex.getMessage());
        return crearRespuestaError(
                CodigoError.CUENTA_BLOQUEADA,
                ex.getMessage(),
                HttpStatus.LOCKED,
                request);
    }

    /**
     * Captura errores de cuentas no habilitadas (pendiente de activación).
     */
    @ExceptionHandler(CuentaNoHabilitadaException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarCuentaNoHabilitada(CuentaNoHabilitadaException ex,
            WebRequest request) {
        return crearRespuestaError(
                CodigoError.CUENTA_NO_ACTIVADA,
                "Su cuenta aún no ha sido activada. Por favor, verifique su correo.",
                HttpStatus.FORBIDDEN,
                request);
    }

    /**
     * Captura errores de tokens o códigos OTP inválidos.
     */
    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ResultadoApi<Void>> manejarTokenInvalido(TokenInvalidoException ex, WebRequest request) {
        return crearRespuestaError(
                CodigoError.TOKEN_INVALIDO,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request);
    }

    /**
     * Captura errores de validación de negocio genéricos.
     */
    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ResultadoApi<Void>> manejarErroresNegocio(RuntimeException ex, WebRequest request) {
        log.error("Error de lógica de negocio: {}", ex.getMessage());
        return crearRespuestaError(
                CodigoError.ERROR_VALIDACION,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request);
    }

    /**
     * Método auxiliar para construir respuestas estandarizadas usando ResultadoApi.
     */
    @SuppressWarnings("null")
    private ResponseEntity<ResultadoApi<Void>> crearRespuestaError(
            CodigoError codigo, String mensaje, HttpStatus status, WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        ResultadoApi<Void> resultado = ResultadoApi.falla(codigo, mensaje, path);
        return new ResponseEntity<>(resultado, status);
    }
}
