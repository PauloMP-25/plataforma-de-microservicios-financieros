package com.nucleo.financiero.aplicacion.dtos.cliente;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Contexto enriquecido del usuario enviado junto al historial.
 * Todos los campos son opcionales en Python — si llegan null,
 * el microservicio-ia usa defaults seguros.
 */
@Value
@Builder
public class ContextoUsuarioDTO {
    
    
    
    @JsonProperty("perfilFinanciero")
    PerfilFinancieroDTO perfilFinanciero;

    /** Lista de metas activas. Puede ser null o vacío. */
    List<MetaAhorroDTO> metas;

    @JsonProperty("limiteGlobal")
    LimiteGlobalDTO limiteGlobal;
}
