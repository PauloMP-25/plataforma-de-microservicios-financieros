package com.nucleo.financiero.aplicacion.puertos;

import com.nucleo.financiero.aplicacion.dtos.RachaDTO;

import java.util.UUID;

/**
 * Puerto de entrada (interfaz) para los casos de uso relacionados con resúmenes financieros.
 */
public interface IResumenService {

    /**
     * Calcula la racha actual de días consecutivos o válidos en los que el usuario
     * ha registrado transacciones, así como los días activos del mes actual.
     *
     * @param usuarioId ID del usuario
     * @return DTO con la información de la racha
     */
    RachaDTO calcularRacha(UUID usuarioId);
}
