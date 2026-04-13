package com.usuario.dominio.repositorios;

import com.usuario.dominio.entidades.IntentoLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface IntentoLoginRepository extends JpaRepository<IntentoLogin, Long> {
    Optional<IntentoLogin> findByDireccionIp(String direccionIp);

    /**
     * Limpieza programada: IPs bloqueadas cuyo tiempo ya expiró.
     * @param ahora
     * @return 
     */
    @Modifying
    @Transactional
    @Query("UPDATE IntentoLogin il SET il.bloqueado = false, il.intentos = 0, il.bloqueadoHasta = null " +
           "WHERE il.bloqueado = true AND il.bloqueadoHasta < :ahora")
    int desbloquearIpsExpiradas(LocalDateTime ahora);
    
    /**
     * Limpieza de registros antiguos no bloqueados (housekeeping).
     * @param umbral
     * @return 
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM IntentoLogin il WHERE il.bloqueado = false AND il.ultimaModificacion < :umbral")
    int eliminarRegistrosAntiguos(LocalDateTime umbral);
}
