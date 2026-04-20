package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.IntentoValidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntentoValidacionRepository extends JpaRepository<IntentoValidacion, Long> {

    Optional<IntentoValidacion> findByUsuarioId(UUID usuarioId);

    /**
     * Desbloquea masivamente a los usuarios cuyo tiempo de castigo terminó. Es
     * más eficiente que iterar uno por uno en Java.
     * @param ahora
     * @return 
     */
    @Modifying
    @Query("""
        UPDATE IntentoValidacion iv
        SET iv.bloqueado = false, iv.intentos = 0, iv.bloqueadoHasta = null
        WHERE iv.bloqueado = true AND iv.bloqueadoHasta < :ahora
        """)
    int desbloquearUsuariosExpirados(@Param("ahora") LocalDateTime ahora);

    /**
     * Optimización de memoria: Elimina registros de intentos de usuarios que NO
     * están bloqueados y no han tenido actividad en los últimos 7 días.
     * @param fecha
     * @return 
     */
    @Modifying
    @Query("DELETE FROM IntentoValidacion i WHERE i.bloqueado = false AND i.ultimaModificacion < :fecha")
    int purgarRegistrosInactivos(@Param("fecha") LocalDateTime fecha);
}
