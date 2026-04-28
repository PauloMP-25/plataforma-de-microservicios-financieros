package com.mensajeria.aplicacion.excepciones;


public class LimiteCodigosExcedidoException extends RuntimeException {
    public LimiteCodigosExcedidoException(String mensaje) {
        super(mensaje);
    }
}
