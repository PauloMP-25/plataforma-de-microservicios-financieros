package com.pagos.dominio.repositorios;

import com.pagos.dominio.entidades.BandejaSalida;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la gestión de la bandeja de salida (Outbox).
 */
public interface RepositorioBandejaSalida extends JpaRepository<BandejaSalida, UUID> {
    
    /**
     * Busca eventos pendientes que no han superado el máximo de intentos.
     */
    List<BandejaSalida> findByProcesadoFalseAndIntentosLessThan(int maxIntentos);
}
