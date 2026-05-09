package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.ListaNegraIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ListaNegraIpRepository extends JpaRepository<ListaNegraIp, String> {

    /**
     * Verifica si una IP existe en la lista negra con bloqueo activo (no expirado).
     * Este es el método crítico de rendimiento: se ejecuta en cada petición al Gateway.
     *
     * @param ip    Dirección IP a verificar
     * @param ahora Momento actual para comparar con la fecha de expiración
     * @return Optional con el registro si la IP está bloqueada activamente
     */
    @Query("""
        SELECT l FROM ListaNegraIp l
        WHERE l.ip = :ip
          AND (l.fechaExpiracion IS NULL OR l.fechaExpiracion > :ahora)
        """)
    Optional<ListaNegraIp> findActivaByIp(
        @Param("ip")    String ip,
        @Param("ahora") LocalDateTime ahora
    );

    /**
     * Elimina IPs cuyo período de bloqueo ya venció.
     * Mantenimiento automático programado cada hora.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ListaNegraIp l WHERE l.fechaExpiracion IS NOT NULL AND l.fechaExpiracion < :ahora")
    int eliminarBloqueoExpirados(@Param("ahora") LocalDateTime ahora);

    /**
     * Comprueba si existe algún bloqueo activo (permanente o no expirado) para la IP.
     */
    @Query("""
        SELECT COUNT(l) > 0 FROM ListaNegraIp l
        WHERE l.ip = :ip
          AND (l.fechaExpiracion IS NULL OR l.fechaExpiracion > :ahora)
        """)
    boolean existeBloqueoActivo(
        @Param("ip")    String ip,
        @Param("ahora") LocalDateTime ahora
    );
}