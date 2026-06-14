package com.suscripciones.dominio.repositorios;

import com.suscripciones.dominio.entidades.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad Suscripcion.
 * Incluye soporte para JPA Specifications para búsquedas complejas.
 */
@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, UUID>, JpaSpecificationExecutor<Suscripcion> {
    
    List<Suscripcion> findByUsuarioId(UUID usuarioId);
    
    List<Suscripcion> findByEstado(String estado);
}
