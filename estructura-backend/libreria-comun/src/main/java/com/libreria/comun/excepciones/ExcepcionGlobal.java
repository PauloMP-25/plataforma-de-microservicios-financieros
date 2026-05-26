package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * Clase base para todas las excepciones personalizadas del ecosistema LUKA APP.
 * <p>
 * Proporciona la estructura necesaria para que el Manejador Global pueda
 * transformar cualquier error de negocio en un objeto {@code ResultadoApi} consistente.
 * </p>
 */
@Getter
public abstract class ExcepcionGlobal extends RuntimeException {
    
    /** Etiqueta única del error (ej. LUKA-404-USR). */
    private final CodigoError error;
    
    /** Mensaje amigable para el usuario. */
    private final String mensaje;
    
    /** Información técnica adicional o contexto del error. */
    private final Object detalles;
    
    /** Marca de tiempo de la ocurrencia. */
    private final String marcaTiempo;

    /**
     * Constructor de la excepción base.
     * 
     * @param error    Código identificador del error.
     * @param mensaje  Descripción en español.
     * @param detalles Objeto con información extra (puede ser null).
     */
    protected ExcepcionGlobal(CodigoError error, String mensaje, Object detalles) {
        super(mensaje);
        this.error = error;
        this.mensaje = mensaje;
        this.detalles = detalles;
        this.marcaTiempo = LocalDateTime.now().toString();
    }
}
