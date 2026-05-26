package com.mensajeria.aplicacion.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * Excepción lanzada cuando un usuario excede el límite diario de solicitudes de
 * códigos OTP para un propósito determinado. El manejador global de la librería
 * la captura y devuelve un HTTP 429 con el código semántico
 * {@code LIMITE_DIARIO_EXCEDIDO}.
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public class LimiteCodigosExcedidoException extends ExcepcionGlobal {

    /**
     * Construye la excepción con un mensaje descriptivo para el usuario final.
     *
     * @param mensaje Descripción legible del límite superado, incluyendo cuándo
     *                podrá reintentar el usuario.
     */
    public LimiteCodigosExcedidoException(String mensaje) {
        super(CodigoError.LIMITE_DIARIO_EXCEDIDO, mensaje, null);
    }
}

