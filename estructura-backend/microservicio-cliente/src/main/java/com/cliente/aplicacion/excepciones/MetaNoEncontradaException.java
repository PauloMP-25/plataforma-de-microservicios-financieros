package com.cliente.aplicacion.excepciones;

import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra una meta de ahorro.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
public class MetaNoEncontradaException extends ExcepcionRecursoNoEncontrado {
    public MetaNoEncontradaException(UUID metaId) {
        super("la meta de ahorro", metaId);
    }
}