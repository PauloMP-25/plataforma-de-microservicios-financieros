package com.mensajeria.aplicacion.excepciones;

// ──────────────────────────────────────────────────────────────

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// CodigoInvalidoException
// ──────────────────────────────────────────────────────────────
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CodigoInvalidoException extends RuntimeException {
    public CodigoInvalidoException(String motivo) {
        super("Código inválido: " + motivo);
    }
}
