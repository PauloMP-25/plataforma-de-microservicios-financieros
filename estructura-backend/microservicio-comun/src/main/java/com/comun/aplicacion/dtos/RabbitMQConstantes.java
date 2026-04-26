package com.comun.aplicacion.dtos;

public class RabbitMQConstantes {
    // Exchanges
    public static final String EXCHANGE_AUDITORIA = "exchange.auditoria";
    public static final String EXCHANGE_AUDITORIA_DLQ = "exchange.auditoria.dlq";

    // Routing Keys (Patrones)
    public static final String RK_ACCESO = "auditoria.acceso.#";
    public static final String RK_TRANSACCION = "auditoria.transaccion.#";
    
    // Routing Keys (Específicas para envío)
    public static final String RK_ACCESO_LOGIN = "auditoria.acceso.login";
    public static final String RK_ACCESOS_DLQ = "dlq.auditoria.accesos";

    private RabbitMQConstantes() {
    }
}
