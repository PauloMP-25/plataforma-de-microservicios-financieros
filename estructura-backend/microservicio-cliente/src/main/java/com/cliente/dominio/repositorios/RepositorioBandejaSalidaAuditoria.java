package com.cliente.dominio.repositorios;

import com.cliente.dominio.entidades.BandejaSalidaAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de base de datos para la bandeja de salida (Outbox) de eventos de auditoría del cliente.
 */
@Repository
public interface RepositorioBandejaSalidaAuditoria extends JpaRepository<BandejaSalidaAuditoria, UUID> {

    /**
     * Recupera todos los eventos de auditoría que aún no han sido procesados y
     * que no han superado el número máximo de reintentos.
     *
     * @param maxIntentos Límite máximo de reintentos.
     * @return Lista de eventos de auditoría pendientes de envío.
     */
    List<BandejaSalidaAuditoria> findByProcesadoFalseAndIntentosLessThan(int maxIntentos);
}
