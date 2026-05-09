package com.auditoria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción de negocio lanzada cuando se detecta actividad desde una dirección IP 
 * que se encuentra en la lista negra activa.
 * <p>
 * Devuelve un estado HTTP 429 (Too Many Requests) al cliente o Gateway.
 * </p>
 * 
 * @author Paulo Moron
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class IpBloqueadaException extends RuntimeException {

    /**
     * Construye la excepción con un mensaje detallado sobre la IP restringida.
     * 
     * @param ip Dirección IP bloqueada.
     */
    public IpBloqueadaException(String ip) {
        super(String.format("Acceso denegado: La IP %s se encuentra bloqueada por seguridad.", ip));
    }
}
