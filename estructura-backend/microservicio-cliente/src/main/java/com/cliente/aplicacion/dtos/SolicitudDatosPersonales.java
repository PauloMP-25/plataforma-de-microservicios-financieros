package com.cliente.aplicacion.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de entrada para crear o actualizar los datos personales del cliente.
 */
@Data
public class SolicitudDatosPersonales {

    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
    private String nombres;

    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;

    @Pattern(
            regexp = "^(MASCULINO|FEMENINO|OTRO|PREFIERO_NO_DECIR)$",
            message = "Género inválido. Valores: MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR"
    )
    private String genero;

    @Min(value = 18, message = "La edad mínima es 18 años")
    @Max(value = 120, message = "La edad máxima es 120 años")
    private Integer edad;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Número de teléfono inválido")
    private String telefono;

    @Size(max = 500, message = "La URL de la foto no puede superar los 500 caracteres")
    private String fotoPerfilUrl;

    @Size(max = 20, message = "El nombre del pais no puede superar los 20 caracteres")
    private String Pais;

    @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
    private String ciudad;
}
