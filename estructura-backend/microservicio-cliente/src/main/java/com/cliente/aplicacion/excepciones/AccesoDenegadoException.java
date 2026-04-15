package com.cliente.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando un usuario intenta modificar el perfil de otro usuario.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException() {
        super("No tiene permisos para modificar este perfil. Solo el propietario puede editarlo.");
    }
}
