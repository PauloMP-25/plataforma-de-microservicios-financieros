package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la gestión de códigos de verificación OTP.
 * <p>
 * Extiende JpaSpecificationExecutor para soportar el Specification Pattern,
 * permitiendo auditorías y limpiezas dinámicas.
 * </p>
 * 
 * @author Paulo Moron
 * @version 1.2.0
 */
@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID>, JpaSpecificationExecutor<CodigoVerificacion> {

    /**
     * Busca el código más reciente, no usado, filtrando por Usuario y PROPÓSITO.
     */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(
            UUID usuarioId,
            PropositoCodigo proposito
    );

    Optional<CodigoVerificacion> findByIdAndCodigoAndUsadoFalse(UUID id, String codigo);
    
    /**
     * Limpieza profunda: Elimina códigos expirados Y códigos ya utilizados.
     */
    @Modifying
    @Query("DELETE FROM CodigoVerificacion c WHERE c.fechaExpiracion < :fecha OR c.usado = true")
    int eliminarCodigosObsoletos(@Param("fecha") LocalDateTime fecha);

    /**
     * Cuenta cuántos códigos tiene un usuario para un propósito.
     */
    long countByUsuarioIdAndPropositoAndUsadoFalse(UUID usuarioId, PropositoCodigo proposito);
    
    /**
     * Cuenta cuántos códigos ha solicitado un usuario para un propósito desde una fecha dada.
     */
    long countByUsuarioIdAndPropositoAndFechaCreacionAfter(UUID usuarioId, PropositoCodigo proposito, LocalDateTime fecha);
}
