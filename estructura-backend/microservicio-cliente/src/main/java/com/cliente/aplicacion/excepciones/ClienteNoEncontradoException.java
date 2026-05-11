package com.cliente.aplicacion.excepciones;

import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra un cliente.
 * 
 * @author Paulo Moron
 * @version 1.1.0
 */
public class ClienteNoEncontradoException extends ExcepcionRecursoNoEncontrado {
    public ClienteNoEncontradoException(UUID usuarioId) {
        super("el cliente", usuarioId);
    }
}
