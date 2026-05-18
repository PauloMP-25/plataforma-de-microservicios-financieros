package com.cliente.aplicacion.mappers;

import com.libreria.comun.dtos.ContextoUsuarioDTO;
import com.cliente.aplicacion.dtos.respuestas.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.respuestas.RespuestaLimiteGasto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper dedicado (ContextoMapper / ContextoEnsamblador) para realizar las conversiones
 * de los DTOs internos del microservicio de cliente hacia los DTOs globales de libreria-comun.
 * 
 * Separa la responsabilidad de mapeo y simplifica la legibilidad y mantenimiento del ServicioContexto.
 */
@Component
public class ContextoMapper {

    public ContextoUsuarioDTO.PerfilFinancieroDTO toPerfilDTO(RespuestaPerfilFinanciero p) {
        if (p == null) {
            return null;
        }
        return ContextoUsuarioDTO.PerfilFinancieroDTO.builder()
                .ocupacion(p.ocupacion())
                .ingresoMensual(p.ingresoMensual())
                .tonoIA(p.tonoIA())
                .nivelRiesgo(p.estiloVida())
                .build();
    }

    public ContextoUsuarioDTO.MetaAhorroDTO toMetaDTO(RespuestaMetaAhorro m) {
        if (m == null) {
            return null;
        }
        return ContextoUsuarioDTO.MetaAhorroDTO.builder()
                .nombre(m.nombre())
                .montoObjetivo(m.montoObjetivo())
                .montoActual(m.montoActual())
                .fechaMeta(m.fechaLimite() != null ? m.fechaLimite().toString() : null)
                .build();
    }

    public List<ContextoUsuarioDTO.MetaAhorroDTO> toMetaDTOList(List<RespuestaMetaAhorro> metas) {
        if (metas == null) {
            return Collections.emptyList();
        }
        return metas.stream()
                .map(this::toMetaDTO)
                .collect(Collectors.toList());
    }

    public ContextoUsuarioDTO.LimiteGlobalDTO toLimiteDTO(RespuestaLimiteGasto l) {
        if (l == null) {
            return null;
        }
        return ContextoUsuarioDTO.LimiteGlobalDTO.builder()
                .montoLimite(l.montoLimite())
                .porcentajeAlerta(l.porcentajeAlerta())
                .build();
    }

    public ContextoUsuarioDTO ensamblarContextoCompleto(UUID usuarioId,
                                                        RespuestaPerfilFinanciero perfil,
                                                        List<RespuestaMetaAhorro> metas,
                                                        RespuestaLimiteGasto limite) {
        return ContextoUsuarioDTO.builder()
                .idUsuario(usuarioId)
                .perfilFinanciero(toPerfilDTO(perfil))
                .metas(toMetaDTOList(metas))
                .limiteGlobal(toLimiteDTO(limite))
                .build();
    }
}
