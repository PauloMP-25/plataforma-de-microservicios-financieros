package com.libreria.comun.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa el evento de dominio disparado cuando un usuario
 * cierra sesión exitosamente en el sistema LUKA.
 * <p>
 * Es publicado por el {@code microservicio-usuario} hacia el exchange
 * {@code exchange.usuario.eventos} con la routing key {@code usuario.logout.exitoso}.
 * </p>
 *
 * @param usuarioId     Identificador único del usuario que realizó el logout.
 * @param ipOrigen      Dirección IP desde la cual se realizó el logout.
 * @param fechaLogout   Momento exacto en que ocurrió el logout.
 * @param correlationId Identificador de correlación para trazabilidad distribuida.
 */
public record EventoLogoutExitosoDTO(
        UUID usuarioId,
        String ipOrigen,
        LocalDateTime fechaLogout,
        String correlationId
) {

    public static EventoLogoutExitosoDTO de(UUID usuarioId, String ipOrigen, String correlationId) {
        return new EventoLogoutExitosoDTO(usuarioId, ipOrigen, LocalDateTime.now(), correlationId);
    }
}
