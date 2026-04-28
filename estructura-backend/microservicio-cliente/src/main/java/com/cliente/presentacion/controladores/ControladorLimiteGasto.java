package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.servicios.ServicioLimiteGasto;
import com.cliente.infraestructura.seguridad.FiltroJwt;
import com.cliente.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador para la gestión de límites de gasto por categoría.
 *
 * Rutas:
 *   POST   /api/v1/clientes/limites                    → crear límite
 *   GET    /api/v1/clientes/limites                    → listar todos los límites del usuario
 *   PUT    /api/v1/clientes/limites/{categoriaId}      → actualizar un límite existente
 *   DELETE /api/v1/clientes/limites/{categoriaId}      → eliminar un límite
 */
@RestController
@RequestMapping("/api/v1/clientes/limites")
@RequiredArgsConstructor
@Slf4j
public class ControladorLimiteGasto {

    private final ServicioLimiteGasto servicio;

    /** Crea un nuevo límite de gasto para una categoría.
     * @param solicitud
     * @param request
     * @return  */
    @PostMapping
    public ResponseEntity<RespuestaLimiteGasto> crear(
            @Valid @RequestBody SolicitudLimiteGasto solicitud,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(servicio.crear(usuarioIdToken, solicitud, ip));
    }

    /** Lista todos los límites del usuario autenticado.
     * @param request
     * @return  */
    @GetMapping
    public ResponseEntity<List<RespuestaLimiteGasto>> listar(HttpServletRequest request) {
        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.listar(usuarioIdToken));
    }

    /**
     * Actualiza el monto límite y/o el porcentaje de alerta de una categoría.
     * Usa el nombre de la categoría como identificador en la URL.
     * @param categoriaId
     * @param solicitud
     * @param request
     * @return 
     */
    @PutMapping("/{categoriaId}")
    public ResponseEntity<RespuestaLimiteGasto> actualizar(
            @PathVariable String categoriaId,
            @Valid @RequestBody SolicitudLimiteGasto solicitud,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity.ok(
                servicio.actualizar(usuarioIdToken, categoriaId, solicitud, ip));
    }

    /** Elimina el límite de una categoría específica.
     * @param categoriaId
     * @param request
     * @return  */
    @DeleteMapping("/{categoriaId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable String categoriaId,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip           = UtilidadIp.obtenerIpRemota(request);
        servicio.eliminar(usuarioIdToken, categoriaId, ip);
        return ResponseEntity.noContent().build();
    }

    private UUID extraerUsuarioIdToken(HttpServletRequest request) {
        return (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);
    }
}
