package com.nucleo.financiero.aplicacion.dtos.cliente;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Límite de gasto mensual configurado por el usuario.
 * Python: LimiteGlobal(montoLimite, porcentajeAlerta, activo)
 */
@Value
@Builder
public class LimiteGlobalDTO {

    @JsonProperty("montoLimite")
    double montoLimite;

    @JsonProperty("porcentajeAlerta")
    int porcentajeAlerta;

    boolean activo;
}
