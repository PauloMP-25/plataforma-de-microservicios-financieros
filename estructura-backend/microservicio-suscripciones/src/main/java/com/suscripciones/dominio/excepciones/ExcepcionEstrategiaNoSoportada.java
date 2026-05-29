package com.suscripciones.dominio.excepciones;

import com.libreria.comun.excepciones.ExcepcionGlobal;
import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * Excepción lanzada cuando se intenta usar una estrategia de fechas de pago no registrada.
 */
public class ExcepcionEstrategiaNoSoportada extends ExcepcionGlobal {

    public ExcepcionEstrategiaNoSoportada(String estrategia) {
        super(CodigoError.ERROR_VALIDACION,
                String.format("La estrategia de cálculo de fecha '%s' no está soportada.", estrategia),
                Map.of("estrategia", estrategia));
    }
}
