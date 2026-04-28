package com.cliente.aplicacion.servicios;

import com.cliente.aplicacion.dtos.*;
import com.cliente.dominio.repositorios.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio que agrega toda la información del cliente en un único objeto.
 * Utilizado por el microservicio-nucleo-financiero vía Feign para
 * obtener el contexto completo con una sola llamada HTTP.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioContextoCliente {

    private final DatosPersonalesRepositorio   repoDatosPersonales;
    private final PerfilFinancieroRepositorio  repoPerfilFinanciero;
    private final MetaAhorroRepositorio        repoMetaAhorro;
    private final LimiteGastoRepositorio       repoLimiteGasto;

    private final ServicioDatosPersonales   servicioDatos;
    private final ServicioPerfilFinanciero  servicioPerfilFinanciero;
    private final ServicioMetaAhorro        servicioMetaAhorro;
    private final ServicioLimiteGasto       servicioLimiteGasto;

    /**
     * Retorna el contexto completo del cliente: datos personales, perfil
     * financiero, metas activas y límites de gasto.
     *
     * Este endpoint es INTERNO — consumido por microservicios de confianza.
     * No requiere validación de propiedad porque el llamante es el núcleo
     * financiero, no el usuario final.
     *
     * @param usuarioId UUID del usuario cuyo contexto se solicita
     * @return agregado con toda la información del cliente
     */
    @Transactional(readOnly = true)
    public RespuestaContextoCliente obtenerContexto(UUID usuarioId) {
        log.debug("Consultando contexto completo para usuarioId={}", usuarioId);

        // Datos personales (puede ser null si aún no los completó)
        RespuestaDatosPersonales datos = repoDatosPersonales
                .findByUsuarioId(usuarioId)
                .map(servicioDatos::convertirADTO)
                .orElse(null);

        // Perfil financiero (puede ser null)
        RespuestaPerfilFinanciero perfil = repoPerfilFinanciero
                .findByUsuarioId(usuarioId)
                .map(servicioPerfilFinanciero::convertirADTO)
                .orElse(null);

        // Solo metas ACTIVAS para no saturar el contexto
        List<RespuestaMetaAhorro> metas = repoMetaAhorro
                .findMetasActivasOrdenadas(usuarioId)
                .stream()
                .map(servicioMetaAhorro::convertirADTO)
                .collect(Collectors.toList());

        // Todos los límites del usuario
        List<RespuestaLimiteGasto> limites = repoLimiteGasto
                .findByUsuarioIdOrderByCategoriaIdAsc(usuarioId)
                .stream()
                .map(servicioLimiteGasto::convertirADTO)
                .collect(Collectors.toList());

        return new RespuestaContextoCliente(usuarioId, datos, perfil, metas, limites);
    }
}
