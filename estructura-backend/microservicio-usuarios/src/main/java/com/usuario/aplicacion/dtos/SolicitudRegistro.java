package com.usuario.aplicacion.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * Payload de registro. Incluye campos básicos y confirmación de contraseña.
 * @author Paulo
 */
@Data
public class SolicitudRegistro {
    @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 100, message = "El usuario debe tener entre 3 y 100 caracteres")
        String nombreUsuario;

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo debe tener un formato válido")
        String correo;

        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(regexp = ".*[a-z].*", message = "Debe contener al menos una letra minuscula")
        @Pattern(regexp = ".*[A-Z].*", message = "Debe contener al menos una letra mayuscula")
        @Pattern(regexp = ".*\\d.*", message = "Debe contener al menos un número")
        @Pattern(regexp = ".{6,}", message = "Debe tener al menos 6 caracteres")
        String password;

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmarPassword;
        
    /**
     * Valida que las contraseñas coincidan (uso en capa de servicio).
     */
    public boolean contrasenasCoinciden(){
        return password != null && password.equals(confirmarPassword);
    }
    
    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }
}
