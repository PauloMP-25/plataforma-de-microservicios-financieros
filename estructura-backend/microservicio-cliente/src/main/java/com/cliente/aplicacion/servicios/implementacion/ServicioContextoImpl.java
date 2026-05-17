package com.cliente.aplicacion.servicios.implementacion;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.cliente.aplicacion.servicios.ServicioContexto;
import com.cliente.dominio.repositorios.DatosPersonalesRepositorio;
import com.cliente.dominio.repositorios.LimiteGastoRepositorio;
import com.cliente.dominio.repositorios.MetaAhorroRepositorio;
import com.cliente.dominio.repositorios.PerfilFinancieroRepositorio;

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

        // 1. Obtener el contexto completo para evitar múltiples queries dispersas
        com.libreria.comun.dtos.ContextoUsuarioDTO completo = obtenerContextoCompleto(usuarioId);

        // 2. Recuperar nombres (esto no está en el DTO común pero es necesario para el
        // saludo)
        String nombres = repoDatosPersonales.findByUsuarioId(usuarioId)
                .map(com.cliente.dominio.entidades.DatosPersonales::getNombres)
                .orElse("Usuario");

        // 3. Extraer datos del perfil
        var perfil = completo.getPerfilFinanciero();
        String ocupacion = (perfil != null && perfil.getOcupacion() != null) ? perfil.getOcupacion()
                        : "No especificado";
        java.math.BigDecimal ingresoMensual = (perfil != null && perfil.getIngresoMensual() != null)
                        ? perfil.getIngresoMensual()
                        : java.math.BigDecimal.ZERO;
        String tonoIA = (perfil != null && perfil.getTonoIA() != null) ? perfil.getTonoIA() : "Amigable";

        // 4. Determinar la meta principal (la de mayor progreso)
        java.math.BigDecimal porcentajeMeta = java.math.BigDecimal.ZERO;
        String nombreMeta = "Sin metas activas";

        if (completo.getMetas() != null && !completo.getMetas().isEmpty()) {
                var mejorMeta = completo.getMetas().stream()
                                .filter(m -> m.getMontoObjetivo() != null
                                                && m.getMontoObjetivo().compareTo(java.math.BigDecimal.ZERO) > 0)
                                .max(java.util.Comparator.comparing(m -> m.getMontoActual()
                                                .multiply(new java.math.BigDecimal("100"))
                                                .divide(m.getMontoObjetivo(), 2, java.math.RoundingMode.HALF_UP)))
                                .orElse(null);

            if (mejorMeta != null) {
                    porcentajeMeta = mejorMeta.getMontoActual().multiply(new java.math.BigDecimal("100"))
                                    .divide(mejorMeta.getMontoObjetivo(), 2, java.math.RoundingMode.HALF_UP);
                    nombreMeta = mejorMeta.getNombre();
            }
    }

        // 5. Límite de Gasto
        Integer alertaGasto = (completo.getLimiteGlobal() != null)
                        ? completo.getLimiteGlobal().getPorcentajeAlerta()
                        : 80;

        return new ContextoEstrategicoIADTO(
                nombres,
                ocupacion,
                ingresoMensual,
                tonoIA,
                porcentajeMeta,
                nombreMeta,
                alertaGasto);
    }

    @Override
    @Transactional(readOnly = true)
    public com.libreria.comun.dtos.ContextoUsuarioDTO obtenerContextoCompleto(UUID usuarioId) {
            log.debug("Consolidando contexto completo para usuarioId={}", usuarioId);

            // 1. Perfil Financiero
            var perfilDTO = repoPerfilFinanciero.findByUsuarioId(usuarioId)
                            .map(p -> com.libreria.comun.dtos.ContextoUsuarioDTO.PerfilFinancieroDTO.builder()
                                            .ocupacion(p.getOcupacion())
                                            .ingresoMensual(p.getIngresoMensual())
                                            .tonoIA(p.getTonoIA())
                                            .nivelRiesgo(p.getEstiloVida())
                                            .build())
                            .orElse(null);

            // 2. Metas de Ahorro (Corregido findByUsuarioId)
            var metasDTO = repoMetaAhorro.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                            .map(m -> com.libreria.comun.dtos.ContextoUsuarioDTO.MetaAhorroDTO.builder()
                                            .nombre(m.getNombre())
                                            .montoObjetivo(m.getMontoObjetivo())
                                            .montoActual(m.getMontoActual())
                                            .fechaMeta(m.getFechaLimite() != null ? m.getFechaLimite().toString()
                                                            : null)
                                            .build())
                            .collect(java.util.stream.Collectors.toList());

            // 3. Límite Global
            var limiteDTO = repoLimiteGasto.findByUsuarioIdAndActivoTrue(usuarioId)
                            .map(l -> com.libreria.comun.dtos.ContextoUsuarioDTO.LimiteGlobalDTO.builder()
                                            .montoLimite(l.getMontoLimite())
                                            .porcentajeAlerta(l.getPorcentajeAlerta())
                                            .build())
                            .orElse(null);

            return com.libreria.comun.dtos.ContextoUsuarioDTO.builder()
                            .idUsuario(usuarioId)
                            .perfilFinanciero(perfilDTO)
                            .metas(metasDTO)
                            .limiteGlobal(limiteDTO)
                            .build();
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
