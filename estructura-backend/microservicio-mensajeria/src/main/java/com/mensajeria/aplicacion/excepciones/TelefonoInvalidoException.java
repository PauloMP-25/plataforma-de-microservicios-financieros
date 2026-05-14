package com.mensajeria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando el formato del teléfono no es válido para el canal solicitado
 * (ej. falta el código de país para WhatsApp).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TelefonoInvalidoException extends RuntimeException {
    public TelefonoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
