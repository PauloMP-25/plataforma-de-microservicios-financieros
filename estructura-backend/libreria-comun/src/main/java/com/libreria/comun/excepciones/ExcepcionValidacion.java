package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.List;

/**
 * Excepción lanzada cuando los datos de entrada no cumplen con las reglas de negocio
 * o las restricciones de validación (@Valid).
 * <p>Mapea a un estado HTTP 400 Bad Request.</p>
 * 
 * @author Paulo Moron
 */
public class ExcepcionValidacion extends ExcepcionGlobal {

    /**
     * @param mensaje  Descripción general del error de validación.
     * @param errores Lista de campos o motivos específicos del fallo.
     */
    public ExcepcionValidacion(String mensaje, List<String> errores) {
        super(CodigoError.ERROR_VALIDACION, mensaje, errores);
    }
}
