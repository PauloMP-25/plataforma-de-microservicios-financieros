package com.pagos.dominio.repositorios;

import com.pagos.dominio.entidades.DetallePago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Repositorio para la gestión de detalles de pago.
 */
public interface RepositorioDetallePago extends JpaRepository<DetallePago, UUID> {

    @Query("SELECT SUM(d.monto) FROM DetallePago d WHERE d.pago.estado = 'COMPLETADO'")
    BigDecimal sumarIngresosTotales();
}
