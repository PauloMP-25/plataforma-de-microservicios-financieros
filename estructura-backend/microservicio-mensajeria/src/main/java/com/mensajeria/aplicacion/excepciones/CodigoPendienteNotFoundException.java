package com.mensajeria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CodigoPendienteNotFoundException extends RuntimeException {

    public CodigoPendienteNotFoundException(java.util.UUID usuarioId) {
        super(String.format("No hay un código OTP pendiente para el usuario '%s'.", usuarioId));
    }
}
