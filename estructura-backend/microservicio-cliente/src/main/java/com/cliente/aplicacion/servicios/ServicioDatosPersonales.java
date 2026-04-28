package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.excepciones.*;
import com.cliente.dominio.entidades.DatosPersonales;
import com.cliente.dominio.repositorios.DatosPersonalesRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
/**
 * Lógica de negocio para la gestión de datos personales del cliente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioDatosPersonales {

    private final DatosPersonalesRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;

    /**
     * Crea un registro vacío de datos personales vinculado al usuarioId.
     * Idempotente: si ya existe, lo devuelve sin crear uno nuevo.
     *
     * @param usuarioId
     * @return
     */
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

                    publicadorAuditoria.publicar(EventoAuditoria.de(
                            "sistema", "PERFIL_CREADO", "interno",
                            "Perfil inicial creado para usuarioId: " + usuarioId
                    ));

                    return convertirADTO(guardado);
                });
    }

    /**
     * Actualiza los datos personales del cliente con validación de propiedad.
     *
     * @param usuarioIdRuta
     * @param usuarioIdToken
     * @param solicitud
     * @param ipOrigen
     * @return
     */
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

        publicadorAuditoria.publicar(EventoAuditoria.de(
                actualizado.obtenerNombreCompleto(),
                "PERFIL_ACTUALIZADO", ipOrigen,
                "Datos personales actualizados para usuarioId: " + usuarioIdRuta
        ));

        return convertirADTO(actualizado);
    }

    /**
     * Consulta los datos personales de un usuario, validando propiedad.
     *
     * @param usuarioIdRuta
     * @param usuarioIdToken
     * @return
     */
    @Transactional(readOnly = true)
    public RespuestaDatosPersonales consultar(UUID usuarioIdRuta, UUID usuarioIdToken) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);
        return repositorio.findByUsuarioId(usuarioIdRuta)
                .map(this::convertirADTO)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdRuta));
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Acceso denegado: usuarioIdToken={} intentó acceder al perfil de usuarioIdRuta={}",
                    usuarioIdToken, usuarioIdRuta);
            throw new AccesoDenegadoException();
        }
    }

    private void validarDniUnico(String nuevoDni, DatosPersonales actual) {
        if (nuevoDni != null
                && !nuevoDni.equals(actual.getDni())
                && repositorio.existsByDni(nuevoDni)) {
            throw new DniDuplicadoException(nuevoDni);
        }
    }

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

    public RespuestaDatosPersonales convertirADTO(DatosPersonales e) {
        return new RespuestaDatosPersonales(
                e.getId(), e.getUsuarioId(), e.getDni(), e.getNombres(), e.getApellidos(),
                e.getGenero(), e.getEdad(), e.getTelefono(), e.getFotoPerfilUrl(),
                e.getPais(), e.getCiudad(), e.getDatosCompletos(),
                e.getFechaCreacion(), e.getFechaActualizacion()
        );
    }
}
