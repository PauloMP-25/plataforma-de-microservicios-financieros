package com.auditoria.aplicacion.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO enriquecido para la visualización administrativa de auditoría.
 * <p>
 * A diferencia de los eventos internos, este objeto incluye información 
 * de identidad (email, nombre) para facilitar la gestión en el Dashboard.
 * </p>
 * 
 * @param id             Identificador único del registro.
 * @param fechaHora      Instante exacto del evento.
 * @param usuarioId      UUID del usuario asociado.
 * @param emailUsuario   Correo electrónico del usuario (para identificación rápida).
 * @param nombreCompleto Nombre y apellidos del usuario.
 * @param accion         Descripción de la operación realizada.
 * @param modulo         Microservicio de origen.
 * @param ipOrigen       Dirección IP desde donde se realizó la petición.
 * @param detalles       Información adicional o técnica sobre el evento.
 * 
 * @author Paulo Moron
 * @since 2026-05
 */
public record RespuestaAuditoriaDetalladoDTO(
    UUID id,
    LocalDateTime fechaHora,
    UUID usuarioId,
    String emailUsuario,
    String nombreCompleto,
    String accion,
    String modulo,
    String ipOrigen,
    String detalles
) {}
