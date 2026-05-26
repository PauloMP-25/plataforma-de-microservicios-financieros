package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RespuestaVerificacionIpDTO;
import com.auditoria.aplicacion.excepciones.IpBloqueadaException;
import com.auditoria.aplicacion.puertos.ServicioSeguridadAuditoria;
import com.auditoria.infraestructura.configuracion.PropiedadesSeguridad;
import com.libreria.comun.enums.EstadoEvento;
import com.auditoria.dominio.entidades.ListaNegraIp;
import com.auditoria.dominio.repositorios.AuditoriaAccesoRepository;
import com.auditoria.dominio.repositorios.ListaNegraIpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final PropiedadesSeguridad propiedadesSeguridad;

    @Override
    @Transactional
    public void verificarIntentoFallido(String ipOrigen) {
        LocalDateTime ventanaDesde = LocalDateTime.now().minusMinutes(propiedadesSeguridad.getVentanaMinutos());

        long fallosRecientes = repositorioAcceso.contarIntentosPorIpYEstadoDesde(
                ipOrigen, EstadoEvento.FALLO, ventanaDesde);

        log.debug("[SEGURIDAD] Evaluación de IP: {}. Fallos: {}/{}", ipOrigen, fallosRecientes,
                propiedadesSeguridad.getMaxIntentosFallidos());

        if (fallosRecientes >= propiedadesSeguridad.getMaxIntentosFallidos()) {
            bloquearIp(ipOrigen, fallosRecientes);
            // Sincronizar con Redis para el Gateway
            redisTemplate.opsForValue().set("bloqueo:ip:" + ipOrigen, true,
                    java.time.Duration.ofMinutes(propiedadesSeguridad.getBloqueoMinutos()));
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
    @Transactional
    public void limpiarBloqueosExpirados() {
        int eliminados = repositorioListaNegra.eliminarBloqueoExpirados(LocalDateTime.now());
        if (eliminados > 0) {
            log.info("[SEGURIDAD] Mantenimiento: {} bloqueos expirados eliminados.", eliminados);
        }
    }

    private void bloquearIp(String ip, long intentos) {
        LocalDateTime ahora = LocalDateTime.now();
        ListaNegraIp registro = repositorioListaNegra.findActivaByIp(Objects.requireNonNull(ip), ahora)
                .orElse(ListaNegraIp.builder().ip(ip).fechaBloqueo(ahora).build());

        registro.setMotivo(String.format("Bloqueo automático: %d intentos fallidos.", intentos));
        registro.setFechaExpiracion(ahora.plusMinutes(propiedadesSeguridad.getBloqueoMinutos()));

        repositorioListaNegra.save(registro);
        log.warn("[SEGURIDAD] IP bloqueada por fuerza bruta: {} hasta {}", ip, registro.getFechaExpiracion());
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ListaNegraIp> listarBloqueos(org.springframework.data.domain.Pageable paginacion) {
        return repositorioListaNegra.findAll(Objects.requireNonNull(paginacion));
    }

    @Override
    @Transactional
    public void bloquearIpManualmente(String ip, String motivo, int minutos) {
        LocalDateTime ahora = LocalDateTime.now();
        ListaNegraIp registro = repositorioListaNegra.findActivaByIp(Objects.requireNonNull(ip), ahora)
                .orElse(ListaNegraIp.builder().ip(ip).fechaBloqueo(ahora).build());

        registro.setMotivo(motivo != null ? motivo : "Bloqueo administrativo manual.");
        if (minutos > 0) {
            registro.setFechaExpiracion(ahora.plusMinutes(minutos));
        } else {
            registro.setFechaExpiracion(null); // Bloqueo permanente
        }

        repositorioListaNegra.save(registro);
        
        // Sincronizar con Redis
        if (minutos > 0) {
            redisTemplate.opsForValue().set("bloqueo:ip:" + ip, true, java.time.Duration.ofMinutes(minutos));
        } else {
            redisTemplate.opsForValue().set("bloqueo:ip:" + ip, true);
        }
        log.warn("[SEGURIDAD-ADMIN] IP bloqueada manualmente: {} por motivo: {}. Expiración: {}", 
                ip, registro.getMotivo(), registro.getFechaExpiracion());
    }

    @Override
    @Transactional
    public void desbloquearIpManualmente(String ip) {
        LocalDateTime ahora = LocalDateTime.now();
        repositorioListaNegra.findActivaByIp(Objects.requireNonNull(ip), ahora).ifPresent(registro -> {
            registro.setFechaExpiracion(ahora);
            registro.setMotivo(registro.getMotivo() + " (Desbloqueado manualmente)");
            repositorioListaNegra.save(registro);
        });

        // Remueve de Redis
        redisTemplate.delete("bloqueo:ip:" + ip);
        log.info("[SEGURIDAD-ADMIN] IP desbloqueada manualmente y sincronizada en Redis: {}", ip);
    }
}
