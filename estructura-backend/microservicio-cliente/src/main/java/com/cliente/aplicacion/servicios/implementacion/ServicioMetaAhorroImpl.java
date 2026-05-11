package com.cliente.aplicacion.servicios.implementacion;

import com.cliente.aplicacion.servicios.ServicioMetaAhorro;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.cliente.aplicacion.dtos.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.SolicitudMetaAhorro;
import com.cliente.aplicacion.excepciones.MetaNoEncontradaException;
import com.cliente.dominio.entidades.MetaAhorro;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.dtos.EventoTransaccionalDTO;

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
 * <p>
 * Publica eventos a RabbitMQ: META_CREADA, META_COMPLETADA.
 * Utiliza el patrón Transactional Event Publisher para garantizar
 * que la sincronización con Redis/RabbitMQ ocurra solo tras COMMIT.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioMetaAhorroImpl implements ServicioMetaAhorro {

    private final MetaAhorroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea una nueva meta de ahorro para el usuario. Publica el evento
     * META_AHORRO_CREADA.
     *
     * @param usuarioIdToken ID del usuario
     * @param solicitud      DTO con datos de la meta
     * @param ipOrigen       IP del cliente
     * @return RespuestaMetaAhorro con la meta creada
     */
    @SuppressWarnings("null")
    @Override
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
        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                usuarioIdToken, "META_AHORRO_CREADA", "MS-CLIENTE", ipOrigen,
                String.format("Meta creada: '%s' — objetivo: S/ %.2f",
                        guardada.getNombre(), guardada.getMontoObjetivo())));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_CREADA"));
        return convertirADTO(guardada);
    }

    /**
     * Actualiza el progreso (monto actual) de una meta existente. Si la meta se
     * completa en esta actualización, publica META_COMPLETADA.
     *
     * @param metaId           ID de la meta
     * @param usuarioIdToken   ID del usuario
     * @param nuevoMontoActual Nuevo monto guardado
     * @param ipOrigen         IP del cliente
     * @return RespuestaMetaAhorro con la meta actualizada
     */
    @Override
    @Transactional
    public RespuestaMetaAhorro actualizarProgreso(UUID metaId, UUID usuarioIdToken,
            BigDecimal nuevoMontoActual,
            String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        meta.setMontoActual(nuevoMontoActual);

        boolean recienCompletada = meta.evaluarYMarcarCompletada();
        MetaAhorro actualizada = repositorio.save(meta);

        if (recienCompletada) {
            publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                    usuarioIdToken, metaId, "MS-FINANCIERO", "META_AHORRO",
                    String.format("¡Meta '%s' alcanzada! S/ %.2f de S/ %.2f",
                            actualizada.getNombre(),
                            actualizada.getMontoActual(),
                            actualizada.getMontoObjetivo()),
                    meta.getMontoActual() + "", actualizada.getMontoActual() + ""));
            log.info("Meta completada: id={} nombre='{}'", metaId, actualizada.getNombre());
        }

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_PROGRESO"));
        return convertirADTO(actualizada);
    }

    /**
     * Lista todas las metas del usuario (activas e inactivas).
     *
     * @param usuarioIdToken ID del usuario
     * @return Lista de RespuestaMetaAhorro con todas las metas
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
     * Lista solo las metas activas (no completadas), ordenadas por fecha
     * límite.
     *
     * @param usuarioIdToken ID del usuario
     * @return Lista de RespuestaMetaAhorro con las metas activas
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
     *
     * @param metaId         ID de la meta
     * @param usuarioIdToken ID del usuario
     * @return RespuestaMetaAhorro con la meta consultada
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaMetaAhorro consultar(UUID metaId, UUID usuarioIdToken) {
        return convertirADTO(obtenerYValidarPropiedad(metaId, usuarioIdToken));
    }

    /**
     * Elimina una meta de ahorro del usuario.
     *
     * @param metaId         ID de la meta
     * @param usuarioIdToken ID del usuario
     * @param ipOrigen       IP del cliente
     */
    @SuppressWarnings("null")
    @Override
    @Transactional
    public void eliminar(UUID metaId, UUID usuarioIdToken, String ipOrigen) {
        MetaAhorro meta = obtenerYValidarPropiedad(metaId, usuarioIdToken);
        repositorio.delete(meta);
        log.info("Meta eliminada: id={} usuario={}", metaId, usuarioIdToken);
        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioIdToken, metaId, "MS-CLIENTE", "META AHORRO",
                String.format("Meta eliminada: '%s'", meta.getNombre()), "ACTIVO", "DESACTIVADO"));
        log.info("Meta eliminada y contexto sincronizado para usuario: {}", usuarioIdToken);
        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdToken, "META_AHORRO_ELIMINADA"));
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    /**
     * Obtiene una meta de ahorro por su ID y valida que pertenezca al usuario del token.
     *
     * @param metaId         ID de la meta a buscar.
     * @param usuarioIdToken ID del usuario extraído del JWT.
     * @return {@link MetaAhorro} validada.
     * @throws MetaNoEncontradaException si la meta no existe.
     * @throws ExcepcionAccesoDenegado   si la meta pertenece a otro usuario.
     */
    @SuppressWarnings("null")
    private MetaAhorro obtenerYValidarPropiedad(UUID metaId, UUID usuarioIdToken) {
        MetaAhorro meta = repositorio.findById(metaId)
                .orElseThrow(() -> new MetaNoEncontradaException(metaId));
        if (!meta.getUsuarioId().equals(usuarioIdToken)) {
            throw new ExcepcionAccesoDenegado();
        }
        return meta;
    }

    /**
     * Convierte una entidad {@link MetaAhorro} a su DTO de respuesta.
     * Incluye lógica de dominio para calcular el porcentaje de progreso.
     *
     * @param m Entidad de dominio a convertir.
     * @return {@link RespuestaMetaAhorro} con los datos mapeados.
     */
    public RespuestaMetaAhorro convertirADTO(MetaAhorro m) {
        return new RespuestaMetaAhorro(
                m.getId(), m.getNombre(),
                m.getMontoObjetivo(), m.getMontoActual(),
                m.calcularPorcentajeProgreso(), // ← lógica de dominio
                m.getFechaLimite(), m.getCompletada(),
                m.getFechaCreacion(), m.getFechaActualizacion());
    }
}
