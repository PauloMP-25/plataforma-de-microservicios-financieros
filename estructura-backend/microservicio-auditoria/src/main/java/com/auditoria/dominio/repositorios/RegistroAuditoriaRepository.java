package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.RegistroAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

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
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, UUID>,
                org.springframework.data.jpa.repository.JpaSpecificationExecutor<RegistroAuditoria> {
}
