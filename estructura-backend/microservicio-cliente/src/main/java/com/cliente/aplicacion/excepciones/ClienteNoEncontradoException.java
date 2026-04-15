package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(UUID usuarioId) {
        super("No existe perfil de cliente para el usuarioId: " + usuarioId);
    }
}
