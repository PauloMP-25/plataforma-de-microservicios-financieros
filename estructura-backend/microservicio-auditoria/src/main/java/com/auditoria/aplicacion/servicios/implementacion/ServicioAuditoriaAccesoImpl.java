package com.auditoria.aplicacion.servicios.implementacion;

import com.auditoria.aplicacion.servicios.ServicioAuditoriaAcceso;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import com.auditoria.dominio.entidades.AuditoriaAcceso;
import com.libreria.comun.enums.EstadoEvento;
import com.auditoria.dominio.repositorios.AuditoriaAccesoRepository;
import com.libreria.comun.dtos.EventoAccesoDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementación concreta del servicio de auditoría de acceso.
 * <p>
 * Gestiona el ciclo de vida de los registros de acceso y colabora con el
 * servicio de seguridad para la detección de anomalías.
 * </p>
 * 
 * @author Paulo Moron
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioAuditoriaAccesoImpl implements ServicioAuditoriaAcceso {

    private final AuditoriaAccesoRepository repositorio;
    private final ServicioSeguridadAuditoria servicioSeguridad;

    @Override
    @Transactional
    public EventoAccesoDTO registrarAcceso(EventoAccesoDTO dto, EstadoEvento estado) {
        log.info("[AUDITORIA-ACCESO] Registrando: {} para IP: {}", dto.estado(), dto.ipOrigen());

        AuditoriaAcceso entidad = AuditoriaAcceso.builder()
                .usuarioId(dto.usuarioId())
                .ipOrigen(dto.ipOrigen())
                .navegador(dto.navegador())
                .estado(dto.estado())
                .detalleError(dto.detalleError())
                .fecha(dto.fecha() != null ? dto.fecha() : LocalDateTime.now())
                .build();
        AuditoriaAcceso guardado = repositorio.save(Objects.requireNonNull(entidad));

        // Si es un fallo, delegamos la lógica de bloqueo al servicio de seguridad
        if (dto.estado() == EstadoEvento.FALLO) {
            servicioSeguridad.verificarIntentoFallido(dto.ipOrigen());
        }
        return convertirADTO(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoAccesoDTO> listarTodo(Pageable paginacion) {
        return repositorio.findAll(Objects.requireNonNull(paginacion)).map(this::convertirADTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoAccesoDTO> listarPorUsuario(UUID usuarioId, Pageable paginacion) {
        return repositorio.findByUsuarioIdOrderByFechaDesc(usuarioId, paginacion)
                .map(this::convertirADTO);
    }

    @Override
    @Transactional
    public int limpiarRegistrosAntiguos(int diasAntiguedad) {
        LocalDateTime umbral = LocalDateTime.now().minusDays(diasAntiguedad);
        int eliminados = repositorio.eliminarRegistrosAnterioresA(umbral);
        if (eliminados > 0) {
            log.info("[MANTENIMIENTO] Se purgaron {} registros de acceso antiguos.", eliminados);
        }
        return eliminados;
    }

    /**
     * Mapea la entidad de persistencia al DTO de auditoría de acceso.
     */
    private EventoAccesoDTO convertirADTO(AuditoriaAcceso e) {
        return new EventoAccesoDTO(
                e.getUsuarioId(), e.getIpOrigen(),
                e.getNavegador(), e.getEstado(), e.getDetalleError(), e.getFecha());
    }
}
