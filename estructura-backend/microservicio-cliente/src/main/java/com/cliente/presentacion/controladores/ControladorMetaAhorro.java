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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador para la gestión de metas de ahorro.
 *
 * Rutas:
 * POST /api/v1/clientes/metas → crear meta
 * PUT /api/v1/clientes/metas/{metaId} → actualizar detalles de meta
 * GET /api/v1/clientes/metas → listar todas las metas del usuario (paginado)
 * GET /api/v1/clientes/metas/activas → listar solo metas activas (paginado)
 * GET /api/v1/clientes/metas/{metaId} → consultar una meta
 * PATCH /api/v1/clientes/metas/{metaId}/progreso → actualizar monto actual
 * DELETE /api/v1/clientes/metas/{metaId} → eliminar (desactivar) meta
 */
import com.libreria.comun.respuesta.Paginacion;

@RestController
@RequestMapping("/api/v1/clientes/metas")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ControladorMetaAhorro {

    private final ServicioMetaAhorro servicio;

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

    @PutMapping("/{metaId}")
    public ResponseEntity<ResultadoApi<RespuestaMetaAhorro>> actualizarMeta(
            @PathVariable UUID metaId,
            @Valid @RequestBody SolicitudMetaAhorro solicitud,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaMetaAhorro respuesta = servicio.actualizarMeta(metaId, usuarioID, solicitud, ip);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Meta de ahorro actualizada con éxito.", null));
    }

    @GetMapping
    public ResponseEntity<ResultadoApi<Paginacion<RespuestaMetaAhorro>>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        Pageable pageable = PageRequest.of(page, size);
        Paginacion<RespuestaMetaAhorro> respuesta = servicio.listar(usuarioID, pageable);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro recuperadas.", null));
    }

    @GetMapping("/activas")
    public ResponseEntity<ResultadoApi<Paginacion<RespuestaMetaAhorro>>> listarActivas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        Pageable pageable = PageRequest.of(page, size);
        Paginacion<RespuestaMetaAhorro> respuesta = servicio.listarActivas(usuarioID, pageable);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro activas recuperadas.", null));
    }

    @GetMapping("/{metaId}")
    public ResponseEntity<ResultadoApi<RespuestaMetaAhorro>> consultar(
            @PathVariable UUID metaId,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        RespuestaMetaAhorro respuesta = servicio.consultar(metaId, usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Meta de ahorro recuperada.", null));
    }

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
