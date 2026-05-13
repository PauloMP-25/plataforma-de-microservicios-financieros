package com.cliente.aplicacion.servicios.implementacion;

import com.cliente.aplicacion.dtos.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.SolicitudDatosPersonales;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.excepciones.DniDuplicadoException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.servicios.ServicioContexto;
import com.cliente.aplicacion.servicios.ServicioDatosPersonales;
import com.cliente.dominio.entidades.DatosPersonales;
import com.cliente.dominio.repositorios.DatosPersonalesRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoAuditoriaDTO;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lógica de negocio para la gestión de datos personales del cliente.
 * 
 * @author Paulo Moron
 * @since 2026-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioDatosPersonalesImpl implements ServicioDatosPersonales {

    private final DatosPersonalesRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;
    private final ServicioContexto servicioContexto;

    /**
     * Crea un perfil inicial vacío de datos personales para un usuario.
     * Si ya existe, retorna el existente.
     *
     * @param usuarioId ID del usuario para el cual se crea el perfil.
     * @return {@link RespuestaDatosPersonales} con el perfil creado o existente.
     */
    @SuppressWarnings("null")
    @Override
    @Transactional
    public RespuestaDatosPersonales crearPerfil(UUID usuarioId) {
        return repositorio.findByUsuarioId(usuarioId)
                .map(this::convertirADTO)
                .orElseGet(() -> {
                    DatosPersonales nuevo = DatosPersonales.builder()
                            .usuarioId(usuarioId)
                            .datosCompletos(false)
                            .build();
                    log.info("Creando perfil inicial de datos personales para usuarioId={}", usuarioId);
                    DatosPersonales guardado = repositorio.save(nuevo);

                    publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                            usuarioId, "PERFIL_CREADO",
                            "MS-CLIENTE", "interno",
                            "Perfil inicial creado para usuarioId: " + usuarioId));

                    return convertirADTO(guardado);
                });
    }

    /**
     * Actualiza los datos personales del usuario. Tras el commit, publica un
     * evento que dispara la sincronización del contexto con Redis y RabbitMQ.
     *
     * @param usuarioIdRuta  ID del usuario en la ruta del endpoint.
     * @param usuarioIdToken ID del usuario extraído del JWT.
     * @param solicitud      DTO con los datos personales a actualizar.
     * @param ipOrigen       IP de origen para auditoría.
     * @return {@link RespuestaDatosPersonales} con los datos actualizados.
     * @throws ExcepcionAccesoDenegado               si el token no pertenece al usuario de la ruta.
     * @throws DatosPersonalesNoEncontradosException si el perfil no existe.
     * @throws DniDuplicadoException                 si el DNI ya pertenece a otro usuario.
     */
    @Override
    @Transactional
    public RespuestaDatosPersonales actualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudDatosPersonales solicitud, String ipOrigen) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);

        DatosPersonales datos = repositorio.findByUsuarioId(usuarioIdRuta)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdRuta));

        validarDniUnico(solicitud.getDni(), datos);
        aplicarCambios(datos, solicitud);
        datos.setDatosCompletos(datos.evaluarDatosCompletos());

        DatosPersonales actualizado = repositorio.save(datos);

        publicadorAuditoria.publicarEventoExitoso(EventoAuditoriaDTO.crear(
                UtilidadSeguridad.obtenerUsuarioId(),
                "PERFIL ACTUALIZADO", "MS-CLIENTE", ipOrigen,
                "Datos personales actualizados para usuarioId: " + usuarioIdRuta));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdRuta, "DATOS_PERSONALES"));
        return convertirADTO(actualizado);
    }

    /**
     * Consulta los datos personales de un usuario validando propiedad.
     *
     * @param usuarioIdRuta  ID del usuario en la ruta.
     * @param usuarioIdToken ID del usuario extraído del JWT.
     * @return {@link RespuestaDatosPersonales} con los datos del perfil.
     * @throws ExcepcionAccesoDenegado               si el token no pertenece al usuario de la ruta.
     * @throws DatosPersonalesNoEncontradosException si el perfil no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaDatosPersonales consultar(UUID usuarioIdRuta, UUID usuarioIdToken) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);
        return repositorio.findByUsuarioId(usuarioIdRuta)
                .map(this::convertirADTO)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdRuta));
    }

    /**
     * Convierte la entidad de dominio a un DTO de respuesta.
     *
     * @param e Entidad de dominio {@link DatosPersonales}.
     * @return {@link RespuestaDatosPersonales} convertido.
     */
    @Override
    @Transactional
    public void actualizarTelefono(UUID usuarioId, String telefono) {
        log.info("Actualizando teléfono verificado para usuario: {}", usuarioId);
        DatosPersonales datos = repositorio.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado para usuario: " + usuarioId));

        datos.setTelefono(telefono);
        repositorio.save(datos);

        // Refrescar contexto para IA
        servicioContexto.refrescarContextoRedis(usuarioId);
    }

    @Override
    public RespuestaDatosPersonales convertirADTO(DatosPersonales e) {
        return new RespuestaDatosPersonales(
                e.getDni(), e.getNombres(), e.getApellidos(),
                e.getGenero(), e.getEdad(), e.getTelefono(),
                e.getPais(), e.getCiudad(), e.getDatosCompletos(),
                e.getFechaCreacion(), e.getFechaActualizacion());
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    /**
     * Valida que el usuario del token tenga acceso al recurso del usuario de la ruta.
     *
     * @param usuarioIdRuta  ID en la ruta.
     * @param usuarioIdToken ID en el token JWT.
     * @throws ExcepcionAccesoDenegado si no coinciden.
     */
    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Acceso denegado: usuarioIdToken={} intentó acceder al perfil de usuarioIdRuta={}",
                    usuarioIdToken, usuarioIdRuta);
            throw new ExcepcionAccesoDenegado();
        }
    }

    /**
     * Valida que el nuevo DNI, si se proporciona, no esté ya en uso por otro usuario.
     *
     * @param nuevoDni DNI a validar.
     * @param actual   Entidad actual para permitir la actualización del propio DNI.
     * @throws DniDuplicadoException si el DNI ya existe en la BD y no es del usuario.
     */
    private void validarDniUnico(String nuevoDni, DatosPersonales actual) {
        if (nuevoDni != null
                && !nuevoDni.equals(actual.getDni())
                && repositorio.existsByDni(nuevoDni)) {
            throw new DniDuplicadoException(nuevoDni);
        }
    }

    /**
     * Aplica los cambios del DTO de solicitud a la entidad de forma parcial (solo no nulos).
     *
     * @param e Entidad {@link DatosPersonales} a modificar.
     * @param d DTO {@link SolicitudDatosPersonales} con los nuevos valores.
     */
    private void aplicarCambios(DatosPersonales e, SolicitudDatosPersonales d) {
        if (d.getDni() != null) {
            e.setDni(d.getDni());
        }
        if (d.getNombres() != null) {
            e.setNombres(d.getNombres());
        }
        if (d.getApellidos() != null) {
            e.setApellidos(d.getApellidos());
        }
        if (d.getGenero() != null) {
            e.setGenero(d.getGenero());
        }
        if (d.getEdad() != null) {
            e.setEdad(d.getEdad());
        }
        if (d.getTelefono() != null) {
            e.setTelefono(d.getTelefono());
        }
        if (d.getFotoPerfilUrl() != null) {
            e.setFotoPerfilUrl(d.getFotoPerfilUrl());
        }
        if (d.getPais() != null) {
            e.setPais(d.getPais());
        }
        if (d.getCiudad() != null) {
            e.setCiudad(d.getCiudad());
        }
    }
}
