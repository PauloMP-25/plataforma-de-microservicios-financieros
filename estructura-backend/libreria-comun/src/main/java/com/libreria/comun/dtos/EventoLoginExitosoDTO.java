package com.libreria.comun.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa el evento de dominio disparado cuando un usuario
 * completa exitosamente el proceso de autenticación en el sistema LUKA.
 * <p>
 * Es publicado por el {@code microservicio-usuario} hacia el exchange
 * {@code exchange.usuario.eventos} con la routing key {@code usuario.login.exitoso}.
 * Todos los microservicios interesados en reaccionar al login del usuario
 * deben suscribirse a este evento.
 * </p>
 *
 * @param usuarioId     Identificador único del usuario que realizó el login.
 * @param ipOrigen      Dirección IP desde la cual se realizó la autenticación.
 * @param fechaLogin    Momento exacto en que ocurrió el login exitoso.
 * @param correlationId Identificador de correlación para trazabilidad distribuida.
 */
public record EventoLoginExitosoDTO(
        UUID usuarioId,
        String ipOrigen,
        LocalDateTime fechaLogin,
        String correlationId
) {

    /**
     * Factory method para construir el evento con la fecha actual.
     *
     * @param usuarioId     ID del usuario autenticado.
     * @param ipOrigen      IP de origen de la petición.
     * @param correlationId ID de correlación para trazabilidad.
     * @return nueva instancia de {@code EventoLoginExitosoDTO} con {@code fechaLogin} = ahora.
     */
    public static EventoLoginExitosoDTO de(UUID usuarioId, String ipOrigen, String correlationId) {
        return new EventoLoginExitosoDTO(usuarioId, ipOrigen, LocalDateTime.now(), correlationId);
    }
}
