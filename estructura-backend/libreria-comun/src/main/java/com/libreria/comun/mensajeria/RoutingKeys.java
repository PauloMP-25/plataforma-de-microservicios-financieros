package com.libreria.comun.mensajeria;

/**
 * Claves de enrutamiento (Routing Keys).
 * Define patrones para Topic Exchanges.
 */
public final class RoutingKeys {
    // Auditoría
    public static final String AUDITORIA_ACCESO_ALL = "auditoria.acceso.#";
    public static final String AUDITORIA_EVENTO_ALL = "auditoria.evento.#";
    public static final String AUDITORIA_TRANSACCION_ALL = "auditoria.transaccion.#";

    // Claves exactas para Direct Exchange (DLQ)
    public static final String DLQ_AUDITORIA_ACCESO = "dlq.auditoria.accesos";
    public static final String DLQ_AUDITORIA_EVENTO = "dlq.auditoria.eventos";
    public static final String DLQ_AUDITORIA_TRANSACCIONAL = "dlq.auditoria.transacciones";
    
    // IA
    public static final String IA_ANALISIS_SOLICITAR = "ia.analisis.solicitar";
    public static final String IA_ANALISIS_RESULTADO = "ia.analisis.resultado";

    // Mensajería
    public static final String MENSAJERIA_OTP_GENERAR = "mensaje.otp.generar";

    // Sincronización Cliente → IA
    public static final String CLIENTE_PERFIL_ACTUALIZADO = "cliente.perfil.actualizado";

    // Pagos
    public static final String PAGO_EXITOSO = "pago.suscripcion.exitoso";
    public static final String PAGO_REEMBOLSO = "pago.suscripcion.reembolso";
    public static final String PAGO_FALLIDO = "pago.suscripcion.fallido";

    private RoutingKeys() {
    }
}
