package com.cliente.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de salida — nunca expone datos sensibles internos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaCliente(
        UUID          id,
        UUID          usuarioId,
        String        nombres,
        String        apellidos,
        String        dni,
        String        fotoPerfilUrl,
        String        biografia,
        String        numeroCelular,
        String        direccion,
        String        ciudad,
        String        ocupacion,
        String        genero,
        Boolean       perfilCompleto,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}
