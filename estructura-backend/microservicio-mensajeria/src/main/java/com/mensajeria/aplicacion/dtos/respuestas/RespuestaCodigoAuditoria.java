package com.mensajeria.aplicacion.dtos.respuestas;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.libreria.comun.enums.PropositoCodigo;
import com.libreria.comun.enums.TipoVerificacion;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para auditoría de códigos OTP.
 * Omitimos intencionalmente el código de verificación para evitar filtraciones de seguridad.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaCodigoAuditoria(
        UUID id,
        UUID usuarioId,
        String email,
        String telefono,
        TipoVerificacion tipo,
        PropositoCodigo proposito,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        Boolean usado,
        LocalDateTime fechaUso
) {}
