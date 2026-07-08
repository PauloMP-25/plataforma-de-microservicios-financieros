package com.nucleo.financiero.aplicacion.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa la racha actual de días registrando transacciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RachaDTO {
    private int diasRacha;
    private int oportunidadesRestantes;
    private List<String> diasActivosMesActual;
}
