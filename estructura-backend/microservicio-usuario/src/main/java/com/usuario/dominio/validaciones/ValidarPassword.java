package com.usuario.dominio.validaciones;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
// Composición de validaciones
@NotBlank(message = "La contraseña no puede estar vacía")
@Size(min = 8, message = "Debe tener al menos 8 caracteres")
@Pattern(regexp = ".*[a-z].*", message = "Debe contener al menos una minúscula")
@Pattern(regexp = ".*[A-Z].*", message = "Debe contener al menos una mayúscula")
@Pattern(regexp = ".*\\d.*", message = "Debe contener al menos un número")
@Pattern(regexp = ".*[@$!%*?&#].*", message = "Debe contener un carácter especial (@$!%*?&#-)")
public @interface ValidarPassword {
    String message() default "La contraseña no cumple con los requisitos de seguridad";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
