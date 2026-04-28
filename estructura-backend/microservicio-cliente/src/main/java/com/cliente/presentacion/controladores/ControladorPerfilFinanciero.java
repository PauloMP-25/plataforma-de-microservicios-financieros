package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.servicios.ServicioPerfilFinanciero;
import com.cliente.infraestructura.seguridad.FiltroJwt;
import com.cliente.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para el perfil financiero del cliente.
 *
 * Rutas:
 *   PUT  /api/v1/clientes/perfil-financiero/{usuarioId} → crear o actualizar perfil financiero
 *   GET  /api/v1/clientes/perfil-financiero/{usuarioId} → consultar perfil financiero
 */
@RestController
@RequestMapping("/api/v1/clientes/perfil-financiero")
@RequiredArgsConstructor
@Slf4j
public class ControladorPerfilFinanciero {

    private final ServicioPerfilFinanciero servicio;

    /**
     * Crea o actualiza el perfil financiero del usuario autenticado (upsert).
     * @param usuarioId
     * @param solicitud
     * @param request
     * @return 
     */
    @PutMapping("/{usuarioId}")
    public ResponseEntity<RespuestaPerfilFinanciero> guardarOActualizar(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudPerfilFinanciero solicitud,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity.ok(
                servicio.guardarOActualizar(usuarioId, usuarioIdToken, solicitud, ip));
    }

    /**
     * Consulta el perfil financiero del usuario autenticado.
     * @param usuarioId
     * @param request
     * @return 
     */
    @GetMapping("/{usuarioId}")
    public ResponseEntity<RespuestaPerfilFinanciero> consultar(
            @PathVariable UUID usuarioId,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.consultar(usuarioId, usuarioIdToken));
    }

    private UUID extraerUsuarioIdToken(HttpServletRequest request) {
        return (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);
    }
}