package com.libreria.comun.excepciones;

import com.libreria.comun.enums.CodigoError;
import java.util.Map;

/**
 * Excepción lanzada cuando ocurre un error en la comunicación con un servicio externo
 * (ej. Fallo en el microservicio de IA, mensajería o API de terceros).
 * <p>Mapea a un estado HTTP 502 Bad Gateway.</p>
 * 
 * @author Paulo Moron
 */
public class ExcepcionServicioExterno extends ExcepcionGlobal {

    /**
     * @param servicio Nombre del servicio que falló.
     * @param razon    Detalle técnico del error devuelto por el servicio externo.
     */
    public ExcepcionServicioExterno(String servicio, String razon) {
        super(CodigoError.ERROR_SERVICIO_EXTERNO, 
              "Error crítico al invocar el servicio: " + servicio, 
              Map.of("servicio", servicio, "razon", razon));
    }
}
