package com.suscripciones.dominio.excepciones;

import com.libreria.comun.excepciones.ExcepcionRecursoNoEncontrado;
import java.util.UUID;

/**
 * Excepción lanzada cuando no se encuentra una suscripción.
 */
public class ExcepcionSuscripcionNoEncontrada extends ExcepcionRecursoNoEncontrado {
    
    public ExcepcionSuscripcionNoEncontrada(UUID id) {
        super("Suscripción", id);
    }
}
