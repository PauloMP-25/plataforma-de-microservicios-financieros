package com.pagos.aplicacion.servicios;

import com.pagos.aplicacion.dtos.ResumenPagosDTO;
import com.pagos.dominio.entidades.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Interfaz para la gestión administrativa del módulo de pagos.
 */
public interface IServicioAdminPagos {

    /**
     * Obtiene un resumen consolidado de todos los pagos.
     * @return DTO con estadísticas clave.
     */
    ResumenPagosDTO obtenerResumenGeneral();

    /**
     * Lista todos los pagos con soporte para paginación.
     * @param pageable Parámetros de paginación.
     * @return Página de registros de pago.
     */
    Page<Pago> listarTodosLosPagos(Pageable pageable);

    /**
     * Busca un pago específico por su ID de transacción.
     * @param pagoId ID interno del pago.
     * @return Entidad pago.
     */
    Pago buscarPagoPorId(UUID pagoId);

    /**
     * Permite a un administrador forzar el estado de un pago (ej. Conciliación manual).
     * @param pagoId ID del pago.
     * @param nuevoEstado Estado a aplicar.
     */
    void actualizarEstadoManual(UUID pagoId, String nuevoEstado);
}
