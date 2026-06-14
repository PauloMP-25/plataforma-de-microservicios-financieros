package com.suscripciones.dominio.repositorios;

import com.suscripciones.dominio.entidades.HistorialPagoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la entidad HistorialPagoSuscripcion.
 */
@Repository
public interface HistorialPagoSuscripcionRepository extends JpaRepository<HistorialPagoSuscripcion, UUID> {
    
    List<HistorialPagoSuscripcion> findBySuscripcionId(UUID suscripcionId);
    
    List<HistorialPagoSuscripcion> findByTransaccionId(UUID transaccionId);
}
