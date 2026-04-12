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

// =========================================================================
// TokenInvalidoException
// =========================================================================
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String razon) {
        super("Token inválido: " + razon);
    }
}

// =========================================================================
// ContrasenasNoCoincidenException
// =========================================================================
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ContrasenasNoCoincidenException extends RuntimeException {

    public ContrasenasNoCoincidenException() {
        super("Las contraseñas no coinciden.");
    }
}