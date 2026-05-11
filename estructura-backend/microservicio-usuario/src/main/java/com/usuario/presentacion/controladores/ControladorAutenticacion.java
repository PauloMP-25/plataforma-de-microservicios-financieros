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
     * Activa la cuenta de un usuario recién registrado.
     */
    @PutMapping("/activar/{usuarioId}")
    public ResponseEntity<ResultadoApi<Void>> activarCuenta(
            @PathVariable UUID usuarioId, 
            @RequestParam(required = false) String telefono, 
            HttpServletRequest request) {
        
        log.info("[API] Solicitud de activación para usuario: {}", usuarioId);
        servicioAuth.activarCuenta(usuarioId, telefono, request.getRemoteAddr());
        
        return ResponseEntity.ok(ResultadoApi.exito(null, "Cuenta activada correctamente."));
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
    @PostMapping("/recuperar-password")
    public ResponseEntity<ResultadoApi<Void>> solicitarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacion solicitud) {
        
        log.info("[API] Solicitud de recuperación para: {}", solicitud.correo());
        servicioAuth.iniciarRecuperacion(solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito(null, "Se ha enviado un código de verificación a su medio de contacto."));
    }

    /**
     * Establece una nueva contraseña tras validar el código de recuperación.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ResultadoApi<Void>> resetearPassword(
            @RequestParam UUID registroId,
            @RequestParam String codigoOtp,
            @Valid @RequestBody SolicitudRestablecerPassword solicitud) {
        
        log.info("[API] Solicitud de reseteo de contraseña para registro: {}", registroId);
        servicioAuth.restablecerPassword(registroId, codigoOtp, solicitud);
        
        return ResponseEntity.ok(ResultadoApi.exito(null, "Contraseña restablecida con éxito."));
    }

    /**
     * Permite a un usuario autenticado cambiar su contraseña.
     */
    @PutMapping("/cambiar-password")
    public ResponseEntity<ResultadoApi<Void>> cambiarPassword(
            @Valid @RequestBody SolicitudCambioPassword solicitud,
            Authentication authentication,
            HttpServletRequest request) {

        UUID usuarioId = ((Usuario) authentication.getPrincipal()).getId();
        servicioAuth.cambiarPassword(usuarioId, solicitud, request.getRemoteAddr());

        return ResponseEntity.ok(ResultadoApi.exito(null, "La contraseña ha sido actualizada correctamente."));
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    @PostMapping("/logout")
    public ResponseEntity<ResultadoApi<Void>> logout(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            servicioAuth.registrarLogout(usuario.getId(), request.getRemoteAddr());
        }
        return ResponseEntity.ok(ResultadoApi.exito(null, "Sesión cerrada correctamente."));
    }

    /**
     * Desactiva lógicamente la cuenta del usuario autenticado.
     */
    @DeleteMapping("/mi-cuenta")
    public ResponseEntity<ResultadoApi<Void>> eliminarCuenta(
            Authentication authentication,
            HttpServletRequest request) {

        Usuario usuario = (Usuario) authentication.getPrincipal();
        servicioAuth.eliminarCuenta(usuario.getId(), request.getRemoteAddr());

        return ResponseEntity.ok(ResultadoApi.exito(null, "Su cuenta ha sido desactivada exitosamente."));
    }
}
