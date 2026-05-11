package com.cliente.aplicacion.servicios.implementacion;

import com.cliente.aplicacion.dtos.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.SolicitudPerfilFinanciero;
import com.cliente.aplicacion.excepciones.DatosPersonalesNoEncontradosException;
import com.cliente.aplicacion.eventos.EventoContextoActualizado;
import com.cliente.aplicacion.servicios.ServicioPerfilFinanciero;
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
 * <p>
 * Utiliza el patrón Transactional Event Publisher: en lugar de
 * disparar la sincronización directamente, publica un
 * {@link EventoContextoActualizado} que es capturado por
 * {@code EscuchaSincronizacionIA} en fase AFTER_COMMIT.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioPerfilFinancieroImpl implements ServicioPerfilFinanciero {

    private final PerfilFinancieroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Crea o actualiza el perfil financiero (upsert). Si no existe, lo crea. Si
     * existe, lo actualiza. Tras el commit, publica un evento que dispara la
     * sincronización con Redis y RabbitMQ.
     * 
     * @param usuarioIdRuta  ID del usuario en la ruta, usado para buscar el perfil.
     * @param usuarioIdToken ID del usuario extraído del JWT para validar propiedad.
     * @param solicitud      DTO con los campos del perfil a crear o actualizar.
     * @param ipOrigen       Dirección IP de origen para trazabilidad en auditoría.
     * @return {@link RespuestaPerfilFinanciero} con los datos del perfil guardado.
     * @throws ExcepcionAccesoDenegado si el usuario del token no coincide con la ruta.
     */
    @SuppressWarnings("null")
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
     * 
     * @param usuarioIdRuta  ID del usuario en la ruta, usado para buscar el perfil.
     * @param usuarioIdToken ID del usuario extraído del JWT para validar propiedad.
     * @return {@link RespuestaPerfilFinanciero} con el perfil consultado.
     * @throws ExcepcionAccesoDenegado si el usuario del token no coincide con la ruta.
     * @throws DatosPersonalesNoEncontradosException si no existe perfil para el usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public RespuestaPerfilFinanciero consultar(UUID usuarioIdRuta, UUID usuarioIdToken) {
        validarPropiedad(usuarioIdRuta, usuarioIdToken);
        return repositorio.findByUsuarioId(usuarioIdRuta)
                .map(this::convertirADTO)
                .orElseThrow(() -> new DatosPersonalesNoEncontradosException(usuarioIdRuta));
    }

    // =========================================================================
    // Soporte interno
    // =========================================================================
    /**
     * Valida que el usuario del token sea el propietario del recurso.
     *
     * @param usuarioIdRuta  ID del usuario en la ruta del endpoint.
     * @param usuarioIdToken ID del usuario extraído del JWT.
     * @throws ExcepcionAccesoDenegado si los IDs no coinciden.
     */
    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Acceso denegado al perfil financiero: token={} ruta={}", usuarioIdToken, usuarioIdRuta);
            throw new ExcepcionAccesoDenegado();
        }
    }

    /**
     * Aplica los cambios del DTO de solicitud a la entidad de dominio.
     * Solo actualiza campos no nulos (merge parcial).
     *
     * @param e Entidad de dominio {@link PerfilFinanciero}.
     * @param d DTO con los campos a aplicar.
     */
    private void aplicarCambios(PerfilFinanciero e, SolicitudPerfilFinanciero d) {
        if (d.getOcupacion() != null) {
            e.setOcupacion(d.getOcupacion());
        }
        if (d.getIngresoMensual() != null) {
            e.setIngresoMensual(d.getIngresoMensual());
        }
        if (d.getEstiloVida() != null) {
            e.setEstiloVida(d.getEstiloVida());
        }
        if (d.getTonoIA() != null) {
            e.setTonoIA(d.getTonoIA());
        }
    }

    /**
     * Convierte una entidad {@link PerfilFinanciero} a su DTO de respuesta.
     *
     * @param e Entidad de dominio a convertir.
     * @return {@link RespuestaPerfilFinanciero} con los datos mapeados.
     */
    public RespuestaPerfilFinanciero convertirADTO(PerfilFinanciero e) {
        return new RespuestaPerfilFinanciero(
                e.getOcupacion(),
                e.getIngresoMensual(), e.getEstiloVida(), e.getTonoIA(),
                e.getFechaCreacion(), e.getFechaActualizacion());
    }
}
