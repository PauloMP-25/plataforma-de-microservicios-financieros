package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /**
     * Recupera el c+¦digo OTP m+ís reciente, no usado, para un usuarioId + tipo.
     * Usado en la validaci+¦n para evitar trabajar con c+¦digos obsoletos.
     * @param usuarioId
     * @param tipo
     * @return 
     */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
            UUID usuarioId,
            CodigoVerificacion.TipoVerificacion tipo
    );

    /**
     * Limpieza programada cada 24 horas: elimina registros expirados.
     * Solo borra los NO usados para no perder el historial de los ya consumidos.
     * @param fecha
     * @return 
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CodigoVerificacion c WHERE c.fechaExpiracion < :fecha AND c.usado = false")
    int eliminarCodigosExpirados(@Param("fecha") LocalDateTime fecha);
}
