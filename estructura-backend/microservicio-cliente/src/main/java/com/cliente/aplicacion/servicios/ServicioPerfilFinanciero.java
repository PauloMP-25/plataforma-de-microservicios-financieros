package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.excepciones.*;
import com.cliente.dominio.entidades.PerfilFinanciero;
import com.cliente.dominio.repositorios.PerfilFinancieroRepositorio;
import com.cliente.infraestructura.mensajeria.PublicadorAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
/**
 * Lógica de negocio para el perfil financiero del cliente.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioPerfilFinanciero {

    private final PerfilFinancieroRepositorio repositorio;
    private final PublicadorAuditoria publicadorAuditoria;

    /**
     * Crea o actualiza el perfil financiero (upsert). Si no existe, lo crea. Si
     * existe, lo actualiza.
     * @param usuarioIdRuta
     * @param usuarioIdToken
     * @param solicitud
     * @param ipOrigen
     * @return 
     */
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

        publicadorAuditoria.publicar(EventoAuditoria.de(
                usuarioIdToken.toString(), "PERFIL_FINANCIERO_ACTUALIZADO", ipOrigen,
                "Perfil financiero guardado para usuarioId: " + usuarioIdRuta
        ));

        return convertirADTO(guardado);
    }

    /**
     * Consulta el perfil financiero del usuario.
     * @param usuarioIdRuta
     * @param usuarioIdToken
     * @return 
     */
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
    private void validarPropiedad(UUID usuarioIdRuta, UUID usuarioIdToken) {
        if (!usuarioIdRuta.equals(usuarioIdToken)) {
            log.warn("Acceso denegado al perfil financiero: token={} ruta={}", usuarioIdToken, usuarioIdRuta);
            throw new AccesoDenegadoException();
        }
    }

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

    public RespuestaPerfilFinanciero convertirADTO(PerfilFinanciero e) {
        return new RespuestaPerfilFinanciero(
                e.getOcupacion(),
                e.getIngresoMensual(), e.getEstiloVida(), e.getTonoIA(),
                e.getFechaCreacion(), e.getFechaActualizacion()
        );
    }
}
