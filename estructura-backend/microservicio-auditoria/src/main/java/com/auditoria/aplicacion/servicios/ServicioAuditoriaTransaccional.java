package com.auditoria.aplicacion.servicios;

import com.libreria.comun.dtos.EventoTransaccionalDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestión de auditorías transaccionales.
 * <p>
 * Define las operaciones necesarias para persistir y consultar los cambios
 * de estado en las entidades de negocio del ecosistema.
 * </p>
 * 
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioAuditoriaTransaccional {

    /**
     * Persiste un nuevo evento de cambio transaccional.
     * 
     * @param evento Datos del cambio (valor anterior, nuevo, entidad, etc.).
     * @return El DTO del evento persistido.
     */
    EventoTransaccionalDTO guardarEvento(EventoTransaccionalDTO evento);

    /**
     * Obtiene el historial de cambios de un usuario específico de forma paginada.
     * 
     * @param usuarioId ID del usuario.
     * @param pageable Configuración de paginación.
     * @return Página de eventos transaccionales.
     */
    Page<EventoTransaccionalDTO> listarPorUsuario(UUID usuarioId, Pageable pageable);

    /**
     * Filtra eventos transaccionales por microservicio y rango de fechas.
     * 
     * @param servicio Nombre del servicio de origen.
     * @param desde Fecha inicial.
     * @param hasta Fecha final.
     * @param pageable Configuración de paginación.
     * @return Página de eventos filtrados.
     */
    Page<EventoTransaccionalDTO> buscarConFiltros(String servicio, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}