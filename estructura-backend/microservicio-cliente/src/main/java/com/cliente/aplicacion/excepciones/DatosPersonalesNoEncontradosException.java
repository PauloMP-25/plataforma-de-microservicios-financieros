package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DatosPersonalesNoEncontradosException extends RuntimeException {
    public DatosPersonalesNoEncontradosException(UUID usuarioId) {
        super("No existen datos personales para el usuarioId: " + usuarioId);
    }
}
