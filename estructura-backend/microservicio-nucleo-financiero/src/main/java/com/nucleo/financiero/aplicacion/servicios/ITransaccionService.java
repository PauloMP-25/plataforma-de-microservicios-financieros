package com.nucleo.financiero.aplicacion.servicios;

import com.nucleo.financiero.aplicacion.dtos.transacciones.ResumenFinancieroDTO;
import com.nucleo.financiero.aplicacion.dtos.transacciones.RespuestaTransaccion;
import com.nucleo.financiero.aplicacion.dtos.transacciones.SolicitudTransaccion;
import com.nucleo.financiero.dominio.entidades.Categoria.TipoMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Interfaz de servicio para la gestión de transacciones financieras.
 * <p>
 * Define el contrato para el registro, consulta y análisis de movimientos
 * financieros (Ingresos y Egresos).
 * </p>
 *
 * @author Luka-Dev-Backend
 * @version 1.1.0
 */
public interface ITransaccionService {

    /**
     * Registra una nueva transacción individual.
     *
     * @param request Datos de la transacción a registrar.
     * @param ipCliente Dirección IP de origen para auditoría.
     * @return DTO con los datos de la transacción persistida.
     */
    RespuestaTransaccion registrar(SolicitudTransaccion request, String ipCliente);

    /**
     * Registra un lote de transacciones en una sola operación atómica.
     *
     * @param solicitudes Lista de solicitudes de transacción.
     * @param ipCliente Dirección IP de origen.
     * @return Lista de DTOs de las transacciones registradas.
     */
    List<RespuestaTransaccion> registrarLote(List<SolicitudTransaccion> solicitudes, String ipCliente);

    /**
     * Consulta el historial de transacciones de un usuario con filtros.
     *
     * @param usuarioId ID del usuario.
     * @param tipo Filtro por tipo de movimiento (opcional).
     * @param categoriaId Filtro por categoría (opcional).
     * @param desde Fecha de inicio del rango (opcional).
     * @param hasta Fecha de fin del rango (opcional).
     * @param paginacion Parámetros de paginación.
     * @param ipCliente Dirección IP de origen.
     * @return Página de resultados de transacciones.
     */
    Page<RespuestaTransaccion> listarHistorial(
            UUID usuarioId, TipoMovimiento tipo, UUID categoriaId,
            LocalDateTime desde, LocalDateTime hasta, Pageable paginacion,
            String ipCliente);

    /**
     * Obtiene un resumen financiero consolidado por periodo.
     *
     * @param usuarioId ID del usuario.
     * @param mes Mes del resumen (1-12).
     * @param anio Año del resumen.
     * @param ipCliente Dirección IP de origen.
     * @return DTO con el resumen de ingresos, gastos y contadores.
     */
    ResumenFinancieroDTO obtenerResumen(UUID usuarioId, Integer mes, Integer anio, String ipCliente);

    /**
     * Busca una transacción por su identificador único.
     *
     * @param id UUID de la transacción.
     * @return DTO de la transacción encontrada.
     * @throws IllegalArgumentException Si la transacción no existe.
     */
    RespuestaTransaccion obtenerPorId(UUID id);
}
