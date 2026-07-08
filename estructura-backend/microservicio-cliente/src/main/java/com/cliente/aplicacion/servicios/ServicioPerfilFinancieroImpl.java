package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.respuestas.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudPerfilFinanciero;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.puertos.ServicioPerfilFinanciero;
import com.libreria.comun.excepciones.ExcepcionAccesoDenegado;
import com.cliente.dominio.entidades.PerfilFinanciero;
import com.cliente.dominio.repositorios.PerfilFinancieroRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import com.libreria.comun.dtos.EventoTransaccionalDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lógica de negocio para el perfil financiero del cliente.
 * 
 * @version 1.1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioPerfilFinancieroImpl implements ServicioPerfilFinanciero {

    private final PerfilFinancieroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea o actualiza el perfil financiero (upsert).
     */
    @Override
    @Transactional
    public RespuestaPerfilFinanciero guardarOActualizar(UUID usuarioIdRuta, UUID usuarioIdToken,
            SolicitudPerfilFinanciero solicitud,
            String ipOrigen) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);

        PerfilFinanciero perfil = repositorio.findByUsuarioId(usuarioIdRuta)
                .orElseGet(() -> {
                    log.info("Creando perfil financiero para usuarioId={}", usuarioIdRuta);
                    return PerfilFinanciero.builder().usuarioId(usuarioIdRuta).build();
                });

        aplicarCambios(perfil, solicitud);
        PerfilFinanciero guardado = repositorio.save(perfil);

        publicadorAuditoria.publicarTransaccionExitosa(EventoTransaccionalDTO.crear(
                usuarioIdToken, perfil.getId(), "MS-CLIENTE", "PERFIL FINANCIERO",
                "Perfil financiero actualizado", "BASE", "ACTUALIZADO"));

        eventPublisher.publishEvent(new EventoContextoActualizado(usuarioIdRuta, "PERFIL_FINANCIERO"));
        return convertirADTO(guardado);
    }

    /**
     * Consulta el perfil financiero del usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaPerfilFinanciero consultar(UUID usuarioIdRuta, UUID usuarioIdToken) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);
        return repositorio.findByUsuarioId(usuarioIdRuta)
                .map(this::convertirADTO)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdRuta));
    }

    /**
     * Consulta interna del perfil financiero sin validación de JWT (uso para Facade).
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaPerfilFinanciero consultarInterno(UUID usuarioId) {
        return repositorio.findByUsuarioId(usuarioId)
                .map(this::convertirADTO)
                .orElse(null);
    }

    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (usuarioIdRuta == null || usuarioIdToken == null || !usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Acceso denegado al perfil financiero: token={} ruta={}", usuarioIdToken, usuarioIdRuta);
            throw new ExcepcionAccesoDenegado();
        }
    }

    private void aplicarCambios(PerfilFinanciero e, SolicitudPerfilFinanciero d) {
        if (d.ocupacion() != null) {
            e.setOcupacion(d.ocupacion());
        }
        if (d.ingresoMensual() != null) {
            e.setIngresoMensual(d.ingresoMensual());
        }
        if (d.estiloVida() != null) {
            e.setEstiloVida(d.estiloVida());
        }
        if (d.tonoIA() != null) {
            e.setTonoIA(d.tonoIA());
        }
    }

    private RespuestaPerfilFinanciero convertirADTO(PerfilFinanciero e) {
        return new RespuestaPerfilFinanciero(
                e.getOcupacion(),
                e.getIngresoMensual(), e.getEstiloVida(), e.getTonoIA());
    }
}
