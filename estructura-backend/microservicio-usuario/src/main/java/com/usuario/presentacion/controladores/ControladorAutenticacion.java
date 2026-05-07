package com.usuario.presentacion.controladores;

import com.usuario.aplicacion.dtos.*;
import com.usuario.aplicacion.servicios.ServicioAutenticacion;
import com.usuario.dominio.entidades.Usuario;
import com.usuario.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación y gestión de usuarios. Proporciona endpoints
 * para el ciclo de vida del usuario y seguridad.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAuth;

    // =========================================================================
    // Activacion de Cuenta
    // =========================================================================
    @PutMapping("/activar/{usuarioId}")
    public ResponseEntity<?> activarCuenta(@PathVariable UUID usuarioId, @RequestParam(required = false) String telefono, HttpServletRequest httpRequest) {
        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);
        servicioAuth.activarCuenta(usuarioId, telefono,ipCliente);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Cuenta activada correctamente",
                "timestamp", LocalDateTime.now()
        ));
    }

    // =========================================================================
    // Cambiar contraseña actual
    // =========================================================================
    @PutMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(
            @Valid @RequestBody SolicitudCambioPassword solicitud,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        UUID usuarioId = ((Usuario) authentication.getPrincipal()).getId();
        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);

        servicioAuth.cambiarPassword(usuarioId, solicitud, ipCliente);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Tu contraseña ha sido actualizada correctamente",
                "timestamp", LocalDateTime.now()
        ));
    }

    // =========================================================================
    // Login
    // =========================================================================
    @PostMapping("/login")
    public ResponseEntity<RespuestaAutenticacion> login(
            @Valid @RequestBody SolicitudLogin request,
            HttpServletRequest httpRequest) {

        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);
        RespuestaAutenticacion respuesta = servicioAuth.login(request, ipCliente);

        return ResponseEntity.ok(respuesta);
    }

    // =========================================================================
    // Cerrar Sesion
    // =========================================================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Authentication authentication, HttpServletRequest httpRequest) {
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);
            servicioAuth.registrarLogout(usuario.getId(), ipCliente);
        }
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }

    // =========================================================================
    // Eliminar cuenta
    // =========================================================================
    @DeleteMapping("/mi-cuenta")
    public ResponseEntity<?> solicitarEliminacionCuenta(
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);

        servicioAuth.eliminarCuenta(usuario.getId(), ipCliente);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Su cuenta ha sido desactivada exitosamente.",
                "timestamp", LocalDateTime.now()
        ));
    }

    // =========================================================================
    // Registro
    // =========================================================================
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody SolicitudRegistro request, HttpServletRequest httpRequest) {
        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);

        // El servicio ahora devuelve solo el UUID
        UUID usuarioId = servicioAuth.registrar(request, ipCliente);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Registro exitoso. Verifique su correo para activar la cuenta.",
                "id", usuarioId,
                "timestamp", LocalDateTime.now()
        ));
    }

    // =========================================================================
    // Recuperacion de contraseña
    // =========================================================================
    @PostMapping("/recuperar-password")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitudRecuperacion solicitud) {
        servicioAuth.iniciarRecuperacion(solicitud);
        return ResponseEntity.ok(Map.of("mensaje", "Se ha enviado un código a su correo."));
    }

    // =========================================================================
    // Resetear contraseña por codigo de verificacion
    // =========================================================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetearPassword(
            @RequestParam UUID registroId,
            @RequestParam String codigoOtp,
            @Valid @RequestBody SolicitudRestablecerPassword solicitud) {

        servicioAuth.restablecerPassword(registroId, codigoOtp, solicitud);

        return ResponseEntity.ok(Map.of(
                "mensaje", "Contraseña restablecida con éxito.",
                "timestamp", LocalDateTime.now()
        ));
    }
}
