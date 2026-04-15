package com.usuario.presentacion.controladores;

import com.usuario.aplicacion.dtos.ErrorApi;
import com.usuario.aplicacion.dtos.RespuestaAutenticacion;
import com.usuario.aplicacion.dtos.RespuestaRegistro;
import com.usuario.aplicacion.dtos.SolicitudLogin;
import com.usuario.aplicacion.dtos.SolicitudRegistro;
import com.usuario.aplicacion.servicios.ServicioAutenticacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

        String ipCliente = obtenerIpCliente(httpRequest);
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
    @GetMapping("/confirmar-email")
    public ResponseEntity<?> confirmarEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        log.debug("GET /auth/confirm-email — token: {}", token);

        try {
            String mensaje = servicioAuth.confirmarCorreo(token);
            return ResponseEntity.ok(mensaje);

        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest()
                    .body(ErrorApi.of(400, "TOKEN_INVALIDO",
                            ex.getMessage(), httpRequest.getRequestURI()));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String obtenerIpCliente(HttpServletRequest request) {

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
