package com.auditoria.aplicacion.servicios;

import com.libreria.comun.dtos.EventoAccesoDTO;
import com.libreria.comun.enums.EstadoEvento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestión de auditorías de acceso.
 * <p>
 * Se encarga exclusivamente de la persistencia, consulta y mantenimiento 
 * de los registros de inicio de sesión y actividad de usuarios.
 * </p>
 * 
 * @author Paulo Moron
 * @since 2026-05
 */
public interface ServicioAuditoriaAcceso {

    /**
     * Registra un nuevo intento de acceso (éxito o fallo).
     * 
     * @param dto Datos del intento de acceso.
     * @param EstadoAcceso Estado del login del usuario
     * @return El registro persistido en formato DTO.
     */
    EventoAccesoDTO registrarAcceso(EventoAccesoDTO dto, EstadoEvento estado);

    /**
     * Recupera una lista paginada de todos los accesos registrados.
     * 
     * @param paginacion Configuración de página y tamaño.
     * @return Página de registros de acceso.
     */
    Page<EventoAccesoDTO> listarTodo(Pageable paginacion);

    /**
     * Filtra los registros de acceso de un usuario específico.
     * 
     * @param usuarioId Identificador único del usuario.
     * @param paginacion Configuración de paginación.
     * @return Página de registros asociados al usuario.
     */
    Page<EventoAccesoDTO> listarPorUsuario(UUID usuarioId, Pageable paginacion);

    /**
     * Purga registros de acceso que superen la antigüedad permitida por la política de retención.
     * 
     * @param diasAntiguedad Cantidad de días hacia atrás para mantener.
     * @return Cantidad de registros eliminados.
     */
    int limpiarRegistrosAntiguos(int diasAntiguedad);
}