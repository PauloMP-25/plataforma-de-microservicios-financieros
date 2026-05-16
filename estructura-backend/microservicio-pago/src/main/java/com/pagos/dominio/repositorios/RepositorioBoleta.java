package com.pagos.dominio.repositorios;

import com.pagos.dominio.entidades.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la gestión de boletas de pago.
 */
public interface RepositorioBoleta extends JpaRepository<Boleta, UUID>, JpaSpecificationExecutor<Boleta> {
    
    Optional<Boleta> findByCodigoBoleta(String codigoBoleta);
    
    Optional<Boleta> findByPagoId(UUID pagoId);
}
