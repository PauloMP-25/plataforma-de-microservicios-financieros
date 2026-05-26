package com.cliente.aplicacion.excepciones;

import com.libreria.comun.excepciones.ExcepcionValidacion;
import java.util.List;

/**
 * Excepción lanzada cuando ocurre un error de validación en el límite de gasto.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
public class LimiteGastoException extends ExcepcionValidacion {
    public LimiteGastoException(String mensaje) {
        super(mensaje, List.of(mensaje));
    }
}