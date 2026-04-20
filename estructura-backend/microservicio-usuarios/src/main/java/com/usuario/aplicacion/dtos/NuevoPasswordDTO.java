package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NuevoPasswordDTO(
        @NotBlank(message = "El código es obligatorio")
        String codigoOtp,
        @NotBlank
        @Size(min = 8)
        @Pattern(regexp = ".*[a-z].*", message = "Minúscula obligatoria")
        @Pattern(regexp = ".*[A-Z].*", message = "Mayúscula obligatoria")
        @Pattern(regexp = ".*\\d.*", message = "Número obligatorio")
        @Pattern(regexp = ".*[@$!%*?&].*", message = "Carácter especial obligatorio")
        String nuevoPassword,
        @NotBlank
        String confirmarPassword) {

    public boolean contrasenasCoinciden() {
        return nuevoPassword != null && nuevoPassword.equals(confirmarPassword);
    }
}
