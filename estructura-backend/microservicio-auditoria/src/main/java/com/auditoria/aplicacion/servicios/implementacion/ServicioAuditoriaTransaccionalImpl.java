package com.auditoria.aplicacion.servicios.implementacion;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaTransaccional;
import com.auditoria.dominio.entidades.AuditoriaTransaccional;
import com.auditoria.dominio.repositorios.AuditoriaTransaccionalRepository;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementación concreta del servicio de auditoría transaccional.
 * <p>
 * Realiza el mapeo entre las entidades de persistencia y los DTOs de la
 * librería común.
 * </p>
 * 
 * @author Paulo Moron
 */
@Service
@RequiredArgsConstructor
public class ServicioAuditoriaTransaccionalImpl implements ServicioAuditoriaTransaccional {

    private final AuditoriaTransaccionalRepository repositorio;

    @SuppressWarnings("null")
    @Override
    @Transactional
    public EventoTransaccionalDTO guardarEvento(EventoTransaccionalDTO dto) {
        AuditoriaTransaccional entidad = AuditoriaTransaccional.builder()
                .usuarioId(dto.usuarioId())
                .entidadId(dto.entidadId())
                .servicioOrigen(dto.servicioOrigen())
                .entidadAfectada(dto.entidadAfectada())
                .descripcion(dto.descripcion())
                .valorAnterior(dto.valorAnterior())
                .valorNuevo(dto.valorNuevo())
                .build();

        repositorio.save(entidad);
        return mapearADto(entidad);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoTransaccionalDTO> listarPorUsuario(UUID usuarioId, Pageable pageable) {
        return repositorio.findByUsuarioIdOrderByFechaDesc(usuarioId, pageable)
                .map(this::mapearADto);
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(readOnly = true)
    public Page<EventoTransaccionalDTO> buscarConFiltros(String servicio, LocalDateTime desde, LocalDateTime hasta,
            Pageable pageable) {

        // Composición dinámica de criterios usando Specification Pattern
        org.springframework.data.jpa.domain.Specification<AuditoriaTransaccional> spec = org.springframework.data.jpa.domain.Specification
                .where(
                        com.auditoria.dominio.especificaciones.AuditoriaSpecs.transaccionPorServicio(servicio))
                .and(
                        com.auditoria.dominio.especificaciones.AuditoriaSpecs.transaccionEntreFechas(desde, hasta));

        return repositorio.findAll(spec, pageable)
                .map(this::mapearADto);
    }

    /**
     * Mapea la entidad de base de datos al DTO estándar de la librería.
     */
    private EventoTransaccionalDTO mapearADto(AuditoriaTransaccional e) {
        return new EventoTransaccionalDTO(
                e.getUsuarioId(),
                e.getEntidadId(),
                e.getServicioOrigen(),
                e.getEntidadAfectada(),
                e.getDescripcion(),
                e.getValorAnterior(),
                e.getValorNuevo(),
                e.getFecha());
    }
}