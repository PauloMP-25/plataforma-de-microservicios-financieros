package com.cliente.presentacion.manejadores;

import com.cliente.aplicacion.dtos.ErrorApi;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ManejadorGlobalErrores {

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorApi> manejarErrorFirma(SignatureException ex, HttpServletRequest request) {
        log.error("Error de firma JWT: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApi.of(401, "JWT_FIRMA_INVALIDA", "La clave secreta no coincide o el token fue alterado.", request.getRequestURI()));
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorApi> manejarTokenExpirado(ExpiredJwtException ex, HttpServletRequest request) {
        log.error("Token expirado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorApi.of(401, "JWT_EXPIRADO", "El token ha caducado. Inicie sesión nuevamente.", request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorApi> manejarMetodoNoSoportado(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorApi.of(405, "METODO_NO_SOPORTADO", "Estás usando POST pero el endpoint requiere PUT (o viceversa).", request.getRequestURI()));
    }
}