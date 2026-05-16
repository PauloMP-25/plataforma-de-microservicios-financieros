package com.pagos.aplicacion.servicios;

import com.pagos.dominio.entidades.Pago;

/**
 * Puerto para la publicación de eventos de pago hacia otros microservicios.
 */
public interface IPublicadorPagos {

    /**
     * Notifica al sistema que un pago se ha completado con éxito.
     * 
     * @param pago Entidad de pago completada.
     */
    void publicarPagoExitoso(Pago pago);

    /**
     * Notifica al sistema que un pago ha fallado o expirado.
     * 
     * @param pago Entidad de pago fallida.
     */
    void publicarPagoFallido(Pago pago);
}
