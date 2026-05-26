package com.pagos.dominio.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * Excepción específica para errores relacionados con el procesamiento de pagos.
 */
public class ExcepcionPago extends ExcepcionGlobal {

    public ExcepcionPago(CodigoError error, String mensaje) {
        super(error, mensaje, null);
    }

    public ExcepcionPago(CodigoError error, String mensaje, Object detalles) {
        super(error, mensaje, detalles);
    }
}
