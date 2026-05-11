package com.cliente.aplicacion.servicios.implementacion;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.cliente.aplicacion.servicios.ServicioContexto;
import com.cliente.dominio.repositorios.DatosPersonalesRepositorio;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.dominio.repositorios.PerfilFinancieroRepositorio;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cliente.infraestructura.mensajeria.PublicadorSincronizacionIA;

/**
 * Servicio que agrega toda la información del cliente en un único objeto.
 * Utilizado por el microservicio-nucleo-financiero vía Feign para obtener el
 * contexto completo con una sola llamada HTTP.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioContextoImpl implements ServicioContexto {

    private final DatosPersonalesRepositorio repoDatosPersonales;
    private final PerfilFinancieroRepositorio repoPerfilFinanciero;
    private final MetaAhorroRepositorio repoMetaAhorro;
    private final LimiteGastoRepositorio repoLimiteGasto;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PublicadorSincronizacionIA publicadorSincronizacionIA;

    @Override
    @Transactional(readOnly = true)
    public ContextoEstrategicoIADTO obtenerContextoFinanciero(UUID usuarioId) {
        log.debug("Construyendo contexto estratégico ligero para IA, usuarioId={}", usuarioId);

        // 1. Nombres
        String nombres = repoDatosPersonales.findByUsuarioId(usuarioId)
                .map(com.cliente.dominio.entidades.DatosPersonales::getNombres)
                .orElse("Usuario");

        // 2. Perfil Financiero (Ocupación, Ingreso, Tono IA)
        String ocupacion = "No especificado";
        java.math.BigDecimal ingresoMensual = java.math.BigDecimal.ZERO;
        String tonoIA = "Amigable"; // Default

        java.util.Optional<com.cliente.dominio.entidades.PerfilFinanciero> perfilOpt = repoPerfilFinanciero
                .findByUsuarioId(usuarioId);
        if (perfilOpt.isPresent()) {
            com.cliente.dominio.entidades.PerfilFinanciero p = perfilOpt.get();
            if (p.getOcupacion() != null)
                ocupacion = p.getOcupacion();
            if (p.getIngresoMensual() != null)
                ingresoMensual = p.getIngresoMensual();
            if (p.getTonoIA() != null)
                tonoIA = p.getTonoIA();
        }

        // 3. Meta con mayor progreso
        java.math.BigDecimal porcentajeMeta = java.math.BigDecimal.ZERO;
        String nombreMeta = "Sin metas activas";

        List<com.cliente.dominio.entidades.MetaAhorro> metas = repoMetaAhorro.findMetasActivasOrdenadas(usuarioId);
        if (!metas.isEmpty()) {
            com.cliente.dominio.entidades.MetaAhorro mejorMeta = metas.stream()
                    .max(java.util.Comparator
                            .comparing(com.cliente.dominio.entidades.MetaAhorro::calcularPorcentajeProgreso))
                    .orElse(metas.get(0));

            porcentajeMeta = mejorMeta.calcularPorcentajeProgreso();
            nombreMeta = mejorMeta.getNombre();
        }

        // 4. Límite de Gasto (porcentaje de alerta)
        Integer alertaGasto = repoLimiteGasto.findByUsuarioIdAndActivoTrue(usuarioId)
                .map(com.cliente.dominio.entidades.LimiteGasto::getPorcentajeAlerta)
                .orElse(80); // Default 80%

        return new ContextoEstrategicoIADTO(
                nombres,
                ocupacion,
                ingresoMensual,
                tonoIA,
                porcentajeMeta,
                nombreMeta,
                alertaGasto);
    }

    @SuppressWarnings("null")
    @Override
    @Async
    public void refrescarContextoRedis(UUID usuarioId) {
        try {
            log.info("Refrescando contexto IA en Redis para usuarioId={}", usuarioId);
            ContextoEstrategicoIADTO contexto = obtenerContextoFinanciero(usuarioId);
            String redisKey = "ia:contexto:" + usuarioId;
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(contexto),
                    java.time.Duration.ofHours(1));

            // Publicar a RabbitMQ para sincronización en tiempo real con ms-ia
            publicadorSincronizacionIA.publicarActualizacionContexto(usuarioId, contexto);
        } catch (Exception e) {
            log.error("Error al refrescar contexto en Redis para usuarioId={}", usuarioId, e);
        }
    }
}
