package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudDatosPersonales;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.excepciones.DniDuplicadoException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.puertos.ServicioDatosPersonales;
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

    /**
     * Crea un perfil inicial vacío de datos personales para un usuario.
     * Si ya existe, retorna el existente.
     */
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
     */
    @Override
    @Transactional
    public RespuestaDatosPersonales actualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudDatosPersonales solicitud, String ipOrigen) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);

        DatosPersonales datos = repositorio.findByUsuarioId(usuarioIdRuta)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdRuta));

        validarDniUnico(solicitud.dni(), datos);
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
     * Actualiza solo el teléfono del usuario (uso interno para sincronización OTP).
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
        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioId, "DATOS_PERSONALES"));
    }

    /**
     * Consulta interna de datos personales sin validación de JWT (uso para Facade).
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaDatosPersonales consultarInterno(UUID usuarioId) {
        return repositorio.findByUsuarioId(usuarioId)
                .map(this::convertirADTO)
                .orElse(null);
    }

    private RespuestaDatosPersonales convertirADTO(DatosPersonales e) {
        return new RespuestaDatosPersonales(
                e.getDni(), e.getNombres(), e.getApellidos(),
                e.getGenero(), e.getEdad(), e.getTelefono(),
                e.getFotoPerfilUrl(),
                e.getPais(), e.getCiudad(), e.getDatosCompletos());
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Acceso denegado: usuarioIdToken={} intentó acceder al perfil de usuarioIdRuta={}",
                    usuarioIdToken, usuarioIdRuta);
            throw new ExcepcionAccesoDenegado();
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
        if (d.dni() != null) {
            e.setDni(d.dni());
        }
        if (d.nombres() != null) {
            e.setNombres(d.nombres());
        }
        if (d.apellidos() != null) {
            e.setApellidos(d.apellidos());
        }
        if (d.genero() != null) {
            e.setGenero(d.genero());
        }
        if (d.edad() != null) {
            e.setEdad(d.edad());
        }
        if (d.telefono() != null) {
            e.setTelefono(d.telefono());
        }
        if (d.fotoPerfilUrl() != null) {
            e.setFotoPerfilUrl(d.fotoPerfilUrl());
        }
        if (d.pais() != null) {
            e.setPais(d.pais());
        }
        if (d.ciudad() != null) {
            e.setCiudad(d.ciudad());
        }
    }
}
