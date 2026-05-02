package com.cliente.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
/**
 * DTO de salida para DatosPersonales — nunca expone datos internos sensibles.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaDatosPersonales(
        String        dni,
        String        nombres,
        String        apellidos,
        String        genero,
        Integer       edad,
        String        telefono,
        String        fotoPerfilUrl,
        String        direccion,
        String        ciudad,
        Boolean       datosCompletos,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {}
