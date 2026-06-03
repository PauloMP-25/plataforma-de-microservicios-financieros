package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.respuestas.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudMetaAhorro;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudProgreso;
import com.cliente.aplicacion.puertos.ServicioMetaAhorro;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.libreria.comun.respuesta.ResultadoApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador para la gestión de metas de ahorro.
 *
 * Rutas:
 * POST /api/v1/clientes/metas → crear meta
 * GET /api/v1/clientes/metas → listar todas las metas del usuario
 * GET /api/v1/clientes/metas/activas → listar solo metas activas
 * GET /api/v1/clientes/metas/{metaId} → consultar una meta
 * PATCH /api/v1/clientes/metas/{metaId}/progreso → actualizar monto actual
 * DELETE /api/v1/clientes/metas/{metaId} → eliminar meta
 */
@RestController
@RequestMapping("/api/v1/clientes/metas")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ControladorMetaAhorro {

    private final ServicioMetaAhorro servicio;

    /**
     * Crea una nueva meta de ahorro.
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<RespuestaMetaAhorro>> crear(
            @Valid @RequestBody SolicitudMetaAhorro solicitud,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaMetaAhorro respuesta = servicio.crear(usuarioID, solicitud, ip);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResultadoApi.creado(respuesta, "Meta de ahorro creada con éxito."));
    }

    /**
     * Lista todas las metas del usuario autenticado.
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<RespuestaMetaAhorro>>> listar(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        List<RespuestaMetaAhorro> respuesta = servicio.listar(usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro recuperadas.", null));
    }

    /**
     * Lista solo las metas activas (no completadas), ordenadas por fecha límite.
     */
    @GetMapping("/activas")
    public ResponseEntity<ResultadoApi<List<RespuestaMetaAhorro>>> listarActivas(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        List<RespuestaMetaAhorro> respuesta = servicio.listarActivas(usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro activas recuperadas.", null));
    }

    /**
     * Filtra dinámicamente las metas de ahorro del usuario utilizando el Specification Pattern.
     */
    @GetMapping("/filtrar")
    public ResponseEntity<ResultadoApi<List<RespuestaMetaAhorro>>> filtrar(
            @RequestParam(required = false) Boolean completada,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate venceAntes,
            @RequestParam(required = false) Double progresoBajo) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        List<RespuestaMetaAhorro> respuesta = servicio.buscar(usuarioID, completada, venceAntes, progresoBajo);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro filtradas dinámicamente con éxito.", null));
    }

    /**
     * Consulta una meta específica del usuario.
     */
    @GetMapping("/{metaId}")
    public ResponseEntity<ResultadoApi<RespuestaMetaAhorro>> consultar(
            @PathVariable UUID metaId,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        RespuestaMetaAhorro respuesta = servicio.consultar(metaId, usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Meta de ahorro recuperada.", null));
    }

    /**
     * Actualiza el monto actual de una meta (progreso de ahorro).
     */
    @PatchMapping("/{metaId}/progreso")
    public ResponseEntity<ResultadoApi<RespuestaMetaAhorro>> actualizarProgreso(
            @PathVariable UUID metaId,
            @Valid @RequestBody SolicitudProgreso solicitud,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaMetaAhorro respuesta = servicio.actualizarProgreso(metaId, usuarioID, solicitud.montoActual(), ip);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Progreso de meta actualizado con éxito.", null));
    }

    /**
     * Elimina una meta de ahorro del usuario.
     */
    @DeleteMapping("/{metaId}")
    public ResponseEntity<ResultadoApi<Void>> eliminar(
            @PathVariable UUID metaId,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        servicio.eliminar(metaId, usuarioID, ip);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ResultadoApi.sinContenido("Meta de ahorro eliminada con éxito."));
    }
}
