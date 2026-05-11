package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.AuditoriaTransaccional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repositorio para la gestión y consulta de trazabilidad transaccional.
 * Permite reconstruir el historial de cambios en las entidades de negocio del
 * sistema.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
public interface AuditoriaTransaccionalRepository extends JpaRepository<AuditoriaTransaccional, UUID> {

    /**
     * Recupera el historial de cambios realizados por un usuario específico.
     * 
     * @param usuarioId  Identificador del usuario.
     * @param paginacion Configuración de página y orden.
     * @return Página de registros transaccionales del usuario.
     */
    Page<AuditoriaTransaccional> findByUsuarioIdOrderByFechaDesc(UUID usuarioId, Pageable paginacion);

    /**
     * Recupera todos los cambios aplicados a una instancia de entidad específica.
     * 
     * @param entidadAfectada Nombre técnico de la entidad (ej: "Cliente").
     * @param entidadId       Identificador de la entidad modificada.
     * @param paginacion      Configuración de página y orden.
     * @return Historial cronológico de cambios de la entidad.
     */
    Page<AuditoriaTransaccional> findByEntidadAfectadaAndEntidadIdOrderByFechaDesc(
            String entidadAfectada,
            String entidadId,
            Pageable paginacion);

    /**
     * Búsqueda avanzada con filtros opcionales por servicio de origen y rango de
     * fechas.
     * 
     * @param servicioOrigen Nombre del microservicio (opcional).
     * @param desde          Fecha inicial del rango (opcional).
     * @param hasta          Fecha final del rango (opcional).
     * @param paginacion     Configuración de página y orden.
     * @return Resultados filtrados de la actividad transaccional.
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
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable paginacion);
}