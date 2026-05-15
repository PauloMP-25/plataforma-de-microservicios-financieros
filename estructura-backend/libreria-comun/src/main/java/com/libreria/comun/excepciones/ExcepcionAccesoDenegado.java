package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;

/**
 * Excepción lanzada cuando un usuario autenticado intenta acceder a un recurso
 * del cual no es propietario o no tiene permisos suficientes.
 * <p>Mapea a un estado HTTP 403 Forbidden.</p>
 * 
 * @author Paulo Moron
 */
public class ExcepcionAccesoDenegado extends ExcepcionGlobal {

    public ExcepcionAccesoDenegado() {
        super(CodigoError.ACCESO_DENEGADO, 
              "No tiene permisos para realizar esta acción sobre este recurso.", 
              null);
    }
}
