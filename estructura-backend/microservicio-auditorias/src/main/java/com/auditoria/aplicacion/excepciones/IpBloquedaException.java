package com.auditoria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando se intenta registrar un acceso desde una IP
 * que ya está en la lista negra activa.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class IpBloqueadaException extends RuntimeException {

    private final String ip;

    public IpBloqueadaException(String ip) {
        super(String.format("La IP '%s' está bloqueada y no puede realizar más peticiones.", ip));
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }
}
