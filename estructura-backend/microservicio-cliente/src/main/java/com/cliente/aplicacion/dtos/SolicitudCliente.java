package com.cliente.aplicacion.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO de entrada para actualizar el perfil del cliente.
 */
@Data
public class SolicitudCliente {

    @Size(min = 2, max = 100, message = "Los nombres deben tener entre 2 y 100 caracteres")
    private String nombres;

    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;

    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener exactamente 8 dígitos")
    private String dni;

    @Size(max = 500)
    private String fotoPerfilUrl;

    @Size(max = 1000, message = "La biografía no puede superar los 1000 caracteres")
    private String biografia;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Número de celular inválido")
    private String numeroCelular;

    @Size(max = 255)
    private String direccion;

    @Size(max = 100)
    private String ciudad;

    @Size(max = 100)
    private String ocupacion;

    @Pattern(regexp = "^(MASCULINO|FEMENINO|OTRO|PREFIERO_NO_DECIR)$",
             message = "Género inválido. Valores: MASCULINO, FEMENINO, OTRO, PREFIERO_NO_DECIR")
    private String genero;
}