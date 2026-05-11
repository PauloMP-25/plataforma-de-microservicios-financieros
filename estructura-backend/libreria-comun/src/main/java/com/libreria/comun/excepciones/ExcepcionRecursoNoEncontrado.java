package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * Excepción lanzada cuando un recurso solicitado no existe en la base de datos.
 * <p>Mapea directamente a un estado HTTP 404 Not Found.</p>
 */
public class ExcepcionRecursoNoEncontrado extends ExcepcionGlobal {
    
    /**
     * @param recurso Nombre de la entidad (ej. "Usuario").
     * @param id      Identificador que no pudo ser hallado.
     */
    public ExcepcionRecursoNoEncontrado(String recurso, Object id) {
        super(CodigoError.RECURSO_NO_ENCONTRADO, 
              String.format("No se encontró %s con identificador: %s", recurso, id), 
              Map.of("recurso", recurso, "id", id.toString()));
    }
}
