package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.auditoria.aplicacion.puertos.ServicioRegistroAuditoria;
import com.auditoria.dominio.entidades.RegistroAuditoria;
import com.auditoria.dominio.repositorios.RegistroAuditoriaRepository;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de registro de auditoría general enriquecida.
 * <p>
 * Utiliza UtilidadSeguridad para capturar datos del contexto de seguridad
 * y mapear los resultados a DTOs detallados.
 * </p>
 * 
 * @version 1.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioRegistroAuditoriaImpl implements ServicioRegistroAuditoria {

    private final RegistroAuditoriaRepository repositorioAuditoria;

    @Override
    @Transactional
    public EventoAuditoriaDTO registrarEvento(EventoAuditoriaDTO request) {
        log.info("[AUDITORIA] Registrando evento para usuario ID: {}", request.usuarioId());

        RegistroAuditoria entidad = convertirAEntidad(request);
        
        // Si el correo no viene en el evento, intentamos obtenerlo del token actual si existe
        if (entidad.getUsuarioId() == null) {
            entidad.setUsuarioId(UtilidadSeguridad.obtenerUsuarioId());
        }

        repositorioAuditoria.save(entidad);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaAuditoriaDetalladoDTO> listarRegistrosDetallados(String modulo, Pageable paginacion) {
        // Usamos Specification Pattern para filtrado dinámico desacoplado
        @SuppressWarnings("null")
        Page<RegistroAuditoria> entidades = repositorioAuditoria.findAll(
                com.auditoria.dominio.especificaciones.AuditoriaSpecs.registroPorModulo(modulo),
                paginacion);

        return entidades.map(this::convertirARespuestaDetallada);
    }

    /**
     * Mapea la entidad a un DTO de respuesta detallada.
     * <p>
     * Nota: En un entorno distribuido, si el email no se guardó en la BD de auditoría,
     * se obtendría aquí mediante Feign del ms-usuarios.
     * </p>
     */
    private RespuestaAuditoriaDetalladoDTO convertirARespuestaDetallada(RegistroAuditoria entidad) {
        String emailUsuario = "N/A";
        try {
            // Evitamos NPE y fugas de información comprobando si el usuario auditado es el mismo que está autenticado
            java.util.UUID authUsuarioId = UtilidadSeguridad.obtenerUsuarioId();
            if (authUsuarioId != null && authUsuarioId.equals(entidad.getUsuarioId())) {
                emailUsuario = UtilidadSeguridad.obtenerUsuarioEmail();
            }
        } catch (Exception e) {
            log.debug("[AUDITORIA] No se pudo obtener el email del contexto de seguridad: {}", e.getMessage());
        }

        return new RespuestaAuditoriaDetalladoDTO(
                entidad.getId(),
                entidad.getUsuarioId(),
                emailUsuario,
                entidad.getAccion(),
                entidad.getModulo(),
                entidad.getIpOrigen(),
                entidad.getCorrelationId(),
                entidad.getDetalles(),
                entidad.getFechaHora()
        );
    }

    private RegistroAuditoria convertirAEntidad(EventoAuditoriaDTO dto) {
        // Intentamos obtener el correlationId de MDC (Mapped Diagnostic Context)
        String correlationId = org.slf4j.MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = org.slf4j.MDC.get("traceId");
        }

        return RegistroAuditoria.builder()
                .usuarioId(dto.usuarioId() != null ? dto.usuarioId() : UtilidadSeguridad.obtenerUsuarioId())
                .accion(dto.accion() != null ? dto.accion() : "ACCESO_SISTEMA")
                .modulo(dto.modulo() != null ? dto.modulo() : "AUDITORIA")
                .ipOrigen(dto.ipOrigen())
                .correlationId(correlationId)
                .detalles(dto.detalles())
                .build();
    }
}
