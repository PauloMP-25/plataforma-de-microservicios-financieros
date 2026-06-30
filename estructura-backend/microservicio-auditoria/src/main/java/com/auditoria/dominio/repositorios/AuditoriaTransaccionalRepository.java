package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.AuditoriaTransaccional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.UUID;

/**
 * Repositorio para la gestión y consulta de trazabilidad transaccional.
 * Permite reconstruir el historial de cambios en las entidades de negocio del
 * sistema.
 * 
 * @version 1.2.0
 */
public interface AuditoriaTransaccionalRepository extends JpaRepository<AuditoriaTransaccional, UUID>, JpaSpecificationExecutor<AuditoriaTransaccional> {

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
            UUID entidadId,
            Pageable paginacion);

    /**
     * Recupera el registro transaccional más reciente de un usuario y entidad afectada.
     * Utilizado para resolver dinámicamente el valor anterior de un plan financiero.
     * 
     * @param usuarioId        Identificador del usuario.
     * @param entidadAfectada  Nombre de la entidad afectada.
     * @return El registro transaccional opcional más reciente.
     */
    java.util.Optional<AuditoriaTransaccional> findFirstByUsuarioIdAndEntidadAfectadaOrderByFechaDesc(
            UUID usuarioId,
            String entidadAfectada);
}