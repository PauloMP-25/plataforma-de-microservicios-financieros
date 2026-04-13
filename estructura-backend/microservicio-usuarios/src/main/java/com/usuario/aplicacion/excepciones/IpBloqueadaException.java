package com.usuario.aplicacion.excepciones;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * IP bloqueada por exceso de intentos fallidos.
 * @author Paulo
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class IpBloqueadaException extends RuntimeException {
    private final String direccionIp;
    private final long minutosParaDesbloqueo;

    public IpBloqueadaException(String direccionIp, long minutosParaDesbloqueo) {
        super(String.format(
            "Demasiados intentos fallidos. La IP %s ha sido bloqueada por seguridad. Intente nuevamente en %d minutos.",
            direccionIp,
            minutosParaDesbloqueo
        ));
        this.direccionIp = direccionIp;
        this.minutosParaDesbloqueo = minutosParaDesbloqueo;
    }
    public String getDireccionIp() {
        return direccionIp;
    }

    public long getMinutosParaDesbloqueo() {
        return minutosParaDesbloqueo;
    }
}
