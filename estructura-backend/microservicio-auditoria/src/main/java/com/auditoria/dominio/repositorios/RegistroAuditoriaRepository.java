package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.RegistroAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, UUID> {

    /**
     * Filtra por módulo y/o nivel. Ambos parámetros son opcionales.
     * Si llegan como null, la condición se ignora con el truco (:param IS NULL OR ...).
     * @param modulo
     * @param paginacion
     * @return 
     */
    @Query("""
        SELECT a FROM RegistroAuditoria a
        WHERE (:modulo IS NULL OR LOWER(a.modulo) = LOWER(:modulo))
        ORDER BY a.fechaHora DESC
        """)
    Page<RegistroAuditoria> buscarPorFiltros(
        @Param("modulo") String modulo,
        Pageable paginacion
    );
}
