package com.auditoria.aplicacion.servicios.implementacion;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.auditoria.aplicacion.servicios.ServicioRegistroAuditoria;
import com.auditoria.dominio.entidades.RegistroAuditoria;
import com.auditoria.dominio.repositorios.RegistroAuditoriaRepository;
import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.utilidades.UtilidadSeguridad; // Importamos la utilidad de la librería

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.time.LocalDateTime;

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
    public EventoAccesoDTO registrarEvento(EventoAccesoDTO request) {
        log.info("[AUDITORIA] Registrando evento para usuario ID: {}", request.usuarioId());

        RegistroAuditoria entidad = convertirAEntidad(request);
        
        // Si el correo no viene en el evento, intentamos obtenerlo del token actual si existe
        if (entidad.getNombreUsuario() == null || entidad.getNombreUsuario().isBlank()) {
            entidad.setNombreUsuario(UtilidadSeguridad.obtenerEmailUsuario());
        }

        repositorioAuditoria.save(entidad);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaAuditoriaDetalladoDTO> listarRegistrosDetallados(String modulo, Pageable paginacion) {
        Page<RegistroAuditoria> entidades = repositorioAuditoria.buscarPorFiltros(modulo, paginacion);

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
                entidad.getFechaHora(),
                // Intentamos parsear el UUID si es posible, si no, generamos uno o enviamos null
                intentarParsearUUID(entidad.getNombreUsuario()), 
                entidad.getNombreUsuario(), // Aquí ya tenemos el email guardado
                "Usuario Sistema", // Este dato suele venir de ms-usuarios
                entidad.getAccion(),
                entidad.getModulo(),
                entidad.getIpOrigen(),
                entidad.getDetalles()
        );
    }

    private RegistroAuditoria convertirAEntidad(EventoAccesoDTO dto) {
        return RegistroAuditoria.builder()
                .fechaHora(dto.fecha() != null ? dto.fecha() : LocalDateTime.now())
                .nombreUsuario(dto.usuarioId() != null ? dto.usuarioId().toString() : UtilidadSeguridad.obtenerEmailUsuario())
                .accion("ACCESO_SISTEMA")
                .modulo(dto.modulo() != null ? dto.modulo() : "AUDITORIA")
                .ipOrigen(dto.ipOrigen())
                .detalles(dto.detalleError())
                .build();
    }

    private UUID intentarParsearUUID(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (Exception e) {
            return null; 
        }
    }
}