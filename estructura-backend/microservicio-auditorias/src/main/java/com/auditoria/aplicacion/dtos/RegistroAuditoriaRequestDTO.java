package com.auditoria.aplicacion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO de ENTRADA: lo envía el iam-service (u otro microservicio).
 * Usa Java Record para inmutabilidad y concisión.
 */
public record RegistroAuditoriaRequestDTO(

    LocalDateTime fechaHora, // opcional: si no viene, se asigna en @PrePersist

    @NotBlank(message = "El campo 'usuario' es obligatorio")
    @Size(max = 150)
    String nombreUsuario,

    @NotBlank(message = "El campo 'accion' es obligatorio")
    @Size(max = 100)
    String accion,

    @NotBlank(message = "El campo 'modulo' es obligatorio")
    @Size(max = 100)
    String modulo,

    @Size(max = 45, message = "IP no puede superar 45 caracteres (IPv6)")
    String ipOrigen,

    String detalles
) {}
