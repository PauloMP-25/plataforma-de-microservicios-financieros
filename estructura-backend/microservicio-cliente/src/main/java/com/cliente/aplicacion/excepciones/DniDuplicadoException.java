package com.cliente.aplicacion.excepciones;

import com.libreria.comun.excepciones.ExcepcionConflicto;

/**
 * Excepción lanzada cuando se intenta registrar un DNI ya existente.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
public class DniDuplicadoException extends ExcepcionConflicto {
    public DniDuplicadoException(String dni) {
        super("DNI", dni);
    }
}
