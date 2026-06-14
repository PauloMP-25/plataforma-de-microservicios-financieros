package com.pagos.aplicacion.puertos;

import com.libreria.comun.respuesta.Paginacion;
import com.pagos.aplicacion.dtos.ResumenPagosDTO;
import com.pagos.dominio.entidades.Pago;

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
     * @param pagina Número de página.
     * @param tamanio Tamaño de página.
     * @return Paginación de registros de pago.
     */
    Paginacion<Pago> listarTodosLosPagos(int pagina, int tamanio);

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
