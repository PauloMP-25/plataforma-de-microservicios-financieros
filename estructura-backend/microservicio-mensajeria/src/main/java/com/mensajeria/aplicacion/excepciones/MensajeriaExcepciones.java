package com.mensajeria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ──────────────────────────────────────────────────────────────
// CodigoInvalidoException
// ──────────────────────────────────────────────────────────────
@ResponseStatus(HttpStatus.BAD_REQUEST)
class CodigoInvalidoException extends RuntimeException {
    public CodigoInvalidoException(String motivo) {
        super("Código inválido: " + motivo);
    }
}

// ──────────────────────────────────────────────────────────────
// CodigoExpiradoException
// ──────────────────────────────────────────────────────────────
@ResponseStatus(HttpStatus.GONE)
class CodigoExpiradoException extends RuntimeException {
    public CodigoExpiradoException() {
        super("El código OTP ha expirado. Solicite uno nuevo.");
    }
}

// ──────────────────────────────────────────────────────────────
// CodigoPendienteNotFoundException
// ──────────────────────────────────────────────────────────────
@ResponseStatus(HttpStatus.NOT_FOUND)
class CodigoPendienteNotFoundException extends RuntimeException {
    public CodigoPendienteNotFoundException(java.util.UUID usuarioId) {
        super(String.format("No hay un código OTP pendiente para el usuario '%s'.", usuarioId));
    }
}
