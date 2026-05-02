package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.*;
import com.cliente.aplicacion.servicios.ServicioLimiteGasto;
import com.cliente.infraestructura.seguridad.FiltroJwt;
import com.cliente.infraestructura.utilidades.UtilidadIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Controlador para la gestión de límites de gasto por categoría.
 *
 * Rutas: POST /api/v1/clientes/limites → crear límite GET
 * /api/v1/clientes/limites → listar todos los límites del usuario PUT
 * /api/v1/clientes/limites/{categoriaId} → actualizar un límite existente
 * DELETE /api/v1/clientes/limites/{categoriaId} → eliminar un límite
 */
@RestController
@RequestMapping("/api/v1/clientes/limites")
@RequiredArgsConstructor
@Slf4j
public class ControladorLimiteGasto {

    private final ServicioLimiteGasto servicio;

    /**
     * Crea un nuevo límite global. Si hay uno vigente, falla. Si el actual está
     * vencido, lo desactiva automáticamente.
     *
     * @param solicitud
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<RespuestaLimiteGasto> crear(
            @Valid @RequestBody SolicitudLimiteGasto solicitud,
            HttpServletRequest request) {
        UUID uId = extraerUsuarioIdToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.crear(uId, solicitud, UtilidadIp.obtenerIpRemota(request)));
    }

    /**
     * Obtiene únicamente el límite que se encuentra activo y vigente.
     *
     * @param request
     * @return
     */
    @GetMapping("/activo")
    public ResponseEntity<RespuestaLimiteGasto> obtenerActivo(HttpServletRequest request) {
        UUID uId = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.obtenerActivo(uId));
    }

    /**
     * Actualiza parcialmente el límite global ACTIVO. Se utiliza PATCH porque
     * solo se modifican montos o porcentajes.
     *
     * @param solicitud
     * @param request
     * @return
     */
    @PatchMapping
    public ResponseEntity<RespuestaLimiteGasto> actualizar(
            @Valid @RequestBody SolicitudLimiteGasto solicitud,
            HttpServletRequest request) {

        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip = UtilidadIp.obtenerIpRemota(request);
        return ResponseEntity.ok(
                servicio.actualizar(usuarioIdToken, solicitud, ip));
    }

    /**
     * Lista todo el historial de límites (activos e inactivos).
     * @param request
     * @return 
     */
    @GetMapping
    public ResponseEntity<List<RespuestaLimiteGasto>> listarHistorial(HttpServletRequest request) {
        UUID usuarioID = extraerUsuarioIdToken(request);
        return ResponseEntity.ok(servicio.listarHistorial(usuarioID));
    }

    /**
     * Realiza una eliminación lógica desactivando el límite activo. Esto
     * permite al usuario crear uno nuevo inmediatamente después.
     * @param request
     * @return 
     */
    @DeleteMapping
    public ResponseEntity<Void> desactivarLimiteActivo(HttpServletRequest request) {
        UUID usuarioIdToken = extraerUsuarioIdToken(request);
        String ip = UtilidadIp.obtenerIpRemota(request);

        servicio.eliminar(usuarioIdToken, ip);
        return ResponseEntity.noContent().build();
    }

    private UUID extraerUsuarioIdToken(HttpServletRequest request) {
        return (UUID) request.getAttribute(FiltroJwt.ATTR_USUARIO_ID);
    }
}
