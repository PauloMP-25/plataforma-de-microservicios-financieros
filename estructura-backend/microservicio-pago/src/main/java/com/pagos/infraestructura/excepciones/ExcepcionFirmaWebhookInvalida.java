package com.pagos.infraestructura.excepciones;

/**
 * Excepción lanzada cuando la firma HMAC-SHA256 del webhook de Mercado Pago no es válida.
 *
 * <p>Actúa como barrera de seguridad crítica: previene el procesamiento de notificaciones
 * fraudulentas o manipuladas antes de que lleguen a la capa de negocio.
 * El {@code ManejadorGlobalExcepciones} la mapea al {@code CodigoError.ACCESO_DENEGADO}
 * (HTTP 403).</p>
 *
 * @author LUKA APP Team
 */
public class ExcepcionFirmaWebhookInvalida extends RuntimeException {

    /**
     * Construye la excepción con un mensaje estándar de firma inválida.
     */
    public ExcepcionFirmaWebhookInvalida() {
        super("La firma HMAC-SHA256 del webhook de Mercado Pago no es válida. " +
              "Posible intento de fraude o payload corrupto.");
    }

    /**
     * Construye la excepción con un mensaje personalizado.
     *
     * @param detalle Información adicional sobre el motivo del fallo de validación.
     */
    public ExcepcionFirmaWebhookInvalida(String detalle) {
        super("Firma del webhook inválida: " + detalle);
    }
}
