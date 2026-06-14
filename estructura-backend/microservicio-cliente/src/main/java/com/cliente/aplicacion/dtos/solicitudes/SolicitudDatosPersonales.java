package com.cliente.aplicacion.dtos.solicitudes;

import jakarta.validation.constraints.*;

/**
 * DTO de entrada para crear o actualizar los datos personales del cliente.
 */
public record SolicitudDatosPersonales(
        @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener exactamente 8 dígitos")
        String dni,

        @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
        String nombres,

        @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
        String apellidos,

        @Pattern(
                regexp = "^(MASCULINO|FEMENINO|OTRO|PREFIERO_NO_DECIR)$",
                message = "Género inválido. Valores: MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR"
        )
        String genero,

        @Min(value = 18, message = "La edad mínima es 18 años")
        @Max(value = 120, message = "La edad máxima es 120 años")
        Integer edad,

        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Número de teléfono inválido")
        String telefono,

        @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
        String fotoPerfilUrl,

        @Size(max = 20, message = "El nombre del pais no puede superar los 20 caracteres")
        String pais,

        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String ciudad
) {}
