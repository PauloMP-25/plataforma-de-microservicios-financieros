package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.servicios.ServicioDatosPersonales;
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
 * Controlador para la gestión del perfil de datos personales del cliente.
 *
 * Rutas:
 *   POST   /api/v1/clientes/perfil/inicial          → crea perfil vacío (llamado por IAM)
 *   PUT    /api/v1/clientes/perfil/{usuarioId}       → actualizar datos personales
 *   GET    /api/v1/clientes/perfil/{usuarioId}       → consultar datos personales
 */
@RestController
@RequestMapping("/api/v1/clientes/perfil")
@RequiredArgsConstructor
@Slf4j
public class ControladorPerfil {

    private final ServicioDatosPersonales servicio;

    /**
     * Crea el perfil vacío inicial tras el registro.
     * Endpoint interno — sin autenticación JWT (permitAll en seguridad).
     * @param usuarioId
     * @return 
     */
    @PostMapping("/inicial")
    public ResponseEntity<RespuestaDatosPersonales> crearPerfilInicial(
            @RequestParam UUID usuarioId) {
        log.info("Creando perfil inicial para usuarioId={}", usuarioId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(servicio.crearPerfil(usuarioId));
    }

    /**
     * Actualiza los datos personales del cliente autenticado.
     * @param usuarioId
     * @param solicitud
     * @param request
     * @return 
     */
    @PutMapping("/{usuarioId}")
    public ResponseEntity<RespuestaDatosPersonales> actualizar(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudDatosPersonales solicitud,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity.ok(servicio.actualizar(usuarioId, usuarioIdToken, solicitud, ip));
    }

    /**
     * Consulta los datos personales del cliente autenticado.
     * @param usuarioId
     * @param request
     * @return 
     */
    @GetMapping("/{usuarioId}")
    public ResponseEntity<RespuestaDatosPersonales> consultar(
            @PathVariable UUID usuarioId,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.consultar(usuarioId, usuarioIdToken));
    }

    private UUID extraerUsuarioIdToken(HttpServletRequest request) {
        return (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);
    }
}
