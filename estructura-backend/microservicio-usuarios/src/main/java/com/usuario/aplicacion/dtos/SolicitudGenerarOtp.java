package com.usuario.aplicacion.dtos;

import java.util.UUID;

/**
 * DTO para solicitar al microservicio de mensajería la generación de un código.
 */
public record SolicitudGenerarOtp(
    UUID usuarioId,
    String correo,
    String tipo // En este caso siempre será "EMAIL"
) {}

