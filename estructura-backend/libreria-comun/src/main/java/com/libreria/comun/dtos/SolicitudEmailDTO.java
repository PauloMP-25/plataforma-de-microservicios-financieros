package com.libreria.comun.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.Map;

/**
 * DTO para envío de correos electrónicos genéricos.
 */
@Builder
public record SolicitudEmailDTO(
    @NotBlank(message = "El destinatario es obligatorio")
    @Email(message = "Formato de email inválido")
    String destinatario,

    @NotBlank(message = "El asunto es obligatorio")
    String asunto,

    @NotBlank(message = "El cuerpo es obligatorio")
    String cuerpo,

    boolean esHtml,

    Map<String, Object> variables
) {
}
