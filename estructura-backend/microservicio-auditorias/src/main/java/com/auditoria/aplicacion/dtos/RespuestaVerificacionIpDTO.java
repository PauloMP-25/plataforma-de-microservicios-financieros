package com.auditoria.aplicacion.dtos;

import java.time.LocalDateTime;

// ──────────────────────────────────────────────────────────────────────────────
// DTO de SALIDA — Respuesta al verificar si una IP está bloqueada
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Respuesta del endpoint GET /verificar-ip/{ip}.
 * El Gateway la utiliza para decidir si rechaza o permite la petición.
 */
public record RespuestaVerificacionIpDTO(
    String  ip,
    boolean bloqueada,
    String  motivo,            // null si no está bloqueada
    LocalDateTime fechaExpiracion  // null si es permanente o no está bloqueada
) {
    /** Factory method para una IP NO bloqueada. */
    public static RespuestaVerificacionIpDTO libre(String ip) {
        return new RespuestaVerificacionIpDTO(ip, false, null, null);
    }

    /** Factory method para una IP bloqueada. */
    public static RespuestaVerificacionIpDTO bloqueada(String ip, String motivo, LocalDateTime expiracion) {
        return new RespuestaVerificacionIpDTO(ip, true, motivo, expiracion);
    }
}
