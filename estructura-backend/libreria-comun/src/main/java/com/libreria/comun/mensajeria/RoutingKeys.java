package com.libreria.comun.mensajeria;

/**
 * Claves de enrutamiento (Routing Keys).
 * Define patrones para Topic Exchanges.
 */
public final class RoutingKeys {
    // Auditoría
    public static final String AUDITORIA_ACCESO_ALL = "auditoria.acceso.#";
    public static final String AUDITORIA_TRANSACCION_ALL = "auditoria.transaccion.#";
    
    // IA
    public static final String IA_ANALISIS_SOLICITAR = "ia.analisis.solicitar";
    public static final String IA_ANALISIS_RESULTADO = "ia.analisis.resultado";

    // Mensajería
    public static final String MENSAJERIA_OTP_GENERAR = "mensaje.otp.generar";

    private RoutingKeys() {}
}
