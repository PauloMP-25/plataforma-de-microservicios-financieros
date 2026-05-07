package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.AuditoriaTransaccional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditoriaTransaccionalRepository extends JpaRepository<AuditoriaTransaccional, UUID> {

    /**
     * Historial de cambios realizados por un usuario específico.
     */
    Page<AuditoriaTransaccional> findByUsuarioIdOrderByFechaDesc(UUID usuarioId, Pageable paginacion);

    /**
     * Todos los cambios sobre una entidad específica (ej: todos los cambios del Cliente X).
     * Útil para reconstruir el historial completo de un registro.
     */
    Page<AuditoriaTransaccional> findByEntidadAfectadaAndEntidadIdOrderByFechaDesc(
        String entidadAfectada,
        String entidadId,
        Pageable paginacion
    );

    /**
     * Cambios agrupados por servicio de origen en un período dado.
     * Permite analizar la actividad de cada microservicio.
     */
    @Query("""
        SELECT a FROM AuditoriaTransaccional a
        WHERE (:servicioOrigen IS NULL OR a.servicioOrigen = :servicioOrigen)
          AND (:desde IS NULL OR a.fecha >= :desde)
          AND (:hasta IS NULL OR a.fecha <= :hasta)
        ORDER BY a.fecha DESC
        """)
    Page<AuditoriaTransaccional> buscarConFiltros(
        @Param("servicioOrigen") String servicioOrigen,
        @Param("desde")          LocalDateTime desde,
        @Param("hasta")          LocalDateTime hasta,
        Pageable paginacion
    );
}
