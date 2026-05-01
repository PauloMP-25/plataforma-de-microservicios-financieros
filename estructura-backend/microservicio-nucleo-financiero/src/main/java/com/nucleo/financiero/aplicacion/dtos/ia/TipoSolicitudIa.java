package com.nucleo.financiero.aplicacion.dtos.ia;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Discrimina el origen de la solicitud hacia el microservicio-ia.
 *
 * TRANSACCION_RECIENTE → El nucleo-financiero dispara el análisis
 *   automáticamente al registrar una transacción. La respuesta
 *   llega a cola.dashboard.consejos.
 *
 * CONSULTA_MODULO → El usuario pulsó un botón en el Dashboard.
 *   Se envía el modulo_solicitado explícitamente. La respuesta
 *   llega a cola.dashboard.modulos.
 */
public enum TipoSolicitudIa {

    TRANSACCION_RECIENTE,
    CONSULTA_MODULO;

    @JsonValue
    public String toJson() {
        return this.name();
    }
}
