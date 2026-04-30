package com.nucleo.financiero.aplicacion.dtos.ia;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Espejo exacto del modelo Python ResumenMes (evento_analisis.py).
 *
 * Python espera alias camelCase en la deserialización
 * (populate_by_name=True, alias="totalIngresos", etc.),
 * por lo que Jackson debe enviar los campos en camelCase.
 *
 * Contrato de campos:
 *   anio              → "anio"
 *   mes               → "mes"
 *   totalIngresos     → alias "totalIngresos"  ← camelCase para Python
 *   totalGastos       → alias "totalGastos"
 *   gastosPorCategoria → alias "gastosPorCategoria"
 *
 * Se usa @Value (Lombok) para inmutabilidad.
 * Se usa @Builder para construcción fluida desde el ServicioTransaccion.
 */
@Value
@Builder
public class ResumenMesDTO {

    int anio;
    int mes;

    @JsonProperty("totalIngresos")
    double totalIngresos;

    @JsonProperty("totalGastos")
    double totalGastos;

    /**
     * Mapa clave: nombre de categoría → monto total del mes.
     * Ejemplo: {"Alimentación": 800.0, "Entretenimiento": 350.0}
     * Python lo deserializa como gastos_por_categoria: Dict[str, float]
     */
    @JsonProperty("gastosPorCategoria")
    Map<String, Double> gastosPorCategoria;
}
