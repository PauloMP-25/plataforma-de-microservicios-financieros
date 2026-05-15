package com.usuario.presentacion.controladores;

import com.libreria.comun.respuesta.ResultadoApi;
import com.usuario.aplicacion.dtos.*;
import com.usuario.aplicacion.servicios.IServicioAutenticacion;
import com.usuario.dominio.entidades.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación y gestión de usuarios.
 * Sigue los estándares de la plataforma LUKA utilizando ResultadoApi para todas las respuestas.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class ControladorAutenticacion {

    private final IServicioAutenticacion servicioAuth;

    /**
     * Activa la cuenta de un usuario recién registrado validando un código OTP.
     */
    @PutMapping("/activar/{usuarioId}")
    public ResponseEntity<ResultadoApi<String>> activarCuenta(
            @PathVariable UUID usuarioId,
            @RequestParam(required = false) String codigoOtp,
            @RequestParam(required = false) String telefono, 
            HttpServletRequest request) {
        
        log.info("[API] Intento de activación para usuario: {}", usuarioId);
        servicioAuth.activarCuenta(usuarioId, codigoOtp, telefono, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito("OK", "Cuenta activada correctamente. Ya puede iniciar sesión."));
    }

    /**
     * Solicita un nuevo código OTP para la activación de cuenta.
     */
    @PostMapping("/solicitar-otp/{usuarioId}")
    public ResponseEntity<ResultadoApi<String>> solicitarOtpActivacion(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudGenerarOtp solicitud) {
        
        log.info("[API] Solicitud de OTP de activación para usuario: {}", usuarioId);
        servicioAuth.solicitarOtpActivacion(usuarioId, solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito("OTP_ENVIADO", "El código ha sido enviado a su medio de contacto."));
    }

    /**
     * Realiza el login del usuario y retorna el token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ResultadoApi<RespuestaAutenticacion>> login(
            @Valid @RequestBody SolicitudLogin solicitud,
            HttpServletRequest request) {
        
        log.info("[API] Intento de login para: {}", solicitud.correo());
        RespuestaAutenticacion respuesta = servicioAuth.login(solicitud, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Autenticación exitosa."));
    }

    /**
     * Renueva el Access Token utilizando un Refresh Token válido.
     */
    @PostMapping("/refrescar-token")
    public ResponseEntity<ResultadoApi<RespuestaAutenticacion>> refrescarToken(
            @Valid @RequestBody SolicitudRefreshToken solicitud,
            HttpServletRequest request) {
        
        log.info("[API] Solicitud de refresco de token");
        RespuestaAutenticacion respuesta = servicioAuth.refrescarToken(solicitud, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Token renovado exitosamente."));
    }

    /**
     * Cierra la sesión del usuario actual e invalida el token.
     */
    @PostMapping("/logout")
    public ResponseEntity<ResultadoApi<String>> logout(Authentication authentication, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;

        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            servicioAuth.registrarLogout(usuario.getId(), token, request.getRemoteAddr());
        }
        return ResponseEntity.ok(ResultadoApi.exito("Sesión cerrada", "Sesión cerrada correctamente. El token ha sido invalidado."));
    }

    /**
     * Registra un nuevo usuario en el sistema.
     */
    @PostMapping("/registrar")
    public ResponseEntity<ResultadoApi<UUID>> registrar(
            @Valid @RequestBody SolicitudRegistro solicitud, 
            HttpServletRequest request) {
        
        log.info("[API] Solicitud de registro para usuario: {}", solicitud.nombreUsuario());
        UUID usuarioId = servicioAuth.registrar(solicitud, request.getRemoteAddr());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultadoApi.exito(usuarioId, "Registro exitoso. Revise su correo para activar la cuenta."));
    }

    /**
     * Inicia el flujo de recuperación de contraseña.
     */
    @PostMapping("/recuperar-solicitar")
    public ResponseEntity<ResultadoApi<String>> solicitarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacion solicitud) {
        
        log.info("[API] Solicitud de recuperación para: {}", solicitud.correo());
        servicioAuth.iniciarRecuperacion(solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito("SOLICITUD_PROCESADA", 
            "Si el correo ingresado existe y está activo, recibirá un código de verificación en breve."));
    }

    /**
     * Establece una nueva contraseña tras validar el código de recuperación.
     */
    @PostMapping("/recuperar-confirmar")
    public ResponseEntity<ResultadoApi<String>> resetearPassword(
            @RequestParam UUID registroId,
            @RequestParam String codigoOtp,
            @Valid @RequestBody SolicitudRestablecerPassword solicitud) {
        
        log.info("[API] Confirmación de recuperación de contraseña para registro: {}", registroId);
        servicioAuth.restablecerPassword(registroId, codigoOtp, solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito("PASSWORD_RESETEADO", "Su contraseña ha sido restablecida correctamente."));
    }

    /**
     * Permite a un usuario autenticado cambiar su contraseña.
     */
    @PutMapping("/cambiar-password")
    public ResponseEntity<ResultadoApi<String>> cambiarPassword(
            @Valid @RequestBody SolicitudCambioPassword solicitud,
            Authentication authentication,
            HttpServletRequest request) {

        UUID usuarioId = ((Usuario) authentication.getPrincipal()).getId();
        servicioAuth.cambiarPassword(usuarioId, solicitud, request.getRemoteAddr());

        return ResponseEntity.ok(ResultadoApi.exito("PASSWORD_ACTUALIZADO", "La contraseña ha sido actualizada correctamente."));
    }

    /**
     * Desactiva lógicamente la cuenta del usuario autenticado.
     */
    @DeleteMapping("/mi-cuenta")
    public ResponseEntity<ResultadoApi<String>> eliminarCuenta(
            Authentication authentication,
            HttpServletRequest request) {

        Usuario usuario = (Usuario) authentication.getPrincipal();
        servicioAuth.eliminarCuenta(usuario.getId(), request.getRemoteAddr());

        return ResponseEntity.ok(ResultadoApi.exito("CUENTA_ELIMINADA", "Su cuenta ha sido desactivada exitosamente."));
    }
}
