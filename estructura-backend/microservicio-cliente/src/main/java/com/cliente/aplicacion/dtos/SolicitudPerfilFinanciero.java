package com.cliente.aplicacion.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO de entrada para crear o actualizar el perfil financiero del cliente.
 */
@Data
public class SolicitudPerfilFinanciero {

    @Size(max = 100, message = "La ocupación no puede superar los 100 caracteres")
    private String ocupacion;

    @DecimalMin(value = "0.00", message = "El ingreso mensual no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual debe tener máximo 10 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    @Pattern(
            regexp = "^(AHORRATIVO|MODERADO|GASTADOR|INVERSOR)$",
            message = "Estilo de vida inválido. Valores: AHORRATIVO, MODERADO, GASTADOR, INVERSOR"
    )
    private String estiloVida;

    @Pattern(
            regexp = "^(FORMAL|AMIGABLE|MOTIVADOR|DIRECTO)$",
            message = "Tono IA inválido. Valores: FORMAL, AMIGABLE, MOTIVADOR, DIRECTO"
    )
    private String tonoIA;
}
