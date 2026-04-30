package com.nucleo.financiero.aplicacion.dtos.cliente;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Meta de ahorro activa del usuario.
 * Python: MetaAhorro(nombre, montoObjetivo, montoActual)
 */
@Value
@Builder
public class MetaAhorroDTO {

    String nombre;

    @JsonProperty("montoObjetivo")
    double montoObjetivo;

    @JsonProperty("montoActual")
    double montoActual;
}
