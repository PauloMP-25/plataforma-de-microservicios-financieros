package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la renovación de tokens de acceso.
 */
public record SolicitudRefreshToken(
    @NotBlank(message = "El refresh token es obligatorio")
    String refreshToken
) {}
