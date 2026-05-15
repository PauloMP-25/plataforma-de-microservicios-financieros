package com.libreria.comun.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.libreria.comun.enums.ModuloIa;
import com.libreria.comun.enums.TipoSolicitudIa;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * Payload completo enviado al microservicio de IA (Python/Gemini).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolicitudIaDTO {

    @NotNull(message = "El id_usuario es obligatorio")
    @JsonProperty("id_usuario")
    private UUID usuarioId;

    @NotNull(message = "El tipo_solicitud es obligatorio")
    @JsonProperty("tipo_solicitud")
    private TipoSolicitudIa tipoSolicitud;

    @JsonProperty("modulo_solicitado")
    private ModuloIa moduloSolicitado;

    @JsonProperty("historial_mensual")
    private List<ResumenMesDTO> historialMensual;

    @JsonProperty("contexto")
    private ContextoUsuarioDTO contexto;

    // Métodos de compatibilidad con código existente que usa getters antiguos
    public UUID getIdUsuario() {
        return usuarioId;
    }
}
