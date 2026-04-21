package com.usuario.aplicacion.dtos;

import java.util.UUID;

/**
 * DTO para solicitar al microservicio de mensajería la generación de un código.
 */
public record SolicitudGenerarOtp(
    UUID usuarioId,
    String email,
    String tipo,
    String proposito // En este caso siempre será "EMAIL"
) {}

