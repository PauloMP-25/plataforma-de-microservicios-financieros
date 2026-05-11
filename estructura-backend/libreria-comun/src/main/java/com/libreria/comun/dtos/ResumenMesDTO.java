package com.libreria.comun.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Resumen consolidado de ingresos y gastos de un mes específico.
 * <p>
 * Diseñado para ser consumido por el motor de IA para análisis de tendencias.
 * Mantiene compatibilidad con el modelo Pydantic de Python mediante alias en camelCase.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMesDTO {

    private int anio;
    private int mes;

    @JsonProperty("totalIngresos")
    private double totalIngresos;

    @JsonProperty("totalGastos")
    private double totalGastos;

    @JsonProperty("gastosPorCategoria")
    private Map<String, Double> gastosPorCategoria;
}
