package com.suscripciones.dominio.excepciones;

import com.libreria.comun.excepciones.ExcepcionGlobal;
import com.libreria.comun.enums.CodigoError;
import java.util.Map;
import java.util.UUID;

/**
 * Excepción lanzada al intentar cancelar una suscripción que ya se encuentra cancelada.
 */
public class ExcepcionSuscripcionYaCancelada extends ExcepcionGlobal {

    public ExcepcionSuscripcionYaCancelada(UUID id) {
        super(CodigoError.CONFLICTO_DE_DATOS,
                String.format("La suscripción con ID %s ya está en estado CANCELADA.", id),
                Map.of("suscripcionId", id.toString(), "estadoActual", "CANCELADA"));
    }
}
