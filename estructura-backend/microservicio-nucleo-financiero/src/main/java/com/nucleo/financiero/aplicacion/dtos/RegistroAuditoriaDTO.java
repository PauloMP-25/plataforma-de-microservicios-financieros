package com.nucleo.financiero.aplicacion.dtos;

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
        String modulo,
        String ipOrigen,
        String detalles) {

    /**
     * Constructor de conveniencia sin fechaHora (el receptor la asigna).
     */
    public RegistroAuditoriaDTO(String nombreUsuario, String accion,
            String detalles, String direccionIp, String modulo) {
        this(null, nombreUsuario, accion, detalles, direccionIp, modulo);
    }
}
