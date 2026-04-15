package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DniDuplicadoException extends RuntimeException {
    public DniDuplicadoException(String dni) {
        super("El DNI '" + dni + "' ya está registrado en el sistema.");
    }
}
