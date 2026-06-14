package com.suscripciones.dominio.repositorios;

import com.suscripciones.dominio.entidades.ClaveIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA para la entidad ClaveIdempotencia.
 */
@Repository
public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotencia, String> {
}
