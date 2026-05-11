package com.libreria.comun.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Define el disparador o naturaleza de la solicitud hacia la IA.
 */
public enum TipoSolicitudIa {
    TRANSACCION_RECIENTE,
    CONSULTA_MODULO;

    @JsonValue
    public String getValue() {
        return name();
    }
}
