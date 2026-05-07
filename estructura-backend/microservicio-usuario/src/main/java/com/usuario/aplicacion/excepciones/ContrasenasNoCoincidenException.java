package com.usuario.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
// =========================================================================
// ContrasenasNoCoincidenException
// =========================================================================
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ContrasenasNoCoincidenException extends RuntimeException {
    public ContrasenasNoCoincidenException() {
        super("Las contraseñas no coinciden.");
    }
}
