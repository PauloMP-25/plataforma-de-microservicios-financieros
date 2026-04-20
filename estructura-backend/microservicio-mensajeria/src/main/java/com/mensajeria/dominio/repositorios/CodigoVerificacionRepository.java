package com.mensajeria.dominio.repositorios;

import com.mensajeria.dominio.entidades.CodigoVerificacion;
import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo; // Importante
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    /**
     * Busca el código más reciente, no usado, filtrando por Usuario y
     * PROPÓSITO. Esto evita que un código de activación se use para resetear
     * password.
     * @param usuarioId
     * @param proposito
     * @return
     */
    Optional<CodigoVerificacion> findTopByUsuarioIdAndPropositoAndUsadoFalseOrderByFechaCreacionDesc(
            UUID usuarioId,
            PropositoCodigo proposito
    );

    /**
     * Limpieza profunda: Elimina códigos expirados Y códigos ya utilizados.
     * Esto mantiene la tabla ligera y rápida.
     * @param fecha
     * @return
     */
    @Modifying
    @Query("DELETE FROM CodigoVerificacion c WHERE c.fechaExpiracion < :fecha OR c.usado = true")
    int eliminarCodigosObsoletos(@Param("fecha") LocalDateTime fecha);

    /**
     * Busca un código específico en toda la tabla (sin importar el usuario aún). 
     * Esto es vital para el flujo de recuperación de contraseña.
     * @param codigo
     * @return 
     */
    Optional<CodigoVerificacion> findTopByCodigoAndUsadoFalseOrderByFechaCreacionDesc(String codigo);
}
