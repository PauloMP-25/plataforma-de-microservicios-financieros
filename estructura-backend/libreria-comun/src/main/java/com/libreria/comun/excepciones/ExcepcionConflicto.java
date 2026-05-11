package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * Excepción lanzada cuando ocurre un conflicto con el estado actual del servidor.
 * <p>
 * Comúnmente utilizada para violaciones de unicidad en la base de datos (ej. DNI duplicado,
 * correo ya registrado). Mapea a un estado HTTP 409 Conflict.
 * </p>
 * 
 * @author Paulo Moron
 */
public class ExcepcionConflicto extends ExcepcionGlobal {
    
    /**
     * Construye una nueva excepción de conflicto.
     * 
     * @param campo Nombre del campo que causa la duplicidad (ej. "email").
     * @param valor Valor que ya existe en el sistema y causa la colisión.
     */
    public ExcepcionConflicto(String campo, String valor) {
        super(CodigoError.CONFLICTO_DE_DATOS, 
              String.format("El campo '%s' con valor '%s' ya existe en el sistema.", campo, valor), 
              Map.of("campo", campo, "valor", valor));
    }
}
