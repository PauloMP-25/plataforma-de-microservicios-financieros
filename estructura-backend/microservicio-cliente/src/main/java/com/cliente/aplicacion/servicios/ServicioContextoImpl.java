package com.cliente.aplicacion.servicios;

import com.libreria.comun.dtos.ContextoEstrategicoIADTO;
import com.libreria.comun.dtos.ContextoUsuarioDTO;
import com.cliente.aplicacion.puertos.ServicioContexto;
import com.cliente.aplicacion.puertos.ServicioDatosPersonales;
import com.cliente.aplicacion.puertos.ServicioPerfilFinanciero;
import com.cliente.aplicacion.puertos.ServicioMetaAhorro;
import com.cliente.aplicacion.puertos.ServicioLimiteGasto;
import com.cliente.aplicacion.mappers.ContextoMapper;
import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.respuestas.RespuestaPerfilFinanciero;
import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.respuestas.RespuestaLimiteGasto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cliente.infraestructura.mensajeria.PublicadorSincronizacionIA;

/**
 * Servicio que consolida la información del cliente mediante el patrón Facade
 * orquestando llamadas a otros servicios.
 * 
 * @version 1.3.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioContextoImpl implements ServicioContexto {

    private final ServicioDatosPersonales servicioDatosPersonales;
    private final ServicioPerfilFinanciero servicioPerfilFinanciero;
    private final ServicioMetaAhorro servicioMetaAhorro;
    private final ServicioLimiteGasto servicioLimiteGasto;
    private final ContextoMapper mapper;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PublicadorSincronizacionIA publicadorSincronizacionIA;

    @Override
    @Transactional(readOnly = true)
    public ContextoEstrategicoIADTO obtenerContextoFinanciero(UUID usuarioId) {
        log.debug("Construyendo contexto estratégico ligero para IA, usuarioId={}", usuarioId);

        // 1. Obtener el contexto completo consolidado
        ContextoUsuarioDTO completo = obtenerContextoCompleto(usuarioId);

        // 2. Recuperar nombres a través de la capa de servicio (Facade)
        RespuestaDatosPersonales datos = servicioDatosPersonales.consultarInterno(usuarioId);
        String nombres = (datos != null && datos.nombres() != null) ? datos.nombres() : "Usuario";

        // 3. Extraer datos del perfil
        var perfil = completo.getPerfilFinanciero();
        String ocupacion = (perfil != null && perfil.getOcupacion() != null) ? perfil.getOcupacion()
                        : "No especificado";
        BigDecimal ingresoMensual = (perfil != null && perfil.getIngresoMensual() != null)
                        ? perfil.getIngresoMensual()
                        : BigDecimal.ZERO;
        String tonoIA = (perfil != null && perfil.getTonoIA() != null) ? perfil.getTonoIA() : "Amigable";

        // 4. Determinar la meta principal (la de mayor progreso)
        BigDecimal porcentajeMeta = BigDecimal.ZERO;
        String nombreMeta = "Sin metas activas";

        if (completo.getMetas() != null && !completo.getMetas().isEmpty()) {
                var mejorMeta = completo.getMetas().stream()
                                .filter(m -> m.getMontoObjetivo() != null
                                                 && m.getMontoObjetivo().compareTo(BigDecimal.ZERO) > 0)
                                .max(Comparator.comparing(m -> m.getMontoActual()
                                                 .multiply(new BigDecimal("100"))
                                                 .divide(m.getMontoObjetivo(), 2, RoundingMode.HALF_UP)))
                                .orElse(null);

            if (mejorMeta != null) {
                    porcentajeMeta = mejorMeta.getMontoActual().multiply(new BigDecimal("100"))
                                     .divide(mejorMeta.getMontoObjetivo(), 2, RoundingMode.HALF_UP);
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
    public ContextoUsuarioDTO obtenerContextoCompleto(UUID usuarioId) {
            log.debug("Consolidando contexto completo para usuarioId={} mediante Facade", usuarioId);

            // 1. Obtener datos de la capa de servicio correspondientes a cada sub-dominio
            RespuestaPerfilFinanciero perfil = servicioPerfilFinanciero.consultarInterno(usuarioId);
            List<RespuestaMetaAhorro> metas = servicioMetaAhorro.listarInterno(usuarioId);
            RespuestaLimiteGasto limite = servicioLimiteGasto.obtenerActivoInterno(usuarioId);

            // 2. Ensamblar usando Mapper dedicado
            return mapper.ensamblarContextoCompleto(usuarioId, perfil, metas, limite);
    }

    @Override
    public void refrescarContextoRedis(UUID usuarioId) {
        if (usuarioId == null) {
            log.warn("Intento de refresco de contexto en Redis con usuarioId nulo.");
            return;
        }
        try {
            log.info("Refrescando diccionario modular de Redis para usuarioId={}", usuarioId);
            String hashKey = "usuario:" + usuarioId;

            // 1. Obtener y serializar Contexto IA
            ContextoEstrategicoIADTO contexto = obtenerContextoFinanciero(usuarioId);
            redisTemplate.opsForHash().put(hashKey, "ia:contexto", objectMapper.writeValueAsString(contexto));

            // 2. Obtener y serializar Datos Personales
            try {
                RespuestaDatosPersonales datosPersonales = servicioDatosPersonales.consultarInterno(usuarioId);
                if (datosPersonales != null) {
                    redisTemplate.opsForHash().put(hashKey, "datos_personales", objectMapper.writeValueAsString(datosPersonales));
                }
            } catch (Exception e) {
                log.error("Error al cachear datos_personales para usuarioId={}", usuarioId, e);
            }

            // 3. Obtener y serializar Perfil Financiero
            try {
                RespuestaPerfilFinanciero perfilFinanciero = servicioPerfilFinanciero.consultarInterno(usuarioId);
                if (perfilFinanciero != null) {
                    redisTemplate.opsForHash().put(hashKey, "perfil_financiero", objectMapper.writeValueAsString(perfilFinanciero));
                }
            } catch (Exception e) {
                log.error("Error al cachear perfil_financiero para usuarioId={}", usuarioId, e);
            }

            // 4. Obtener y serializar Metas
            try {
                List<RespuestaMetaAhorro> metas = servicioMetaAhorro.listarInterno(usuarioId);
                if (metas != null) {
                    redisTemplate.opsForHash().put(hashKey, "metas", objectMapper.writeValueAsString(metas));
                }
            } catch (Exception e) {
                log.error("Error al cachear metas para usuarioId={}", usuarioId, e);
            }

            // 5. Obtener y serializar Límites de Gasto
            try {
                RespuestaLimiteGasto limite = servicioLimiteGasto.obtenerActivoInterno(usuarioId);
                if (limite != null) {
                    redisTemplate.opsForHash().put(hashKey, "limites", objectMapper.writeValueAsString(limite));
                }
            } catch (Exception e) {
                log.error("Error al cachear limites para usuarioId={}", usuarioId, e);
            }

            // 6. Configurar expiración global para la "carpeta/diccionario" del usuario en Redis (1 hora)
            redisTemplate.expire(hashKey, java.time.Duration.ofHours(1));
            
            log.info("Diccionario de Redis refrescado exitosamente para usuarioId={}", usuarioId);

            // Publicar a RabbitMQ para sincronización en tiempo real con ms-ia (síncronamente en hilo seguro)
            publicadorSincronizacionIA.publicarActualizacionContexto(usuarioId, contexto);
        } catch (Exception e) {
            log.error("Error al refrescar diccionario modular en Redis para usuarioId={}", usuarioId, e);
        }
    }
}
