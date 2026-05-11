package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.ListaNegraIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio para la gestión de IPs bloqueadas por seguridad.
 * Utilizado por el Gateway para filtrar peticiones maliciosas en tiempo real.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
public interface ListaNegraIpRepository extends JpaRepository<ListaNegraIp, String> {

    /**
     * Busca un registro de bloqueo que no haya expirado para la IP dada.
     * 
     * @param ip    Dirección IP a consultar.
     * @param ahora Momento actual de validación.
     * @return Optional con el registro si el bloqueo sigue vigente.
     */
    @Query("""
            SELECT l FROM ListaNegraIp l
            WHERE l.ip = :ip
              AND (l.fechaExpiracion IS NULL OR l.fechaExpiracion > :ahora)
            """)
    Optional<ListaNegraIp> findActivaByIp(
            @Param("ip") String ip,
            @Param("ahora") LocalDateTime ahora);

    /**
     * Elimina de forma masiva los registros cuya fecha de expiración ha pasado.
     * 
     * @param ahora Fecha actual para determinar la caducidad.
     * @return Total de registros eliminados.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ListaNegraIp l WHERE l.fechaExpiracion IS NOT NULL AND l.fechaExpiracion < :ahora")
    int eliminarBloqueoExpirados(@Param("ahora") LocalDateTime ahora);

    /**
     * Valida la existencia de un bloqueo activo (permanente o temporal).
     * 
     * @param ip    Dirección IP a verificar.
     * @param ahora Momento actual de validación.
     * @return Verdadero si la IP está actualmente en la lista negra.
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM ListaNegraIp l
            WHERE l.ip = :ip
              AND (l.fechaExpiracion IS NULL OR l.fechaExpiracion > :ahora)
            """)
    boolean existeBloqueoActivo(
            @Param("ip") String ip,
            @Param("ahora") LocalDateTime ahora);
}