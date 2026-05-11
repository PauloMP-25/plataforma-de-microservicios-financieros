package com.auditoria.dominio.repositorios;

import com.auditoria.dominio.entidades.AuditoriaAcceso;
import com.libreria.comun.enums.EstadoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repositorio para gestionar la persistencia y análisis de intentos de acceso.
 * Proporciona herramientas clave para la detección de ataques y mantenimiento
 * de logs.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 * @since 2026-05-10
 */
public interface AuditoriaAccesoRepository extends JpaRepository<AuditoriaAcceso, UUID> {

    /**
     * Cuenta intentos por IP, estado y tiempo. Base para la detección de fuerza
     * bruta.
     * 
     * @param ipOrigen IP a evaluar.
     * @param estado   Estado del evento (comúnmente FALLO).
     * @param desde    Inicio de la ventana temporal de evaluación.
     * @return Total de intentos registrados en el periodo.
     */
    @Query("""
            SELECT COUNT(a) FROM AuditoriaAcceso a
            WHERE a.ipOrigen = :ipOrigen
              AND a.estado = :estado
              AND a.fecha >= :desde
            """)
    long contarIntentosPorIpYEstadoDesde(
            @Param("ipOrigen") String ipOrigen,
            @Param("estado") EstadoEvento estado,
            @Param("desde") LocalDateTime desde);

    /**
     * Recupera el historial de accesos de un usuario ordenado por fecha
     * descendente.
     * 
     * @param usuarioId  Identificador del usuario.
     * @param paginacion Configuración de página y orden.
     * @return Página de resultados de acceso.
     */
    Page<AuditoriaAcceso> findByUsuarioIdOrderByFechaDesc(UUID usuarioId, Pageable paginacion);

    /**
     * Recupera el historial de accesos por IP para auditorías de seguridad forense.
     * 
     * @param ipOrigen   Dirección IP de origen.
     * @param paginacion Configuración de página y orden.
     * @return Página de resultados por IP.
     */
    Page<AuditoriaAcceso> findByIpOrigenOrderByFechaDesc(String ipOrigen, Pageable paginacion);

    /**
     * Elimina registros antiguos para optimizar el almacenamiento de la tabla.
     * 
     * @param umbral Fecha límite para la depuración de datos.
     * @return Cantidad de registros eliminados.
     */
    @Query("DELETE FROM AuditoriaAcceso a WHERE a.fecha < :umbral")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    int eliminarRegistrosAnterioresA(@Param("umbral") LocalDateTime umbral);
}