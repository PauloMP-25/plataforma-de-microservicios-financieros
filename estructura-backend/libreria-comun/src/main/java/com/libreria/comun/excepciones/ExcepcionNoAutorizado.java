package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * Excepción lanzada cuando el usuario no proporciona credenciales válidas
 * o su token JWT ha expirado/es inválido.
 * <p>Mapea a un estado HTTP 401 Unauthorized.</p>
 * 
 * @author Paulo Moron
 */
public class ExcepcionNoAutorizado extends ExcepcionGlobal {

    /**
     * @param causa Razón específica del rechazo (ej. "TOKEN_EXPIRADO", "TOKEN_INVALIDO").
     */
    public ExcepcionNoAutorizado(String causa) {
        super(CodigoError.ACCESO_NO_AUTORIZADO, 
              "Acceso denegado: " + causa + ". Por favor, inicie sesión nuevamente.", 
              Map.of("causa", causa));
    }
}
