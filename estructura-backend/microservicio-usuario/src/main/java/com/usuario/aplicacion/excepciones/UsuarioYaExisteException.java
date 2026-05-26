package com.usuario.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// =========================================================================
// UsuarioYaExisteException
// =========================================================================
@ResponseStatus(HttpStatus.CONFLICT)
public class UsuarioYaExisteException extends RuntimeException {
public UsuarioYaExisteException(String campo, String valor) {
        super(String.format("Ya existe un usuario con %s: %s", campo, valor));
    }
}