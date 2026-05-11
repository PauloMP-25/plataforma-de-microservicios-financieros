package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.RespuestaMetaAhorro;
import com.cliente.aplicacion.dtos.SolicitudMetaAhorro;
import com.cliente.aplicacion.servicios.ServicioMetaAhorro;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import com.libreria.comun.respuesta.ResultadoApi;
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
     * 
     * @param solicitud DTO con los datos de la meta a crear
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con la meta creada
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
     * 
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con la lista de metas
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<RespuestaMetaAhorro>>> listar(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        List<RespuestaMetaAhorro> respuesta = servicio.listar(usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro recuperadas.", null));
    }

    /**
     * Lista solo las metas activas (no completadas), ordenadas por fecha límite.
     * 
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con la lista de metas activas
     */
    @GetMapping("/activas")
    public ResponseEntity<ResultadoApi<List<RespuestaMetaAhorro>>> listarActivas(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        List<RespuestaMetaAhorro> respuesta = servicio.listarActivas(usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Metas de ahorro activas recuperadas.", null));
    }

    /**
     * Consulta una meta específica del usuario.
     * 
     * @param metaId Identificador único de la meta
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con los datos de la meta
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
     * Si alcanza el objetivo, marca la meta como completada y publica el evento.
     * 
     * @param metaId Identificador único de la meta
     * @param montoActual Nuevo monto ahorrado
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con la meta actualizada
     */
    @PatchMapping("/{metaId}/progreso")
    public ResponseEntity<ResultadoApi<RespuestaMetaAhorro>> actualizarProgreso(
            @PathVariable UUID metaId,
            @RequestParam @DecimalMin(value = "0.00", message = "El monto no puede ser negativo") @Digits(integer = 10, fraction = 2, message = "Formato de monto inválido") BigDecimal montoActual,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaMetaAhorro respuesta = servicio.actualizarProgreso(metaId, usuarioID, montoActual, ip);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Progreso de meta actualizado con éxito.", null));
    }

    /**
     * Elimina una meta de ahorro del usuario.
     * 
     * @param metaId Identificador único de la meta
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi sin contenido
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
