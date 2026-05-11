package com.mensajeria.aplicacion.servicios;

import com.mensajeria.dominio.entidades.CodigoVerificacion.PropositoCodigo;

/**
 * Contrato del servicio principal de mensajería y OTP.
 * <p>
 * Expone las operaciones de generación, validación y restricción de códigos
 * de un solo uso (OTP), diferenciando flujos de activación y recuperación.
 * </p>
 *
 * @author Paulo Moron
 * @version 1.1.0
 */
public interface IMensajeriaService {

    /**
     * Genera un código OTP de 6 dígitos, lo persiste y lo envía al canal
     * indicado en la solicitud (EMAIL o SMS). Aplica verificación de bloqueo y
     * límite diario antes del envío.
     *
     * @param solicitud DTO con los datos del usuario (ID, email, teléfono,
     *                  canal y propósito).
     * @return {@code RespuestaGeneracion} con el estado del envío y el canal
     *         utilizado.
     * @throws com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion
     *             si el usuario está bloqueado por intentos fallidos previos.
     * @throws com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException
     *             si el usuario ya agotó los 3 códigos diarios para ese propósito.
     */
    com.mensajeria.aplicacion.dtos.RespuestaGeneracion generarYEnviarCodigo(
            com.mensajeria.aplicacion.dtos.SolicitudGenerarCodigo solicitud);

    /**
     * Valida el OTP para el flujo de activación de cuenta. Si es correcto,
     * notifica al ms-usuario para activar la cuenta y sincronizar el teléfono.
     *
     * @param solicitud DTO con el ID del usuario y el código OTP ingresado.
     * @return {@code RespuestaValidacion} confirmando la activación exitosa.
     * @throws com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion
     *             si el usuario ya está bloqueado.
     * @throws com.mensajeria.aplicacion.excepciones.CodigoInvalidoException
     *             si el código es incorrecto o ya fue usado.
     */
    com.mensajeria.aplicacion.dtos.RespuestaValidacion validarParaActivacion(
            com.mensajeria.aplicacion.dtos.SolicitudValidarCodigo solicitud);

    /**
     * Valida el OTP para el flujo de recuperación de contraseña. Retorna el UUID
     * del usuario asociado al código para que el ms-usuario inicie el reset.
     *
     * @param registroId UUID del registro OTP, enviado por el ms-usuario como
     *                   identificador del proceso de recuperación.
     * @param codigoStr  Código OTP de 6 dígitos ingresado por el usuario.
     * @return UUID del usuario propietario del código, para ser usado por el
     *         ms-usuario al generar el token de reset.
     * @throws IllegalArgumentException si el par (registroId, código) no existe o
     *                                  ya fue usado.
     * @throws IllegalStateException    si el código ya expiró.
     */
    java.util.UUID validarCodigoYObtenerUsuario(java.util.UUID registroId, String codigoStr);

    /**
     * Valida de forma anticipada las restricciones de bloqueo y límite diario
     * para el usuario dado, sin generar ningún código. Usado por el controlador
     * en el endpoint {@code /validar-limite}.
     *
     * @param usuarioId UUID del usuario a verificar.
     * @param proposito Propósito del OTP ({@code ACTIVACION_CUENTA} o
     *                  {@code RECUPERACION_PASSWORD}) para verificar el límite
     *                  correcto.
     * @throws com.mensajeria.aplicacion.excepciones.UsuarioBloqueadoExcepcion
     *             si el usuario está bloqueado.
     * @throws com.mensajeria.aplicacion.excepciones.LimiteCodigosExcedidoException
     *             si ya superó el límite diario.
     */
    void verificarRestricciones(java.util.UUID usuarioId, PropositoCodigo proposito);
}
