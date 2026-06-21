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
    public static final String CLIENTE_IA_SINCRO = "cola.cliente.ia.sincronizacion";

    public static final String IA_PROCESAMIENTO_DLQ = "cola.ia.procesamiento.dlq";
    public static final String IA_CLASIFICACION = "q.ia.clasificacion";
    public static final String IA_INVALIDACION_CACHE = "cola.ia.invalidacion.cache";
    public static final String DASHBOARD_MODULOS = "cola.dashboard.modulos";

    // Mensajería
    public static final String MENSAJERIA_OTP_GENERAR = "cola.mensajeria.otp.generar";
    public static final String MENSAJERIA_ERROR = "cola.mensajeria.error";
    public static final String MENSAJERIA_EMAIL_ENVIAR = "cola.mensajeria.email.enviar";
    public static final String MENSAJERIA_SMS_ENVIAR = "cola.mensajeria.sms.enviar";

    // Pagos
    public static final String PAGOS_EXITOSOS = "cola.pagos.exitosos";
    public static final String PAGOS_EXITOSOS_USUARIO = "cola.usuario.pagos.exitosos";
    public static final String PAGOS_EXITOSOS_MENSAJERIA = "cola.mensajeria.pagos.exitosos";
    public static final String PAGOS_EXITOSOS_FINANCIERO = "cola.financiero.pagos.exitosos";
    public static final String PAGOS_EXITOSOS_AUDITORIA = "cola.auditoria.pagos.exitosos";
    public static final String PAGOS_REEMBOLSO = "cola.pagos.reembolso";

    // Financiero -> Cliente
    public static final String FINANCIERO_TRANSACCIONES_CLIENTE = "cola.cliente.transacciones.registradas";

    private NombresCola() {
    }
}
