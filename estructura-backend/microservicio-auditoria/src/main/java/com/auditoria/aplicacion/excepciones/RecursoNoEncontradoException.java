package com.auditoria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando no se encuentra un recurso solicitado en la base de datos.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String identificador) {
        super(String.format("%s no encontrado con identificador: %s", recurso, identificador));
    }
}
