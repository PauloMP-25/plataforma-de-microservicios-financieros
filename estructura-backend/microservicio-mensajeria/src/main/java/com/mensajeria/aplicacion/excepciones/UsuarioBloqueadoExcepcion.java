package com.mensajeria.aplicacion.excepciones;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// ──────────────────────────────────────────────────────────────
// UsuarioBloqueadoException
// ──────────────────────────────────────────────────────────────
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class UsuarioBloqueadoExcepcion extends RuntimeException {

    private final java.util.UUID usuarioId;
    private final long horasRestantes;

    public UsuarioBloqueadoExcepcion(java.util.UUID usuarioId, long horasRestantes) {
        super(String.format(
                "El usuario '%s' ha sido bloqueado por exceder el número de intentos. "
                + "Intente nuevamente en %d hora(s).",
                usuarioId, horasRestantes
        ));
        this.usuarioId = usuarioId;
        this.horasRestantes = horasRestantes;
    }

    public java.util.UUID getUsuarioId() {
        return usuarioId;
    }

    public long getHorasRestantes() {
        return horasRestantes;
    }
}
