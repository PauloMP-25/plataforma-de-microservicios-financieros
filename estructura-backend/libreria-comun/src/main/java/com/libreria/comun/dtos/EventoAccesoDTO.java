package com.libreria.comun.dtos;

import com.libreria.comun.enums.EstadoAcceso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un evento de seguridad relacionado con el acceso al sistema.
 * <p>
 * Se utiliza para registrar logins, logouts e intentos fallidos. 
 * Es enviado principalmente por el microservicio de identidad o el Gateway.
 * </p>
 * 
 * @param usuarioId    ID único del usuario (puede ser null si el usuario no existe).
 * @param ipOrigen     Dirección IP desde donde se realiza la petición (soporta IPv6).
 * @param navegador    Información del User-Agent o dispositivo.
 * @param estado       Resultado de la operación ({@link EstadoAcceso}).
 * @param detalleError Descripción técnica o amigable del porqué falló el acceso.
 * @param fecha        Momento exacto del evento.
 */
public record EventoAccesoDTO(
    UUID usuarioId,

    @NotBlank(message = "La IP de origen es obligatoria")
    @Size(max = 45, message = "IP no puede superar 45 caracteres")
    String ipOrigen,

    @Size(max = 500)
    String navegador,

    @NotNull(message = "El estado de acceso es obligatorio")
    EstadoAcceso estado,

    @Size(max = 500)
    String detalleError,

    LocalDateTime fecha
) {
    /**
     * Factory method para crear una instancia rápidamente con la fecha actual.
     */
    public static EventoAccesoDTO de(UUID usuarioId, String ip, EstadoAcceso estado, String detalle) {
        return new EventoAccesoDTO(usuarioId, ip, "LUKA-APP-CLIENT", estado, detalle, LocalDateTime.now());
    }
}
