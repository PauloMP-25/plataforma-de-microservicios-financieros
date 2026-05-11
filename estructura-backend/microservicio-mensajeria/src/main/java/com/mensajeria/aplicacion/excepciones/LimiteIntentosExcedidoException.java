package com.mensajeria.aplicacion.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * Excepción lanzada por el {@code ServicioThrottling} cuando un usuario supera
 * el límite máximo de intentos de envío por canal (EMAIL o SMS).
 * <p>
 * El manejador global de la librería la captura automáticamente y devuelve
 * HTTP 429 con el código semántico {@code LIMITE_DIARIO_EXCEDIDO}.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public class LimiteIntentosExcedidoException extends ExcepcionGlobal {

    /**
     * Construye la excepción indicando el canal bloqueado para informar al usuario
     * qué medio de notificación ha quedado suspendido hasta la medianoche.
     *
     * @param canal El canal de notificación bloqueado (ej. {@code "email"} o
     *              {@code "sms"}).
     */
    public LimiteIntentosExcedidoException(String canal) {
        super(
            CodigoError.LIMITE_DIARIO_EXCEDIDO,
            String.format(
                "Ha superado el límite de intentos de envío por %s. El contador se reinicia a las 00:00:00.",
                canal.toUpperCase()
            ),
            null
        );
    }
}
