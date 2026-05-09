package com.auditoria.aplicacion.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de error estandarizada. Garantiza un contrato uniforme en toda la API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorApi(
    int            estado,
    String         error,
    String         mensaje,
    String         ruta,
    LocalDateTime  fechaHora,
    List<String>   detalles
) {
    public static ErrorApi of(int estado, String error, String mensaje, String ruta) {
        return new ErrorApi(estado, error, mensaje, ruta, LocalDateTime.now(), null);
    }

    public static ErrorApi of(int estado, String error, String mensaje, String ruta, List<String> detalles) {
        return new ErrorApi(estado, error, mensaje, ruta, LocalDateTime.now(), detalles);
    }
}
