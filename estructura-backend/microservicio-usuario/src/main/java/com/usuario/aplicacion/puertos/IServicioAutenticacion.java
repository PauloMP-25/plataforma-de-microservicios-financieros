package com.usuario.aplicacion.puertos;

import com.usuario.aplicacion.dtos.solicitudes.*;
import com.usuario.aplicacion.dtos.respuestas.*;
import java.util.UUID;

/**
 * Interfaz que define las operaciones de autenticación y gestión de usuarios.
 * Sigue el principio de Inversión de Dependencias (SOLID).
 */
public interface IServicioAutenticacion {

    /**
     * Activa la cuenta de un usuario recién registrado.
     *
     * @param correo Correo electrónico del usuario a activar.
     * @param codigoOtp Código OTP (opcional si la activación es directa).
     * @param telefono  Teléfono asociado (opcional).
     * @param ipCliente Dirección IP del cliente para la auditoría.
     */
    void activarCuenta(String correo, String codigoOtp, String telefono, String ipCliente);

    /**
     * Activa la cuenta de un usuario recién registrado por su ID.
     *
     * @param usuarioId ID único del usuario a activar.
     * @param telefono  Teléfono verificado a sincronizar (opcional).
     */
    void activarCuentaPorId(UUID usuarioId, String telefono);

    /**
     * Cambia la contraseña de un usuario autenticado.
     * 
     * @param usuarioId ID del usuario.
     * @param solicitud DTO con las contraseñas actual y nueva.
     * @param ipCliente Dirección IP de origen para auditoría.
     */
    void cambiarPassword(UUID usuarioId, SolicitudCambioPassword solicitud, String ipCliente);

    /**
     * Inicia el flujo de recuperación de contraseña enviando un código OTP.
     * 
     * @param solicitud DTO con los datos de recuperación.
     * @return ID del registro de solicitud generado.
     */
    UUID iniciarRecuperacion(SolicitudRecuperacion solicitud);

    /**
     * Restablece la contraseña de un usuario validando un código OTP.
     *
     * @param solicitud DTO con el correo, código OTP y la nueva contraseña.
     */
    void restablecerPassword(SolicitudRestablecerPassword solicitud);

    /**
     * Desactiva lógicamente la cuenta de un usuario.
     * 
     * @param usuarioId ID del usuario.
     * @param ipCliente Dirección IP de origen para auditoría.
     */
    void eliminarCuenta(UUID usuarioId, String ipCliente);

    /**
     * Realiza la autenticación del usuario y genera un token JWT.
     * 
     * @param request   DTO con credenciales de acceso.
     * @param ipCliente Dirección IP de origen para auditoría.
     * @return DTO con el token y datos básicos del usuario.
     */
    RespuestaAutenticacion login(SolicitudLogin request, String ipCliente);

    /**
     * Renueva el token de acceso utilizando un refresh token válido.
     * 
     * @param solicitud DTO con el refresh token.
     * @param ipCliente Dirección IP de origen para auditoría.
     * @return Nueva respuesta de autenticación.
     */
    RespuestaAutenticacion refrescarToken(SolicitudRefreshToken solicitud, String ipCliente);

    /**
     * Registra un nuevo usuario en la plataforma deshabilitado por defecto.
     * 
     * @param request   DTO con los datos de registro.
     * @param ipCliente Dirección IP de origen para auditoría.
     * @return ID del usuario creado.
     */
    UUID registrar(SolicitudRegistro request, String ipCliente);

    /**
     * Registra el evento de cierre de sesión e invalida el token.
     * 
     * @param usuarioId ID del usuario.
     * @param token     Token JWT a invalidar.
     * @param ipCliente Dirección IP de origen para auditoría.
     */
    void registrarLogout(UUID usuarioId, String token, String ipCliente);

    /**
     * Solicita un nuevo código OTP para la activación de cuenta.
     * 
     * @param usuarioId ID del usuario.
     * @param solicitud DTO con el medio de envío.
     */
    void solicitarOtpActivacion(com.usuario.aplicacion.dtos.solicitudes.SolicitudReenvioOtp solicitud);

    /**
     * Sincroniza el teléfono verificado de un usuario, usualmente después de una
     * validación OTP exitosa en el ms-mensajeria.
     * 
     * @param usuarioId ID del usuario.
     * @param telefono  Número de teléfono verificado.
     */
    void sincronizarTelefono(UUID usuarioId, String telefono);
}
