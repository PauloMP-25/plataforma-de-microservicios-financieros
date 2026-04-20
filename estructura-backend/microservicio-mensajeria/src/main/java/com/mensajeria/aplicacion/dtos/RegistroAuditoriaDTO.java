package com.mensajeria.aplicacion.dtos;

import java.time.LocalDateTime;

public record RegistroAuditoriaDTO(
        LocalDateTime fechaHora,
        String nombreUsuario,
        String accion,
        String detalles,
        String ipOrigen,
        String modulo) {

    public RegistroAuditoriaDTO(String nombreUsuario, String accion, String detalles, String ipOrigen, String modulo) {
        this(LocalDateTime.now(), nombreUsuario, accion, detalles, ipOrigen, modulo);
    }
}
