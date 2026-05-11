package com.nucleo.financiero.dominio.repositorios;

import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import com.nucleo.financiero.dominio.entidades.Transaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para la gestión persistente de transacciones financieras.
 * <p>
 * Contiene consultas personalizadas (JPQL) para el filtrado dinámico de historial
 * y el cálculo de agregados financieros (sumas y conteos) por periodos de tiempo.
 * </p>
 * 
 * @author Luka-Dev-Backend
 * @version 1.1.0
 */
@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, UUID> {

    List<Transaccion> findTop10ByUsuarioIdOrderByFechaTransaccionDesc(UUID usuarioId);
    
    @Query("""
        SELECT t FROM Transaccion t
        JOIN FETCH t.categoria c
        WHERE t.usuarioId = :usuarioId
          AND (CAST(:tipo AS string) IS NULL OR t.tipo = :tipo)
          AND (CAST(:categoriaId AS uuid) IS NULL OR c.id = :categoriaId)
          AND (CAST(:desde AS localdatetime) IS NULL OR t.fechaTransaccion >= :desde)
          AND (CAST(:hasta AS localdatetime) IS NULL OR t.fechaTransaccion <= :hasta)
        ORDER BY t.fechaTransaccion DESC
        """)
    Page<Transaccion> buscarConFiltros(
            @Param("usuarioId") UUID usuarioId,
            @Param("tipo") TipoMovimiento tipo,
            @Param("categoriaId") UUID categoriaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable paginacion
    );

    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM Transaccion t
        WHERE t.usuarioId = :usuarioId
          AND t.tipo = 'INGRESO'
          AND t.fechaTransaccion BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarIngresosPorPeriodo(
            @Param("usuarioId") UUID usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT COALESCE(SUM(t.monto), 0)
        FROM Transaccion t
        WHERE t.usuarioId = :usuarioId
          AND t.tipo = 'GASTO'
          AND t.fechaTransaccion BETWEEN :desde AND :hasta
        """)
    BigDecimal sumarGastosPorPeriodo(
            @Param("usuarioId") UUID usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaccion t
        WHERE t.usuarioId = :usuarioId
          AND t.tipo = :tipo
          AND t.fechaTransaccion BETWEEN :desde AND :hasta
        """)
    long contarPorTipoYPeriodo(
            @Param("usuarioId") UUID usuarioId,
            @Param("tipo") TipoMovimiento tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
