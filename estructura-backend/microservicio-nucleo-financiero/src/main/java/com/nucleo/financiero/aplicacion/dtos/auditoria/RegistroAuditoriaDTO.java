package com.nucleo.financiero.aplicacion.dtos.auditoria;

import java.time.LocalDateTime;
import lombok.Builder;

/**
 * DTO de SALIDA: lo que devuelve la API al consultar registros de auditoría.
 * Java Record: inmutable, compacto, ideal para respuestas de lectura.
 */
@Builder
public record RegistroAuditoriaDTO(
        LocalDateTime fechaHora,
        String usuarioId,
        String accion,
        String modulo,
        String ipOrigen,
        String detalles) {

    /**
     * Constructor de conveniencia sin fechaHora (el receptor la asigna).
     */
    public RegistroAuditoriaDTO(String usuarioId, String accion,
            String detalles, String direccionIp, String modulo) {
        this(null, usuarioId, accion, detalles, direccionIp, modulo);
    }
}
