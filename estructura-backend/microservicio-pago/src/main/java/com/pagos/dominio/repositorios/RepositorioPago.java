package com.pagos.dominio.repositorios;

import com.pagos.dominio.entidades.Pago;
import com.pagos.aplicacion.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Repositorio JPA para la gestión de pagos en la base de datos exclusiva del microservicio.
 * Incluye JpaSpecificationExecutor para soportar el patrón SpecFactory.
 */
public interface RepositorioPago extends JpaRepository<Pago, UUID>, JpaSpecificationExecutor<Pago> {

    Optional<Pago> findByStripeSessionId(String stripeSessionId);

    /** Consulta clave para garantizar idempotencia de webhooks. */
    boolean existsByStripeEventoId(String stripeEventoId);

    long countByEstado(EstadoPago estado);

    @Query("SELECT d.planSolicitado, COUNT(p) FROM Pago p JOIN p.detalles d WHERE p.estado = 'COMPLETADO' GROUP BY d.planSolicitado")
    List<Object[]> contarSuscripcionesActivasPorPlan();

    Optional<Pago> findFirstByUsuarioIdAndEstadoOrderByFechaCreacionDesc(UUID usuarioId, EstadoPago estado);
}
