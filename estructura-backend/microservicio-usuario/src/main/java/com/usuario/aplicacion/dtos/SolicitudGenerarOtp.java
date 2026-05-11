package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SolicitudGenerarOtp(
        @NotNull(message = "El usuarioId es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @Size(max = 15, message = "El teléfono no puede tener más de 15 caracteres")
        String telefono, // Opcional, se llena si el tipo es SMS

        @NotNull(message = "El tipo de canal es obligatorio")
        TipoVerificacion tipo, // <--- Faltaba este campo

        @NotNull(message = "El propósito es obligatorio")
        PropositoCodigo proposito 
) {}
