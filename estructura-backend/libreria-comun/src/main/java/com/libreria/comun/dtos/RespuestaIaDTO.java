package com.libreria.comun.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Respuesta enriquecida del microservicio de IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaIaDTO {

    private String id;

    @JsonProperty("id_usuario")
    private String idUsuario;

    @JsonProperty("consejo_texto")
    private String consejoTexto;

    @JsonProperty("tipo_modulo")
    private String tipoModulo;

    @JsonProperty("fecha_generacion")
    private String fechaGeneracion;

    @JsonProperty("metadata_grafico")
    private MetadataGraficoDTO metadataGrafico;

    @JsonProperty("kpi_principal")
    private Double kpiPrincipal;

    @JsonProperty("kpi_label")
    private String kpiLabel;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetadataGraficoDTO {
        @JsonProperty("tipo_grafico")
        private String tipoGrafico;
        private String titulo;
        private List<PuntoGraficoDTO> datos;
        @JsonProperty("datos_aux")
        private List<PuntoGraficoDTO> datosAux;
        private String unidad;
        @JsonProperty("meta_linea")
        private Double metaLinea;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PuntoGraficoDTO {
        private String etiqueta;
        private double valor;
        private String color;
    }
}
