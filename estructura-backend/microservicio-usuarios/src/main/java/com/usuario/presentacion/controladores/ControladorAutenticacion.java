package com.usuario.presentacion.controladores;

import com.usuario.aplicacion.dtos.NuevoPasswordDTO;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.RespuestaRegistro;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRecuperacion;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.aplicacion.servicios.ServicioAutenticacion;
import com.usuario.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación y gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAuth;

    // =========================================================================
    // Login
    // =========================================================================
    @PostMapping("/login")
    public ResponseEntity<RespuestaAutenticacion> login(@Valid @RequestBody SolicitudLogin request, HttpServletRequest httpRequest) {
        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);

        RespuestaAutenticacion respuesta = servicioAuth.login(request, ipCliente);

        return ResponseEntity.ok(respuesta);
    }

    // =========================================================================
    // Registro
    // =========================================================================
    @PostMapping("/registrar")
    public ResponseEntity<RespuestaRegistro> registrar(@Valid @RequestBody SolicitudRegistro request) {
        log.debug("POST /auth/registrar — usuario: {}", request.nombreUsuario());

        // Sin (T), sin com.usuario.etc... solo el tipo:
        RespuestaRegistro respuesta = servicioAuth.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // =========================================================================
    // Confirmación de email
    // =========================================================================
    @PutMapping("/activar/{usuarioId}")
    public ResponseEntity<?> activarCuenta(@PathVariable UUID usuarioId) {
        log.debug("Petición de activación recibida para usuarioId: {}", usuarioId);
        try {
            servicioAuth.activarCuenta(usuarioId);
            return ResponseEntity.ok(Map.of("mensaje", "Cuenta activada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // Recuperacion de contraseña
    // =========================================================================
    @PostMapping("/recuperar-password")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitudRecuperacion solicitud) {
        servicioAuth.iniciarRecuperacion(solicitud);
        return ResponseEntity.ok(Map.of("mensaje", "Se ha enviado un código a su correo."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetearPassword(@Valid @RequestBody NuevoPasswordDTO solicitud) {
        servicioAuth.completarRecuperacion(solicitud);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada con éxito."));
    }
}
