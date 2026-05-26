package com.cliente.aplicacion.excepciones;

import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentran los datos personales.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
public class DatosPersonalesNoEncontradosException extends ExcepcionRecursoNoEncontrado {
    public DatosPersonalesNoEncontradosException(UUID usuarioId) {
        super("los datos personales", usuarioId);
    }
}
