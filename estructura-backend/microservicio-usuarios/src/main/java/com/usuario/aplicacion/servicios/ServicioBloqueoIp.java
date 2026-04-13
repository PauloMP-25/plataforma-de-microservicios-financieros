package com.usuario.aplicacion.servicios;

import com.usuario.aplicacion.dtos.RegistroAuditoriaDTO;
import com.usuario.infraestructura.clientes.ClienteAuditoria;
import com.usuario.dominio.entidades.IntentoLogin;
import com.usuario.dominio.repositorios.IntentoLoginRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioBloqueoIp {

    private final IntentoLoginRepository repository;

    @Value("${application.ip-blocker.max-attempts:3}")
    private int maxIntentos;

    @Value("${application.ip-blocker.window-minutes:5}")
    private long ventanaMinutos;

    @Value("${application.ip-blocker.block-duration-hours:24}")
    private long duracionBloqueoHoras;

    private final ClienteAuditoria clienteAuditoria;
    private static final String MODULO = "MICROSERVICIO-USUARIO";

    /**
     * Caché en memoria: IP → fecha de desbloqueo
     */
    private final ConcurrentHashMap<String, LocalDateTime> cacheIpsBloqueadas
            = new ConcurrentHashMap<>();

    // =========================================================================
    // API pública
    // =========================================================================
    public boolean bloqueado(String ip) {
        LocalDateTime hasta = cacheIpsBloqueadas.get(ip);

        if (hasta != null) {
            if (LocalDateTime.now().isBefore(hasta)) {
                return true;
            }
            cacheIpsBloqueadas.remove(ip);
        }

        return repository.findByDireccionIp(ip)
                .map(intento -> {
                    if (intento.isBloqueado() && !intento.bloqueoExpirado()) {
                        cacheIpsBloqueadas.put(ip, intento.getBloqueadoHasta());
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Transactional
    public boolean loginFallido(String ip) {

        IntentoLogin intento = repository.findByDireccionIp(ip)
                .orElseGet(() -> IntentoLogin.builder()
                .direccionIp(ip)
                .intentos(0)
                .ultimaModificacion(LocalDateTime.now())
                .bloqueado(false)
                .build()
                );

        if (ventanaExpirada(intento)) {
            intento.reiniciar();
        }

        intento.incrementarIntentos();

        boolean debeBloquear = intento.getIntentos() >= maxIntentos;

        if (debeBloquear) {
            intento.bloquear(duracionBloqueoHoras);
            cacheIpsBloqueadas.put(ip, intento.getBloqueadoHasta());
            clienteAuditoria.enviar(new RegistroAuditoriaDTO(
                    "SISTEMA",
                    "IP_BLOQUEADA",
                    String.format("IP bloqueada tras %d intentos fallidos. Duración: %dh",
                            intento.getIntentos(), duracionBloqueoHoras),
                    ip,
                    MODULO
            ));
            log.warn("IP {} BLOQUEADA — intentos: {}/{}", ip, intento.getIntentos(), maxIntentos);
        }

        repository.save(intento);
        return debeBloquear;
    }

    @Transactional
    public void loginExitoso(String ip) {

        cacheIpsBloqueadas.remove(ip);

        repository.findByDireccionIp(ip).ifPresent(intento -> {
            if (intento.getIntentos() > 0 || intento.isBloqueado()) {
                intento.reiniciar();
                repository.save(intento);
            }
        });
    }

    public long minutosParaDesbloqueo(String ip) {

        LocalDateTime hasta = cacheIpsBloqueadas.get(ip);

        if (hasta != null && LocalDateTime.now().isBefore(hasta)) {
            return ChronoUnit.MINUTES.between(LocalDateTime.now(), hasta);
        }

        return repository.findByDireccionIp(ip)
                .filter(i -> i.isBloqueado() && !i.bloqueoExpirado())
                .map(i -> ChronoUnit.MINUTES.between(LocalDateTime.now(), i.getBloqueadoHasta()))
                .orElse(0L);
    }

    // =========================================================================
    // Jobs programados
    // =========================================================================
    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void desbloquearIpsExpiradas() {
        int desbloqueadas = repository.desbloquearIpsExpiradas(LocalDateTime.now());
        limpiarCacheExpirada();

        if (desbloqueadas > 0) {
            log.info("IPs desbloqueadas automáticamente: {}", desbloqueadas);
        }
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void limpiarRegistrosAntiguos() {
        LocalDateTime umbral = LocalDateTime.now().minusHours(48);
        repository.eliminarRegistrosAntiguos(umbral);
    }

    // =========================================================================
    // Privados
    // =========================================================================
    private boolean ventanaExpirada(IntentoLogin intento) {

        if (intento.getUltimaModificacion() == null) {
            return true;
        }

        long minutos = ChronoUnit.MINUTES.between(
                intento.getUltimaModificacion(),
                LocalDateTime.now()
        );

        return minutos > ventanaMinutos;
    }

    private void limpiarCacheExpirada() {

        LocalDateTime ahora = LocalDateTime.now();

        cacheIpsBloqueadas.entrySet().removeIf(entry
                -> ahora.isAfter(entry.getValue())
        );
    }
}
