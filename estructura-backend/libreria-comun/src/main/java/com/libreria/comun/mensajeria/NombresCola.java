package com.libreria.comun.mensajeria;

/**
 * Nombres de las Colas físicas en RabbitMQ.
 */
public final class NombresCola {
    public static final String AUDITORIA_ACCESOS = "cola.auditoria.accesos";
    public static final String AUDITORIA_EVENTOS = "cola.auditoria.eventos";
    public static final String AUDITORIA_TRANSACCIONES = "cola.auditoria.transacciones";
    public static final String AUDITORIA_ACCESOS_DLQ = "cola.auditoria.accesos.dlq";
    public static final String AUDITORIA_EVENTOS_DLQ = "cola.auditoria.eventos.dlq";
    public static final String AUDITORIA_TRANSACCIONES_DLQ = "cola.auditoria.transacciones.dlq";
    public static final String IA_PROCESAMIENTO = "cola.ia.procesamiento";
    public static final String IA_SINCRONIZACION_CONTEXTO = "cola.ia.sincronizacion.contexto";
    public static final String IA_SINCRONIZACION_ERROR = "cola.ia.sincronizacion.error";
    public static final String DASHBOARD_CONSEJOS = "cola.dashboard.consejos";
    public static final String MENSAJERIA_OTP = "cola.mensajeria.otp";

    private NombresCola() {
    }
}
