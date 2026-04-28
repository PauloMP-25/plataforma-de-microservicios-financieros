package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.servicios.ServicioMetaAhorro;
import com.cliente.infraestructura.seguridad.FiltroJwt;
import com.cliente.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controlador para la gestión de metas de ahorro.
 *
 * Rutas:
 *   POST   /api/v1/clientes/metas                    → crear meta
 *   GET    /api/v1/clientes/metas                    → listar todas las metas del usuario
 *   GET    /api/v1/clientes/metas/activas            → listar solo metas activas
 *   GET    /api/v1/clientes/metas/{metaId}           → consultar una meta
 *   PATCH  /api/v1/clientes/metas/{metaId}/progreso  → actualizar monto actual
 *   DELETE /api/v1/clientes/metas/{metaId}           → eliminar meta
 */
@RestController
@RequestMapping("/api/v1/clientes/metas")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ControladorMetaAhorro {

    private final ServicioMetaAhorro servicio;

    /** Crea una nueva meta de ahorro.
     * @param solicitud
     * @param request
     * @return  */
    @PostMapping
    public ResponseEntity<RespuestaMetaAhorro> crear(
            @Valid @RequestBody SolicitudMetaAhorro solicitud,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(servicio.crear(usuarioIdToken, solicitud, ip));
    }

    /** Lista todas las metas del usuario autenticado.
     * @param request
     * @return  */
    @GetMapping
    public ResponseEntity<List<RespuestaMetaAhorro>> listar(HttpServletRequest request) {
        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.listar(usuarioIdToken));
    }

    /** Lista solo las metas activas (no completadas), ordenadas por fecha límite.
     * @param request
     * @return  */
    @GetMapping("/activas")
    public ResponseEntity<List<RespuestaMetaAhorro>> listarActivas(HttpServletRequest request) {
        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.listarActivas(usuarioIdToken));
    }

    /** Consulta una meta específica del usuario.
     * @param metaId
     * @param request
     * @return  */
    @GetMapping("/{metaId}")
    public ResponseEntity<RespuestaMetaAhorro> consultar(
            @PathVariable UUID metaId,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.consultar(metaId, usuarioIdToken));
    }

    /**
     * Actualiza el monto actual de una meta (progreso de ahorro).
     * Si alcanza el objetivo, marca la meta como completada y publica el evento.
     * @param metaId
     * @param montoActual
     * @param request
     * @return 
     */
    @PatchMapping("/{metaId}/progreso")
    public ResponseEntity<RespuestaMetaAhorro> actualizarProgreso(
            @PathVariable UUID metaId,
            @RequestParam
            @DecimalMin(value = "0.00", message = "El monto no puede ser negativo")
            @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido")
            BigDecimal montoActual,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity.ok(
                servicio.actualizarProgreso(metaId, usuarioIdToken, montoActual, ip));
    }

    /** Elimina una meta de ahorro del usuario.
     * @param metaId
     * @param request
     * @return  */
    @DeleteMapping("/{metaId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID metaId,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        servicio.eliminar(metaId, usuarioIdToken, ip);
        return ResponseEntity.noContent().build();
    }

    private UUID extraerUsuarioIdToken(HttpServletRequest request) {
        return (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);
    }
}
