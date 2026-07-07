package com.cliente.aplicacion.dtos.respuestas;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * DTO de salida para DatosPersonales — nunca expone datos internos sensibles.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaDatosPersonales(
        String        dni,
        String        nombres,
        String        apellidos,
        String        genero,
        java.time.LocalDate fechaNacimiento,
        String        telefono,
        String        fotoPerfilUrl,
        String        pais,
        String        ciudad,
        Boolean       datosCompletos
) {}
