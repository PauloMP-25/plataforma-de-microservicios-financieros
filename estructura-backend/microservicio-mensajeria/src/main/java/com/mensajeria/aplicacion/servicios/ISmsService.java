package com.mensajeria.aplicacion.servicios;

/**
 * Contrato del servicio de envío de SMS.
 * <p>
 * Desacopla la implementación de Twilio del dominio, permitiendo mock en tests
 * y futura migración de proveedor sin impacto en el resto del servicio.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface ISmsService {

    /**
     * Envía un código OTP de 6 dígitos al número de teléfono indicado mediante
     * un proveedor de SMS externo (actualmente Twilio).
     *
     * @param telefono Número destino en formato E.164 (ej. {@code +51987654321}).
     *                 Debe haber sido previamente validado por
     *                 {@link #esNumeroValido(String)}.
     * @param codigo   Código OTP de 6 dígitos que el usuario debe ingresar.
     * @throws RuntimeException si el proveedor externo rechaza el envío o no está
     *                          disponible.
     */
    void enviarCodigoVerificacion(String telefono, String codigo);

    /**
     * Valida que el número de teléfono cumple el formato E.164 requerido por el
     * proveedor SMS.
     *
     * @param telefono Número de teléfono a validar (con o sin código de país).
     * @return {@code true} si el número tiene formato E.164 válido (ej.
     *         {@code +51987654321}), {@code false} en caso contrario.
     */
    boolean esNumeroValido(String telefono);
}
