package com.nucleo.financiero.aplicacion.dtos.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de ENTRADA: objeto recibido desde el microservicio-ia (Python/FastAPI).
 * Espejo del ResultadoAnalisisIA de Python.
 *
 * JSON que llega desde Python:
 * {
 *   "id":               "uuid-string",
 *   "id_usuario":       "uuid-string",
 *   "consejo_texto":    "Durante este mes tus gastos subieron un 12%...",
 *   "tipo_modulo":      "COMPARACION_MENSUAL",
 *   "fecha_generacion": "2026-04-29T10:30:00.123456",
 *   "metadata_grafico": { ... },     // null si el módulo no genera gráfico
 *   "kpi_principal":    -12.5,       // null si no aplica
 *   "kpi_label":        "% vs mes anterior"
 * }
 *
 * @JsonIgnoreProperties(ignoreUnknown = true): si Python añade campos nuevos
 * en el futuro, Java no rompe — principio de compatibilidad hacia adelante.
 *
 * Se usa @Data + @NoArgsConstructor (no @Value) porque Jackson necesita
 * poder instanciar y setear campos al deserializar.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaIaDTO {

    /** UUID generado por Python para este resultado de análisis. */
    String id;

    @JsonProperty("id_usuario")
    String idUsuario;

    /**
     * Texto del consejo generado por Gemini.
     * Listo para mostrar en el widget del Dashboard.
     */
    @JsonProperty("consejo_texto")
    String consejoTexto;

    /**
     * Módulo que generó este resultado.
     * Permite al Dashboard saber qué widget renderizar.
     */
    @JsonProperty("tipo_modulo")
    String tipoModulo;

    /**
     * ISO-8601 string: "2026-04-29T10:30:00.123456"
     * Usar String aquí evita problemas de zona horaria entre Python y Java.
     * Convertir a LocalDateTime si se necesita persistir.
     */
    @JsonProperty("fecha_generacion")
    String fechaGeneracion;

    /**
     * Datos para renderizar el gráfico en el Dashboard.
     * Null si el módulo no genera gráfico.
     */
    @JsonProperty("metadata_grafico")
    MetadataGraficoDTO metadataGrafico;

    /**
     * Valor numérico destacado para el header del widget.
     * Ejemplo: -12.5 (% de variación), 250.0 (S/ en gastos hormiga)
     * Null si el módulo no tiene KPI principal.
     */
    @JsonProperty("kpi_principal")
    Double kpiPrincipal;

    /** Etiqueta del KPI. Ejemplo: "% vs mes anterior", "S/ en gastos hormiga" */
    @JsonProperty("kpi_label")
    String kpiLabel;

    // ── Sub-DTO para metadata_grafico ─────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataGraficoDTO {

        /** "line", "bar", "pie", "doughnut" */
        @JsonProperty("tipo_grafico")
        String tipoGrafico;

        String titulo;

        /** Serie principal de datos para el gráfico. */
        List<PuntoGraficoDTO> datos;

        /** Serie auxiliar (ej: datos del mes anterior para comparar). */
        @JsonProperty("datos_aux")
        List<PuntoGraficoDTO> datosAux;

        /** "S/" u otras unidades. */
        String unidad;

        /** Línea de referencia en el gráfico (ej: 0 para balance, proyección). */
        @JsonProperty("meta_linea")
        Double metaLinea;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PuntoGraficoDTO {

        /** Etiqueta del eje X. Ejemplo: "2026-04", "Alimentación" */
        String etiqueta;

        double valor;

        /** Color hex opcional para el frontend. Ejemplo: "#E74C3C" */
        String color;
    }
}
