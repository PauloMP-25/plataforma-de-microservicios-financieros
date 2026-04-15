package com.cliente.aplicacion.dtos;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * DTO de SALIDA: lo que devuelve la API al consultar registros de auditoría.
 * Java Record: inmutable, compacto, ideal para respuestas de lectura.
 */
@Builder
public record RegistroAuditoriaDTO(
        LocalDateTime fechaHora,
        String nombreUsuario,
        String accion,
        String detalles,
        String ipOrigen,
        String modulo) {

    /**
     * Constructor de conveniencia sin fechaHora (el receptor la asigna).
     */
    public RegistroAuditoriaDTO(String nombreUsuario, String accion, String detalles, String ipOrigen, String modulo) {
        this(LocalDateTime.now(), nombreUsuario, accion, detalles, ipOrigen, modulo);
    }
}
