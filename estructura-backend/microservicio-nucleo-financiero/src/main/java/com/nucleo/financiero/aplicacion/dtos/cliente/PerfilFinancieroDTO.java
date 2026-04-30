package com.nucleo.financiero.aplicacion.dtos.cliente;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Contexto del perfil del usuario enviado al microservicio-ia.
 * Python lo deserializa como PerfilFinanciero con populate_by_name=True.
 *
 * Nota: tonoIA acepta los valores: Motivador | Formal | Amigable | Directo | Empático
 * Si se envía un valor desconocido, Python hace fallback a "Amigable".
 */
@Value
@Builder
public class PerfilFinancieroDTO {

    String ocupacion;

    @JsonProperty("ingresoMensual")
    double ingresoMensual;

    /**
     * Controla el estilo de comunicación del Coach IA.
     * Valores válidos (case-insensitive en Python):
     *   "Motivador", "Formal", "Amigable", "Directo", "Empático"
     */
    @JsonProperty("tonoIA")
    String tonoIA;
}
