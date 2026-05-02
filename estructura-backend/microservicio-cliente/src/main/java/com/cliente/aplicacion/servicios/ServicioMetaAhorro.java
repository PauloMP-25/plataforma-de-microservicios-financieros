package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.excepciones.*;
import com.cliente.dominio.entidades.MetaAhorro;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lógica de negocio para la gestión de metas de ahorro.
 *
 * Publica eventos a RabbitMQ: - META_CREADA → cuando se crea una nueva meta -
 * META_COMPLETADA → cuando montoActual >= montoObjetivo
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioMetaAhorro {

    private final MetaAhorroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;

    /**
     * Crea una nueva meta de ahorro para el usuario. Publica el evento
     * META_CREADA.
     *
     * @param usuarioIdToken
     * @param solicitud
     * @param ipOrigen
     * @return
     */
    @Transactional
    public RespuestaMetaAhorro crear(UUID usuarioIdToken, SolicitudMetaAhorro solicitud,
            String ipOrigen) {
        MetaAhorro meta = MetaAhorro.builder()
                .usuarioId(usuarioIdToken)
                .nombre(solicitud.getNombre())
                .montoObjetivo(solicitud.getMontoObjetivo())
                .montoActual(solicitud.getMontoActual() != null
                        ? solicitud.getMontoActual()
                        : BigDecimal.ZERO)
                .fechaLimite(solicitud.getFechaLimite())
                .completada(false)
                .build();

        MetaAhorro guardada = repositorio.save(meta);

        // Publicar evento META_CREADA (asíncrono)
        publicadorAuditoria.publicar(EventoAuditoria.de(
                usuarioIdToken.toString(), "META_CREADA", ipOrigen,
                String.format("Meta creada: '%s' — objetivo: S/ %.2f",
                        guardada.getNombre(), guardada.getMontoObjetivo())
        ));

        log.info("Meta creada: id={} usuario={} nombre='{}'",
                guardada.getId(), usuarioIdToken, guardada.getNombre());

        return convertirADTO(guardada);
    }

    /**
     * Actualiza el progreso (monto actual) de una meta existente. Si la meta se
     * completa en esta actualización, publica META_COMPLETADA.
     *
     * @param metaId
     * @param usuarioIdToken
     * @param nuevoMontoActual
     * @param ipOrigen
     * @return
     */
    @Transactional
    public RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken,
            BigDecimal nuevoMontoActual,
            String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        meta.setMontoActual(nuevoMontoActual);

        boolean recienCompletada = meta.evaluarYMarcarCompletada();
        MetaAhorro actualizada = repositorio.save(meta);

        if (recienCompletada) {
            publicadorAuditoria.publicar(EventoAuditoria.de(
                    usuarioIdToken.toString(), "META_COMPLETADA", ipOrigen,
                    String.format("¡Meta '%s' alcanzada! S/ %.2f de S/ %.2f",
                            actualizada.getNombre(),
                            actualizada.getMontoActual(),
                            actualizada.getMontoObjetivo())
            ));
            log.info("Meta completada: id={} nombre='{}'", metaId, actualizada.getNombre());
        }

        return convertirADTO(actualizada);
    }

    /**
     * Lista todas las metas del usuario (activas e inactivas).
     *
     * @param usuarioIdToken
     * @return
     */
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> listar(UUID usuarioIdToken) {
        return repositorio.findByUsuarioIdOrderByFechaCreacionDesc(usuarioIdToken)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Lista solo las metas activas (no completadas), ordenadas por fecha
     * límite.
     *
     * @param usuarioIdToken
     * @return
     */
    @Transactional(readOnly = true)
    public List<RespuestaMetaAhorro> listarActivas(UUID usuarioIdToken) {
        return repositorio.findMetasActivasOrdenadas(usuarioIdToken)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Consulta una meta por id validando que pertenece al usuario.
     *
     * @param metaId
     * @param usuarioIdToken
     * @return
     */
    @Transactional(readOnly = true)
    public RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken) {
        return convertirADTO(obtenerYValidarPropiedad(metaId, usuarioIdToken));
    }

    /**
     * Elimina una meta de ahorro del usuario.
     *
     * @param metaId
     * @param usuarioIdToken
     * @param ipOrigen
     */
    @Transactional
    public void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        repositorio.delete(meta);
        log.info("Meta eliminada: id={} usuario={}", metaId, usuarioIdToken);

        publicadorAuditoria.publicar(EventoAuditoria.de(
                usuarioIdToken.toString(), "META_ELIMINADA", ipOrigen,
                String.format("Meta eliminada: '%s'", meta.getNombre())
        ));
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    private MetaAhorro obtenerYValidarPropiedad(UUID metaId, UUID usuarioIdToken) {
        MetaAhorro meta = repositorio.findById(metaId)
                .orElseThrow(() -> new MetaNoEncontradaException(metaId));
        if (!meta.getUsuarioId().equals(usuarioIdToken)) {
            throw new AccesoDenegadoException();
        }
        return meta;
    }

    public RespuestaMetaAhorro convertirADTO(MetaAhorro m) {
        return new RespuestaMetaAhorro(
                m.getId(), m.getNombre(),
                m.getMontoObjetivo(), m.getMontoActual(),
                m.calcularPorcentajeProgreso(), // ← lógica de dominio
                m.getFechaLimite(), m.getCompletada(),
                m.getFechaCreacion(), m.getFechaActualizacion()
        );
    }
}
