package com.auditoria.aplicacion.servicios.implementacion;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;
import com.auditoria.aplicacion.excepciones.IpBloqueadaException;
import com.auditoria.aplicacion.servicios.ServicioSeguridadAuditoria;
import com.libreria.comun.enums.EstadoEvento;
import com.auditoria.dominio.entidades.ListaNegraIp;
import com.auditoria.dominio.repositorios.AuditoriaAccesoRepository;
import com.auditoria.dominio.repositorios.ListaNegraIpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Implementación de la lógica de protección contra fuerza bruta y gestión de
 * lista negra.
 * <p>
 * Utiliza una ventana deslizante de tiempo y un umbral de intentos fallidos
 * configurables para determinar el bloqueo automático de atacantes.
 * </p>
 * 
 * @author Paulo Moron
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioSeguridadAuditoriaImpl implements ServicioSeguridadAuditoria {

    private final AuditoriaAccesoRepository repositorioAcceso;
    private final ListaNegraIpRepository repositorioListaNegra;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Value("${auditoria.seguridad.max-intentos-fallidos:3}")
    private int maxIntentosFallidos;

    @Value("${auditoria.seguridad.ventana-minutos:10}")
    private long ventanaMinutos;

    @Value("${auditoria.seguridad.bloqueo-minutos:30}")
    private long bloqueoMinutos;

    @SuppressWarnings("null")
    @Override
    @Transactional
    public void verificarIntentoFallido(String ipOrigen) {
        LocalDateTime ventanaDesde = LocalDateTime.now().minusMinutes(ventanaMinutos);

        long fallosRecientes = repositorioAcceso.contarIntentosPorIpYEstadoDesde(
                ipOrigen, EstadoEvento.FALLO, ventanaDesde);

        log.debug("[SEGURIDAD] Evaluación de IP: {}. Fallos: {}/{}", ipOrigen, fallosRecientes, maxIntentosFallidos);

        if (fallosRecientes >= maxIntentosFallidos) {
            bloquearIp(ipOrigen, fallosRecientes);
            // Sincronizar con Redis para el Gateway
            redisTemplate.opsForValue().set("bloqueo:ip:" + ipOrigen, true, java.time.Duration.ofMinutes(bloqueoMinutos));
            log.info("[SEGURIDAD-REDIS] IP sincronizada en caché de bloqueo: {}", ipOrigen);
            
            throw new IpBloqueadaException(ipOrigen);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaVerificacionIpDTO verificarEstadoIp(String ip) {
        return repositorioListaNegra.findActivaByIp(ip, LocalDateTime.now())
                .map(reg -> RespuestaVerificacionIpDTO.bloqueada(ip, reg.getMotivo(), reg.getFechaExpiracion()))
                .orElseGet(() -> RespuestaVerificacionIpDTO.libre(ip));
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Cada 60 minutos
    @Transactional
    public void limpiarBloqueosExpirados() {
        int eliminados = repositorioListaNegra.eliminarBloqueoExpirados(LocalDateTime.now());
        if (eliminados > 0) {
            log.info("[SEGURIDAD] Mantenimiento: {} bloqueos expirados eliminados.", eliminados);
        }
    }

    /**
     * Registra o actualiza el bloqueo de una IP en la base de datos.
     */
    private void bloquearIp(String ip, long intentos) {
        LocalDateTime ahora = LocalDateTime.now();
        ListaNegraIp registro = repositorioListaNegra.findById(Objects.requireNonNull(ip))
                .orElse(ListaNegraIp.builder().ip(ip).fechaBloqueo(ahora).build());

        registro.setMotivo(String.format("Bloqueo automático: %d intentos fallidos.", intentos));
        registro.setFechaExpiracion(ahora.plusMinutes(bloqueoMinutos));

        repositorioListaNegra.save(registro);
        log.warn("[SEGURIDAD] IP bloqueada por fuerza bruta: {} hasta {}", ip, registro.getFechaExpiracion());
    }
}
