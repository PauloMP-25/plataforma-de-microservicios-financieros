package com.libreria.comun.mensajeria;

/**
 * Nombres de las Colas físicas en RabbitMQ.
 */
public final class NombresCola {
    public static final String AUDITORIA_ACCESOS = "cola.auditoria.accesos";
    public static final String AUDITORIA_TRANSACCIONES = "cola.auditoria.transacciones";
    public static final String AUDITORIA_ACCESOS_DLQ = "cola.auditoria.accesos.dlq";
    public static final String IA_PROCESAMIENTO = "cola.ia.procesamiento";
    public static final String DASHBOARD_CONSEJOS = "cola.dashboard.consejos";
    public static final String MENSAJERIA_OTP = "cola.mensajeria.otp";

    private NombresCola() {}
}
