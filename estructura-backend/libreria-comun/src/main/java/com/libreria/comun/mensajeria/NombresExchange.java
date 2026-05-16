package com.libreria.comun.mensajeria;

/**
 * Nombres de los Exchanges (Intercambiadores) del ecosistema LUKA.
 * Centralizados para evitar discrepancias entre productores y consumidores.
 */
public final class NombresExchange {
    /** Exchange principal para eventos de auditoría y trazabilidad */
    public static final String AUDITORIA = "exchange.auditoria";
    /** Exchange para mensajes fallidos (Dead Letter Exchange) */
    public static final String AUDITORIA_DLX = "exchange.auditoria.dlq";
    /** Exchange para comunicación con el motor de Inteligencia Artificial */
    public static final String IA = "exchange.ia";
    /** Exchange para el envío de notificaciones y OTP */
    public static final String MENSAJERIA = "exchange.mensajeria";
    /** Exchange para actualizaciones en tiempo real hacia el Dashboard */
    public static final String DASHBOARD = "exchange.dashboard";
    /** Exchange para sincronización de cambios del cliente hacia consumidores (IA, Dashboard) */
    public static final String CLIENTE_ACTUALIZACIONES = "exchange.cliente.actualizaciones";
    /** Dead Letter Exchange para mensajes fallidos de sincronización de cliente */
    public static final String CLIENTE_ACTUALIZACIONES_DLX = "exchange.cliente.actualizaciones.dlq";
    /** Exchange para eventos de pago y suscripciones */
    public static final String PAGOS = "exchange.pagos";

    private NombresExchange() {} // Previene instanciación
}