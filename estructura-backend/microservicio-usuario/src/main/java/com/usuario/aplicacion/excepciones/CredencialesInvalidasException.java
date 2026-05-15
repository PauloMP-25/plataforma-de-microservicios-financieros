package com.usuario.aplicacion.excepciones;

/**
 * Lanzada cuando las credenciales ingresadas son incorrectas.
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
