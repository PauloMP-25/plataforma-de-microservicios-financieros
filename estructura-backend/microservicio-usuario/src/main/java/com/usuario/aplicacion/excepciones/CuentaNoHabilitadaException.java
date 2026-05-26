package com.usuario.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
// =========================================================================
// CuentaNoHabilitadaException
// =========================================================================
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CuentaNoHabilitadaException extends RuntimeException {
    public CuentaNoHabilitadaException(String nombreUsuario) {
        super(String.format(
                "La cuenta '%s' no ha sido activada. Revise su correo electrónico.",
                nombreUsuario
        ));
    }
}
