package com.auditoria.aplicacion.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de SALIDA: lo que devuelve la API al consultar registros de auditoría.
 * Java Record: inmutable, compacto, ideal para respuestas de lectura.
 */
public record RegistroAuditoriaDTO(
    UUID          id,
    LocalDateTime fechaHora,
    String        usuario,
    String        accion,
    String        modulo,
    String        ipOrigen,
    String        detalles,
    String        nivel
) {}
