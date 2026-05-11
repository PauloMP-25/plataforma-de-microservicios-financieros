package com.auditoria.aplicacion.dtos;

import java.time.LocalDateTime;

/**
 * Respuesta del endpoint GET /verificar-ip/{ip}.
 * <p>
    DTO de respuesta de consulta del Gateway para decidir si rechaza o permite la petición.
 * </p>
 * 
 * @param ip              IP del usuario que esta realizando la peticion.
 * @param bloqueada       Estado de la IP puede ser true o false.
 * @param motivo          Motivo del bloqueo (intentos fallidos, spam, protocolo, etc).
 * @param fechaExpiracion Fecha de desbloqueo de la IP bloqueada (null si es permanente o no esta bloqueada).
 * 
 * @author Paulo Moron
 * @since 2026-05
 */
public record RespuestaVerificacionIpDTO(
    String  ip,
    boolean bloqueada,
    String  motivo,      
    LocalDateTime fechaExpiracion
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
