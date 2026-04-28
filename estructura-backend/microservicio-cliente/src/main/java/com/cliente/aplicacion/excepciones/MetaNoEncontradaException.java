package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MetaNoEncontradaException extends RuntimeException {
    public MetaNoEncontradaException(UUID metaId) {
        super("No existe la meta de ahorro con id: " + metaId);
    }
}