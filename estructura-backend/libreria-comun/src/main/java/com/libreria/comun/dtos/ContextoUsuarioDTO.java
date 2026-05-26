package com.libreria.comun.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Agregado de información de contexto del usuario para servicios analíticos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextoUsuarioDTO {
    private UUID idUsuario;
    private PerfilFinancieroDTO perfilFinanciero;
    private List<MetaAhorroDTO> metas;
    private LimiteGlobalDTO limiteGlobal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerfilFinancieroDTO {
        private String ocupacion;
        private BigDecimal ingresoMensual;
        private String nivelRiesgo;
        private String tonoIA;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetaAhorroDTO {
        private String nombre;
        private BigDecimal montoObjetivo;
        private BigDecimal montoActual;
        private String fechaMeta;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimiteGlobalDTO {
        private BigDecimal montoLimite;
        private Integer porcentajeAlerta;
        private BigDecimal montoGastadoActual;
    }
}
