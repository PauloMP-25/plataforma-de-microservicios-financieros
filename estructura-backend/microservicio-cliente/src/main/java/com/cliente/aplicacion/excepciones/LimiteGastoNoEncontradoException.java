package com.cliente.aplicacion.excepciones;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author user
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class LimiteGastoNoEncontradoException extends RuntimeException {
    public LimiteGastoNoEncontradoException(UUID usuarioId) {
        super("No existe un limite de gasto activo para el usuario con id: " + usuarioId);
    }
}
