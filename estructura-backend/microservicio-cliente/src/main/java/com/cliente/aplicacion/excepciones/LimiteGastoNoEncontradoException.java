package com.cliente.aplicacion.excepciones;

import java.util.UUID;
import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;

/**
 * Excepción lanzada cuando no se encuentra un límite de gasto.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
public class LimiteGastoNoEncontradoException extends ExcepcionRecursoNoEncontrado {
    public LimiteGastoNoEncontradoException(UUID usuarioId) {
        super("el límite de gasto", usuarioId);
    }
}
