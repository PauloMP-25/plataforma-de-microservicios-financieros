package com.usuario.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
// =========================================================================
// TokenInvalidoException
// =========================================================================
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String razon) {
        super("Token inválido: " + razon);
    }
}
