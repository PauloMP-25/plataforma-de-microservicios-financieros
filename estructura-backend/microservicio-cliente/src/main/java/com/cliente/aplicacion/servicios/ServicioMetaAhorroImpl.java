package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudMetaAhorro;
import com.cliente.aplicacion.excepciones.MetaNoEncontradaException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.puertos.ServicioMetaAhorro;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.cliente.dominio.entidades.MetaAhorro;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.dominio.especificaciones.MetaAhorroSpecs;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión de metas de ahorro.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioMetaAhorroImpl implements ServicioMetaAhorro {

    private final MetaAhorroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea una nueva meta de ahorro para el usuario.
     */
    @Override
    @Transactional
    public RespuestaMetaAhorro crear(UUID usuarioIdToken, SolicitudMetaAhorro solicitud,
            String ipOrigen) {
        MetaAhorro meta = MetaAhorro.builder()
                .usuarioId(usuarioIdToken)
                .nombre(solicitud.nombre())
                .montoObjetivo(solicitud.montoObjetivo())
                .montoActual(solicitud.montoActual() != null
                        ? solicitud.montoActual()
                        : BigDecimal.ZERO)
                .fechaLimite(solicitud.fechaLimite())
                .completada(false)
                .build();

        MetaAhorro guardada = repositorio.save(meta);

        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                usuarioIdToken, "META_AHORRO_CREADA", "MS-CLIENTE", ipOrigen,
                String.format("Meta creada: '%s' — objetivo: S/ %.2f",
                        guardada.getNombre(), guardada.getMontoObjetivo())));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_CREADA"));
        return convertirADTO(guardada);
    }

    /**
     * Actualiza el progreso (monto actual) de una meta existente.
     */
    @Override
    @Transactional
    public RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken,
            BigDecimal nuevoMontoActual,
            String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        BigDecimal montoAnterior = meta.getMontoActual();
        meta.setMontoActual(nuevoMontoActual);

        boolean recienCompletada = meta.evaluarYMarcarCompletada();
        MetaAhorro actualizada = repositorio.save(meta);

        if (recienCompletada) {
            publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                    usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                    String.format("¡Meta '%s' alcanzada! S/ %.2f de S/ %.2f",
                            actualizada.getNombre(),
                            actualizada.getMontoActual(),
                            actualizada.getMontoObjetivo()),
                    montoAnterior + "", actualizada.getMontoActual() + ""));
            log.info("Meta completada: id={} nombre='{}'", metaId, actualizada.getNombre());
        } else {
            publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                    usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                    String.format("Progreso de ahorro actualizado para la meta '%s': S/ %.2f de S/ %.2f",
                            actualizada.getNombre(),
                            actualizada.getMontoActual(),
                            actualizada.getMontoObjetivo()),
                    montoAnterior + "", actualizada.getMontoActual() + ""));
        }

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_PROGRESO"));
        return convertirADTO(actualizada);
    }

    /**
     * Lista todas las metas del usuario (activas e inactivas).
     */
    @Override
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> listar(UUID usuarioIdToken) {
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioIdToken)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista solo las metas activas (no completadas), ordenadas por fecha límite.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> listarActivas(UUID usuarioIdToken) {
        return repositorio.findMetasActivasOrdenadas(usuarioIdToken)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Consulta una meta por id validando que pertenece al usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken) {
        return convertirADTO(obtenerYValidarPropiedad(metaId, usuarioIdToken));
    }

    /**
     * Elimina una meta de ahorro del usuario.
     */
    @Override
    @Transactional
    public void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        repositorio.delete(meta);
        log.info("Meta eliminada: id={} usuario={}", metaId, usuarioIdToken);
        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioIdToken, metaId, "MS-CLIENTE", "META_AHORRO",
                String.format("Meta eliminada: '%s'", meta.getNombre()), "ACTIVO", "DESACTIVADO"));
        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_ELIMINADA"));
        log.info("Meta eliminada y contexto sincronizado para usuario: {}", usuarioIdToken);
    }

    /**
     * Consulta interna del listado de metas sin validación de JWT (uso para Facade).
     */
    @Override
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> listarInterno(UUID usuarioId) {
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId)
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
        return meta;
    }

    private RespuestaMetaAhorro convertirADTO(MetaAhorro m) {
        return new RespuestaMetaAhorro(
                m.getId(), m.getNombre(),
                m.getMontoObjetivo(), m.getMontoActual(),
                m.calcularPorcentajeProgreso(),
                m.getFechaLimite(), m.getCompletada(),
                m.getFechaCreacion(), m.getFechaActualizacion());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> buscar(UUID usuarioIdToken, Boolean completada, LocalDate venceAntes, Double progresoBajo) {
        log.debug("Filtrando metas de ahorro dinámicamente para usuarioId={}", usuarioIdToken);

        Specification<MetaAhorro> specs = MetaAhorroSpecs.perteneceAUsuario(usuarioIdToken);

        if (completada != null) {
            specs = specs.and(MetaAhorroSpecs.estaCompletada(completada));
        }
        if (venceAntes != null) {
            specs = specs.and(MetaAhorroSpecs.venceAntesDe(venceAntes));
        }
        if (progresoBajo != null) {
            specs = specs.and(MetaAhorroSpecs.tieneProgresoBajo(progresoBajo));
        }

        return repositorio.findAll(specs).stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
}
