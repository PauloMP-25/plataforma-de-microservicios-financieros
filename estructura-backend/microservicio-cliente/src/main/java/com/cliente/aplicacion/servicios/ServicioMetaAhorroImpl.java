package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudMetaAhorro;
import com.cliente.aplicacion.excepciones.MetaNoEncontradaException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.puertos.ServicioMetaAhorro;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.cliente.dominio.entidades.MetaAhorro;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;
import com.libreria.comun.respuesta.Paginacion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión de metas de ahorro.
 * 
 * @author Paulo Moron
 * @version 1.2.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioMetaAhorroImpl implements ServicioMetaAhorro {

    private final MetaAhorroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RespuestaMetaAhorro crear(UUID usuarioIdToken, SolicitudMetaAhorro solicitud, String ipOrigen) {
        MetaAhorro meta = MetaAhorro.builder()
                .usuarioId(usuarioIdToken)
                .nombre(solicitud.nombre())
                .proposito(solicitud.proposito())
                .montoObjetivo(solicitud.montoObjetivo())
                .montoActual(solicitud.montoActual() != null ? solicitud.montoActual() : BigDecimal.ZERO)
                .fechaLimite(solicitud.fechaLimite())
                .completada(false)
                .activa(true)
                .build();

        MetaAhorro guardada = repositorio.save(meta);

        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                usuarioIdToken, "META_AHORRO_CREADA", "MS-CLIENTE", ipOrigen,
                String.format("Meta creada: '%s' — objetivo: S/ %.2f", guardada.getNombre(), guardada.getMontoObjetivo())));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_CREADA"));
        return convertirADTO(guardada);
    }

    @Override
    @Transactional
    public RespuestaMetaAhorro actualizarMeta(UUID metaId, UUID usuarioIdToken, SolicitudMetaAhorro solicitud, String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        
        // El nombre y propósito no se actualizan según las reglas de negocio
        meta.setMontoObjetivo(solicitud.montoObjetivo());
        meta.setFechaLimite(solicitud.fechaLimite());
        
        if (solicitud.montoActual() != null) {
            meta.setMontoActual(solicitud.montoActual());
        }

        meta.evaluarYMarcarCompletada();
        MetaAhorro actualizada = repositorio.save(meta);

        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                String.format("Meta editada: '%s'", actualizada.getNombre()),
                "N/A", "N/A"));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_EDITADA"));
        return convertirADTO(actualizada);
    }

    @Override
    @Transactional
    public RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken, BigDecimal nuevoMontoActual, String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        BigDecimal montoAnterior = meta.getMontoActual();
        meta.setMontoActual(nuevoMontoActual);

        boolean recienCompletada = meta.evaluarYMarcarCompletada();
        MetaAhorro actualizada = repositorio.save(meta);

        if (recienCompletada) {
            publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                    usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                    String.format("¡Meta '%s' alcanzada! S/ %.2f de S/ %.2f", actualizada.getNombre(), actualizada.getMontoActual(), actualizada.getMontoObjetivo()),
                    montoAnterior + "", actualizada.getMontoActual() + ""));
        } else {
            publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                    usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                    String.format("Progreso de ahorro actualizado para '%s'", actualizada.getNombre()),
                    montoAnterior + "", actualizada.getMontoActual() + ""));
        }

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_PROGRESO"));
        return convertirADTO(actualizada);
    }

    @Override
    @Transactional(readOnly = true)
    public Paginacion<RespuestaMetaAhorro> listar(UUID usuarioIdToken, Pageable pageable) {
        Page<RespuestaMetaAhorro> page = repositorio.findByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(usuarioIdToken, pageable)
                .map(this::convertirADTO);
        return Paginacion.desde(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Paginacion<RespuestaMetaAhorro> listarActivas(UUID usuarioIdToken, Pageable pageable) {
        Page<RespuestaMetaAhorro> page = repositorio.findMetasActivasOrdenadas(usuarioIdToken, pageable)
                .map(this::convertirADTO);
        return Paginacion.desde(page);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken) {
        return convertirADTO(obtenerYValidarPropiedad(metaId, usuarioIdToken));
    }

    @Override
    @Transactional
    public void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        // Soft delete
        meta.setActiva(false);
        repositorio.save(meta);
        
        log.info("Meta desactivada: id={} usuario={}", metaId, usuarioIdToken);
        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                String.format("Meta desactivada: '%s'", meta.getNombre()), "ACTIVO", "DESACTIVADO"));
        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_ELIMINADA"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> listarInterno(UUID usuarioId) {
        return repositorio.findByUsuarioIdAndActivaTrueOrderByFechaCreacionDesc(usuarioId)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private MetaAhorro obtenerYValidarPropiedad(UUID metaId, UUID usuarioIdToken) {
        if (metaId == null || usuarioIdToken == null) {
            throw new IllegalArgumentException("Los identificadores de meta y token no pueden ser nulos");
        }
        MetaAhorro meta = repositorio.findById(metaId)
                .orElseThrow(() -> new MetaNoEncontradaException(metaId));
        if (meta.getUsuarioId() == null || !meta.getUsuarioId().equals(usuarioIdToken)) {
            throw new ExcepcionAccesoDenegado();
        }
        if (!meta.getActiva()) {
            throw new MetaNoEncontradaException(metaId); // Si está desactivada, es como si no existiera
        }
        return meta;
    }

    private RespuestaMetaAhorro convertirADTO(MetaAhorro m) {
        return new RespuestaMetaAhorro(
                m.getId(), m.getNombre(),
                m.getMontoObjetivo(), m.getMontoActual(),
                m.calcularPorcentajeProgreso(),
                m.getFechaLimite(), m.getCompletada(),
                m.getProposito(),
                m.getFechaCreacion(), m.getFechaActualizacion());
    }
}
