package com.pagos.infraestructura.excepciones;

import com.libreria.comun.excepciones.ExcepcionServicioExterno;

/**
 * Excepción de infraestructura para errores de comunicación con la API de Mercado Pago.
 *
 * <p>Extiende {@link ExcepcionServicioExterno} de {@code libreria-comun} para integrarse
 * automáticamente con el manejador global de excepciones ({@code ManejadorGlobalExcepcionesBase})
 * y ser mapeada al {@code CodigoError.ERROR_SERVICIO_EXTERNO} (HTTP 502).</p>
 *
 */
public class ExcepcionMercadoPago extends ExcepcionServicioExterno {

    /**
     * Construye la excepción con el nombre de la operación fallida y el detalle del error.
     *
     * @param operacion Nombre de la operación de Mercado Pago que falló (ej: "crearPreapproval").
     * @param detalle   Mensaje descriptivo del error devuelto por la API o el SDK.
     */
    public ExcepcionMercadoPago(String operacion, String detalle) {
        super("Mercado Pago", "Error en operación [" + operacion + "]: " + detalle);
    }
}
