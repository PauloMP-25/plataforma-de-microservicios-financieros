package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.AuditoriaAcceso;
import com.auditoria.dominio.entidades.AuditoriaAcceso.EstadoAcceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditoriaAccesoRepository extends JpaRepository<AuditoriaAcceso, UUID> {

    /**
     * Cuenta los fallos de una IP dentro de una ventana temporal.
     * Es el núcleo de la lógica de detección de fuerza bruta.
     *
     * @param ipOrigen IP a evaluar
     * @param estado   Siempre FALLO en el caso de uso principal
     * @param desde    Inicio de la ventana temporal (ej: ahora - 10 min)
     * @return cantidad de intentos fallidos en el período
     */
    @Query("""
        SELECT COUNT(a) FROM AuditoriaAcceso a
        WHERE a.ipOrigen = :ipOrigen
          AND a.estado = :estado
          AND a.fecha >= :desde
        """)
    long contarIntentosPorIpYEstadoDesde(
        @Param("ipOrigen") String ipOrigen,
        @Param("estado")   EstadoAcceso estado,
        @Param("desde")    LocalDateTime desde
    );

    /**
     * Historial de accesos de un usuario específico, ordenado por fecha descendente.
     */
    Page<AuditoriaAcceso> findByUsuarioIdOrderByFechaDesc(UUID usuarioId, Pageable paginacion);

    /**
     * Historial de accesos de una IP específica (para auditorías forenses).
     */
    Page<AuditoriaAcceso> findByIpOrigenOrderByFechaDesc(String ipOrigen, Pageable paginacion);

    /**
     * Limpieza programada: elimina registros anteriores al umbral dado.
     * Se invoca desde una tarea @Scheduled para mantener la tabla ligera.
     */
    @Query("DELETE FROM AuditoriaAcceso a WHERE a.fecha < :umbral")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int eliminarRegistrosAnterioresA(@Param("umbral") LocalDateTime umbral);
}
