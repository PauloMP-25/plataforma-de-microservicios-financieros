package com.mensajeria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class CodigoExpiradoException extends RuntimeException {
    public CodigoExpiradoException() {
        super("El código OTP ha expirado. Solicite uno nuevo.");
    }
}

