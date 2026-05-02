package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LimiteGastoException extends RuntimeException {
    public LimiteGastoException(String mensaje) {
        super(mensaje);
    }
}