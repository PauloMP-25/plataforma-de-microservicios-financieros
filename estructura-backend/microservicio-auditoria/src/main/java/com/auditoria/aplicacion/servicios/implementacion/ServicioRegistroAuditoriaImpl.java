package com.auditoria.aplicacion.servicios.implementacion;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.auditoria.aplicacion.servicios.ServicioRegistroAuditoria;
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
 * @author Paulo Moron
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
        return new RespuestaAuditoriaDetalladoDTO(
                entidad.getId(),
                entidad.getUsuarioId(),
                UtilidadSeguridad.obtenerUsuarioEmail(),
                entidad.getAccion(),
                entidad.getModulo(),
                entidad.getIpOrigen(),
                entidad.getDetalles(),
                entidad.getFechaHora()
        );
    }

    private RegistroAuditoria convertirAEntidad(EventoAuditoriaDTO dto) {
        return RegistroAuditoria.builder()
                .usuarioId(dto.usuarioId() != null ? dto.usuarioId() : UtilidadSeguridad.obtenerUsuarioId())
                .accion("ACCESO_SISTEMA")
                .modulo(dto.modulo() != null ? dto.modulo() : "AUDITORIA")
                .ipOrigen(dto.ipOrigen())
                .detalles(dto.detalles())
                .build();
    }
}