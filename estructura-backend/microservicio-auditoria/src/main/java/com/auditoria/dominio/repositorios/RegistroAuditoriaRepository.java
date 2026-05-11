package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repositorio para la gestión de persistencia de la entidad
 * {@link RegistroAuditoria}.
 * <p>
 * Proporciona los métodos estándar de CRUD mediante Spring Data JPA y define
 * consultas
 * especializadas para la explotación de datos de auditoría, permitiendo
 * filtrado
 * dinámico y paginación sobre los eventos registrados.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, UUID> {

    /**
     * Realiza una búsqueda paginada de registros de auditoría permitiendo un
     * filtrado opcional por módulo.
     * <p>
     * La consulta utiliza una lógica de filtrado flexible: si el parámetro
     * {@code modulo} es {@code null}, se ignorará la condición y se retornarán
     * todos los registros.
     * La comparación no distingue entre mayúsculas y minúsculas (case-insensitive).
     * </p>
     * 
     * @param modulo     Nombre del módulo por el cual filtrar (opcional).
     * @param paginacion Objeto que contiene la configuración de paginación y
     *                   ordenamiento.
     * @return Una página de {@link RegistroAuditoria} que coinciden con los
     *         criterios,
     *         ordenados por fecha de creación en forma descendente.
     */
    @Query("""
            SELECT a FROM RegistroAuditoria a
            WHERE (:modulo IS NULL OR LOWER(a.modulo) = LOWER(:modulo))
            ORDER BY a.fechaHora DESC
            """)
    Page<RegistroAuditoria> buscarPorFiltros(
            @Param("modulo") String modulo,
            Pageable paginacion);
}
