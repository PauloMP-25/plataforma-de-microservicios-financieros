package com.auditoria.aplicacion.servicios;

import com.auditoria.aplicacion.dtos.RespuestaAuditoriaDetalladoDTO;
import com.libreria.comun.dtos.EventoAuditoriaDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz de servicio para el registro y consulta de eventos de auditoría
 * general.
 * <p>
 * Define el contrato para persistir trazas de actividad de usuario y
 * realizar consultas históricas filtradas para el ecosistema Luka App.
 * </p>
 * 
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioRegistroAuditoria {

    /**
     * Persiste un nuevo evento de auditoría en la base de datos.
     * <p>
     * Se utiliza {@link EventoAuditoriaDTO} como contrato unificado para
     * capturar la actividad proveniente de cualquier microservicio.
     * </p>
     * 
     * @param request Datos del evento (acción, módulo, IP, etc.).
     * @return {@link EventoAuditoriaDTO} con los datos persistidos e ID generado.
     */
    EventoAuditoriaDTO registrarEvento(EventoAuditoriaDTO request);

    /**
     * Recupera el historial de auditoría enriquecido con datos de usuario.
     * 
     * @param modulo     Filtro opcional por microservicio.
     * @param paginacion Metadatos de paginación.
     * @return Página de DTOs detallados para la interfaz de usuario.
     */
    Page<RespuestaAuditoriaDetalladoDTO> listarRegistrosDetallados(String modulo, Pageable paginacion);
}