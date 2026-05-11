package com.cliente.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

/**
 * DTO de contexto completo del cliente para el microservicio-nucleo-financiero.
 * Se expone a través del endpoint interno /contexto/{usuarioId}.
 *
 * Permite que el núcleo financiero acceda a toda la información relevante del
 * cliente en una sola llamada Feign.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaContexto(
                UUID usuarioId,
                RespuestaDatosPersonales datosPersonales,
                RespuestaPerfilFinanciero perfilFinanciero,
                List<RespuestaMetaAhorro> metas,
                RespuestaLimiteGasto limiteGasto) {
}
