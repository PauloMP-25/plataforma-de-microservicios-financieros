package com.mensajeria.aplicacion.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * Excepción interna lanzada cuando la sincronización con el ms-cliente falla.
 * <p>
 * Esta excepción es capturada en la capa de servicio (patrón Fallback) para
 * registrar el evento sin propagar el error al usuario final. No debe llegar
 * al {@code ManejadorGlobalExcepciones}.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public class SincronizacionPendienteException extends ExcepcionGlobal {

    /**
     * Construye la excepción indicando el identificador del usuario cuya
     * sincronización quedó pendiente para reintento posterior.
     *
     * @param usuarioId Identificador UUID del usuario afectado, incluido en el
     *                  mensaje para facilitar la trazabilidad en logs.
     */
    public SincronizacionPendienteException(String usuarioId) {
        super(
            CodigoError.ERROR_SERVICIO_EXTERNO,
            String.format(
                "Sincronización de teléfono pendiente para usuario %s. El ms-cliente no respondió.",
                usuarioId
            ),
            null
        );
    }
}
