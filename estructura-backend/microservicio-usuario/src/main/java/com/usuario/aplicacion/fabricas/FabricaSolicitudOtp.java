package com.usuario.aplicacion.fabricas;

import com.usuario.aplicacion.dtos.solicitudes.SolicitudGenerarOtp;
import com.usuario.dominio.entidades.Usuario;
import com.libreria.comun.enums.TipoVerificacion;
import com.libreria.comun.enums.PropositoCodigo;

/**
 * Fábrica estática para centralizar y estandarizar la creación de DTOs de SolicitudGenerarOtp.
 * Evita duplicidad de instanciaciones y garantiza la coherencia de propósitos y canales.
 */
public final class FabricaSolicitudOtp {

    private FabricaSolicitudOtp() {
        // Constructor privado para evitar instanciación
    }

    /**
     * Construye la solicitud de generación de OTP para el registro de cuenta inicial.
     * Por defecto, este canal es siempre por Correo Electrónico (EMAIL) y no requiere teléfono.
     *
     * @param usuario El usuario que acaba de registrarse.
     * @return SolicitudGenerarOtp configurada para EMAIL y ACTIVACION_CUENTA.
     */
    public static SolicitudGenerarOtp paraActivacionRegistroInicial(Usuario usuario) {
        return new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                null,
                TipoVerificacion.EMAIL,
                PropositoCodigo.ACTIVACION_CUENTA
        );
    }

    /**
     * Construye la solicitud de generación de OTP cuando el usuario solicita manualmente un
     * reenvío o un cambio de canal de activación (ej. recibir el código por SMS o WhatsApp).
     *
     * @param usuario   El usuario que solicita el código.
     * @param tipo      Canal de entrega deseado (EMAIL, SMS, WHATSAPP).
     * @param telefono  Teléfono de destino (puede ser nulo si el canal elegido es EMAIL).
     * @return SolicitudGenerarOtp configurada con el canal elegido y ACTIVACION_CUENTA.
     */
    public static SolicitudGenerarOtp paraReenvioActivacionManual(Usuario usuario, TipoVerificacion tipo, String telefono) {
        return new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                telefono,
                tipo,
                PropositoCodigo.ACTIVACION_CUENTA
        );
    }

    /**
     * Construye la solicitud de generación de OTP para el flujo de recuperación y restablecimiento
     * de contraseña olvidada.
     *
     * @param usuario   El usuario que solicita recuperación.
     * @param tipo      Canal de entrega deseado (EMAIL, SMS, WHATSAPP).
     * @param telefono  Teléfono de destino recuperado del perfil (nulo si es EMAIL).
     * @return SolicitudGenerarOtp configurada con el canal y RESTABLECER_PASSWORD.
     */
    public static SolicitudGenerarOtp paraRecuperacionPassword(Usuario usuario, TipoVerificacion tipo, String telefono) {
        return new SolicitudGenerarOtp(
                usuario.getId(),
                usuario.getCorreo(),
                telefono,
                tipo,
                PropositoCodigo.RESTABLECER_PASSWORD
        );
    }
}
