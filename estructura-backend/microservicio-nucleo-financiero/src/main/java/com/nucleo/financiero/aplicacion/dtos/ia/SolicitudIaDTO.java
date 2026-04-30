package com.nucleo.financiero.aplicacion.dtos.ia;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nucleo.financiero.aplicacion.dtos.cliente.ContextoUsuarioDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * DTO de SALIDA: payload enviado al microservicio-ia (Python/FastAPI).
 * Endpoint destino: POST /api/v1/ia/analizar   (o via RabbitMQ exchange.ia)
 *
 * Contrato JSON resultante:
 * {
 *   "id_usuario":         "uuid-string",
 *   "tipo_solicitud":     "TRANSACCION_RECIENTE" | "CONSULTA_MODULO",
 *   "modulo_solicitado":  "GASTO_HORMIGA" | null,
 *   "historial_mensual":  [ { "anio":2026, "mes":4, ... } ],
 *   "contexto": {
 *     "perfilFinanciero": { ... },
 *     "metas":            [ ... ],
 *     "limiteGlobal":     { ... }
 *   }
 * }
 *
 * DECISIONES DE DISEÑO:
 *   - @JsonProperty en snake_case: Python espera id_usuario, tipo_solicitud, etc.
 *   - @JsonInclude(NON_NULL): modulo_solicitado se omite si es null,
 *     evitando que Python reciba el campo con valor null y no lo reconozca.
 *   - ModuloIa serializa con @JsonValue → name() exacto en SNAKE_CASE.
 */

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolicitudIaDTO {

    @NotBlank
    @JsonProperty("id_usuario")
    String idUsuario;

    @NotNull
    @JsonProperty("tipo_solicitud")
    TipoSolicitudIa tipoSolicitud;

    /**
     * Solo presente cuando tipo_solicitud == CONSULTA_MODULO.
     * Si es TRANSACCION_RECIENTE, se omite del JSON (@JsonInclude NON_NULL).
     */
    @JsonProperty("modulo_solicitado")
    ModuloIa moduloSolicitado;

    /**
     * Historial de los últimos N meses (configurable, default 6).
     * El microservicio-ia usa estos datos para análisis comparativo,
     * detección de tendencias y proyecciones.
     */
    @NotNull
    @JsonProperty("historial_mensual")
    List<ResumenMesDTO> historialMensual;

    /**
     * Contexto enriquecido del usuario.
     * Puede ser null si no está disponible; Python usa defaults seguros.
     */
    ContextoUsuarioDTO contexto;

    // ── Métodos de fábrica ────────────────────────────────────────────────────

    /**
     * Crea una solicitud para análisis automático post-transacción.
     * No requiere modulo_solicitado.
     */
    public static SolicitudIaDTO paraTransaccionReciente(
            String idUsuario,
            List<ResumenMesDTO> historial,
            ContextoUsuarioDTO contexto) {

        return SolicitudIaDTO.builder()
                .idUsuario(idUsuario)
                .tipoSolicitud(TipoSolicitudIa.TRANSACCION_RECIENTE)
                .historialMensual(historial)
                .contexto(contexto)
                .build();
    }

    /**
     * Crea una solicitud para un módulo específico solicitado desde el Dashboard.
     */
    public static SolicitudIaDTO paraConsultaModulo(
            String idUsuario,
            ModuloIa modulo,
            List<ResumenMesDTO> historial,
            ContextoUsuarioDTO contexto) {

        return SolicitudIaDTO.builder()
                .idUsuario(idUsuario)
                .tipoSolicitud(TipoSolicitudIa.CONSULTA_MODULO)
                .moduloSolicitado(modulo)
                .historialMensual(historial)
                .contexto(contexto)
                .build();
    }
}
