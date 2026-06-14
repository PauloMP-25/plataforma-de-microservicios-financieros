package com.suscripciones.dominio.repositorios;

import com.suscripciones.dominio.entidades.BandejaSalida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio JPA para la bandeja de salida (Outbox) de eventos de suscripción.
 */
@Repository
public interface BandejaSalidaRepository extends JpaRepository<BandejaSalida, UUID> {
    
    List<BandejaSalida> findByProcesadoFalseOrderByFechaCreacionAsc();
}
