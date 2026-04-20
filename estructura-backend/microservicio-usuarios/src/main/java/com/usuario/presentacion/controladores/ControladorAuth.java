package com.usuario.presentacion.controladores;

import com.usuario.aplicacion.dtos.ErrorApi;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.RespuestaRegistro;
import com.usuario.aplicacion.dtos.SolicitudLogin;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación y gestión de usuarios.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class ControladorAuth {

    private final ServicioAutenticacion servicioAuth;

    // =========================================================================
    // Login
    // =========================================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody SolicitudLogin request,
            HttpServletRequest httpRequest) {

        String ipCliente = UtilidadIp.obtenerIpRemota(httpRequest);
        log.debug("POST /auth/login — ip: {}, username: {}", ipCliente, request.getNombreUsuario());

        try {
            RespuestaAutenticacion respuesta = servicioAuth.login(request, ipCliente);
            return ResponseEntity.ok(respuesta);

        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorApi.of(401, "CREDENCIALES_INVALIDAS",
                            ex.getMessage(), httpRequest.getRequestURI()));

        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorApi.of(403, "CUENTA_DESHABILITADA",
                            ex.getMessage(), httpRequest.getRequestURI()));

        } catch (LockedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorApi.of(403, "CUENTA_BLOQUEADA",
                            ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    // =========================================================================
    // Registro
    // =========================================================================
    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(
            @Valid @RequestBody SolicitudRegistro request,
            HttpServletRequest httpRequest) {

        log.debug("POST /auth/registrar — username: {}, email: {}", request.getNombreUsuario(), request.getCorreo());

        try {
            RespuestaRegistro respuesta = servicioAuth.registrar(request);
            // Devolvemos el objeto con estado 201 Created o 202 Accepted
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ErrorApi.of(400, "ERROR_VALIDACION",
                            ex.getMessage(), httpRequest.getRequestURI()));

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorApi.of(409, "CONFLICTO",
                            ex.getMessage(), httpRequest.getRequestURI()));
        }
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
}
