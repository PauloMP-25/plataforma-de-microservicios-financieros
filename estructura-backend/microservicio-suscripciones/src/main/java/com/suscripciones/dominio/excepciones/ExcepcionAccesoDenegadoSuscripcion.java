package com.suscripciones.dominio.excepciones;

import com.libreria.comun.enums.CodigoError;
import com.libreria.comun.excepciones.ExcepcionGlobal;

/**
 * Excepción lanzada cuando un usuario autenticado intenta realizar operaciones
 * sobre suscripciones de las cuales no es el propietario.
 */
public class ExcepcionAccesoDenegadoSuscripcion extends ExcepcionGlobal {

    public ExcepcionAccesoDenegadoSuscripcion(String mensaje) {
        super(CodigoError.ACCESO_DENEGADO, mensaje, null);
    }
}
