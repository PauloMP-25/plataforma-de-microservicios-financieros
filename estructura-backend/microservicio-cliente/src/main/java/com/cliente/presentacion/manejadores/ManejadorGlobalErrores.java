package com.cliente.presentacion.manejadores;

import com.cliente.aplicacion.dtos.ErrorApi;
import com.cliente.aplicacion.excepciones.*;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ManejadorGlobalErrores {

    // 1. Errores de Negocio Específicos
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(AccesoDenegadoException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorApi.of(403, "ACCESO_DENEGADO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<ErrorApi> manejarNoEncontrado(ClienteNoEncontradoException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorApi.of(404, "CLIENTE_NO_ENCONTRADO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DniDuplicadoException.class)
    public ResponseEntity<ErrorApi> manejarDniDuplicado(DniDuplicadoException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorApi.of(409, "DNI_DUPLICADO", ex.getMessage(), request.getRequestURI()));
    }

    // 2. Errores de Validación (Bean Validation @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidaciones(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorApi.of(400, "DATOS_INVALIDOS", errores, request.getRequestURI()));
    }

    // 3. Errores de Seguridad JWT
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorApi> manejarErrorFirma(SignatureException ex, HttpServletRequest request) {
        log.error("Error de firma JWT: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApi.of(401, "JWT_FIRMA_INVALIDA", "Token alterado o clave inválida.", request.getRequestURI()));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorApi> manejarTokenExpirado(ExpiredJwtException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApi.of(401, "JWT_EXPIRADO", "El token ha caducado.", request.getRequestURI()));
    }

    // 4. Error Genérico (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApi> manejarErrorGeneral(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: ", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorApi.of(500, "ERROR_INTERNO", "Ocurrió un error inesperado en el servidor.", request.getRequestURI()));
    }
}
