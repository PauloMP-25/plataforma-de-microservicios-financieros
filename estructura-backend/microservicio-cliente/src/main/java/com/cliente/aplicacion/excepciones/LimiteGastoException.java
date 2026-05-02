package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LimiteGastoDuplicadoException extends RuntimeException {
    public LimiteGastoDuplicadoException(String categoria) {
        super("Ya existe un límite de gasto para la categoría: '" + categoria + "'. Use PUT para actualizar.");
    }
}