package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.SolicitudCliente;
import com.cliente.aplicacion.dtos.RespuestaCliente;
import com.cliente.aplicacion.servicios.ServicioCliente;
import com.cliente.infraestructura.seguridad.FiltroJwt;
import com.cliente.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la gestión de datos de perfil de cliente. Optimizado para
 * usar ManejadorGlobalErrores.
 */
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Slf4j
public class ControladorCliente {

    private final ServicioCliente clienteService;

    /**
     * Crea un perfil básico tras el registro de un usuario.
     * @param usuarioId
     * @return 
     */
    @PostMapping("/inicial")
    public ResponseEntity<RespuestaCliente> crearPerfilInicial(@RequestParam UUID usuarioId) {
        log.info("Creando perfil inicial para usuario: {}", usuarioId);
        RespuestaCliente creado = clienteService.crearPerfilInicial(usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Completa o actualiza los datos del cliente (DNI, Dirección, etc).
     * @param usuarioId
     * @param requestDTO
     * @param request
     * @return 
     */
    @PutMapping("/actualizar/{usuarioId}")
    public ResponseEntity<RespuestaCliente> completarPerfil(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudCliente requestDTO,
            HttpServletRequest request) {

        String ipCliente = obtenerIpReal(request);
        UUID usuarioIdToken = (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);

        log.debug("Actualizando perfil. UsuarioId: {}, Solicitado por: {}", usuarioId, usuarioIdToken);

        RespuestaCliente actualizado = clienteService.actualizarPerfil(usuarioId, usuarioIdToken, requestDTO, ipCliente);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Obtiene la información del perfil de un cliente específico.
     * @param usuarioId
     * @param request
     * @return 
     */
    @GetMapping("/perfil/{usuarioId}")
    public ResponseEntity<RespuestaCliente> consultarPerfil(
            @PathVariable UUID usuarioId,
            HttpServletRequest request) {

        UUID usuarioIdToken = (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);
        RespuestaCliente perfil = clienteService.consultarPerfil(usuarioId, usuarioIdToken);
        return ResponseEntity.ok(perfil);
    }
    
    /**
     * Utilidad privada para extraer la IP considerando proxies.
     */
    private String obtenerIpReal(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return UtilidadIp.obtenerIpRemota(request);
    }
}
