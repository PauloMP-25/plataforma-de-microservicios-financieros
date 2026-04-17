package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.IntentoValidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntentoValidacionRepository extends JpaRepository<IntentoValidacion, Long> {

    Optional<IntentoValidacion> findByUsuarioId(UUID usuarioId);

    /**
     * Desbloquea automáticamente los usuarioIds cuyo período ya expiró.
     * Invocado por el job de limpieza cada 15 minutos.
     * @param ahora
     * @return 
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE IntentoValidacion iv
        SET iv.bloqueado = false, iv.intentos = 0, iv.bloqueadoHasta = null
        WHERE iv.bloqueado = true AND iv.bloqueadoHasta < :ahora
        """)
    int desbloquearUsuariosExpirados(@Param("ahora") LocalDateTime ahora);
}
