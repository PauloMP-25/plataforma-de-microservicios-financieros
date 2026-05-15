package com.usuario.aplicacion.excepciones;

/**
 * Lanzada cuando el usuario no existe en la base de datos.
 */
public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String message) {
        super(message);
    }
}
