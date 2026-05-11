package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.RespuestaLimiteGasto;
import com.cliente.aplicacion.dtos.SolicitudLimiteGasto;
import com.cliente.aplicacion.servicios.ServicioLimiteGasto;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.libreria.comun.respuesta.ResultadoApi;
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
     * @param solicitud DTO con los detalles del límite a crear
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con el límite creado
     */
    @PostMapping
    public ResponseEntity<ResultadoApi<RespuestaLimiteGasto>> crear(
            @Valid @RequestBody SolicitudLimiteGasto solicitud,
            HttpServletRequest request) {
        UUID usuarioToken = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaLimiteGasto respuesta = servicio.crear(usuarioToken, solicitud, ip);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultadoApi.creado(respuesta, "Límite de gasto creado con éxito."));
    }

    /**
     * Obtiene únicamente el límite que se encuentra activo y vigente.
     *
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con el límite de gasto activo
     */
    @GetMapping("/activo")
    public ResponseEntity<ResultadoApi<RespuestaLimiteGasto>> obtenerActivo(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        RespuestaLimiteGasto respuesta = servicio.obtenerActivo(usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Límite activo recuperado exitosamente.", null));
    }

    /**
     * Actualiza parcialmente el límite global ACTIVO. Se utiliza PATCH porque
     * solo se modifican montos o porcentajes.
     *
     * @param solicitud DTO con los datos del límite a actualizar
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con el límite de gasto actualizado
     */
    @PatchMapping
    public ResponseEntity<ResultadoApi<RespuestaLimiteGasto>> actualizar(
            @Valid @RequestBody SolicitudLimiteGasto solicitud,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaLimiteGasto respuesta = servicio.actualizar(usuarioID, solicitud, ip);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Límite de gasto actualizado con éxito.", null));
    }

    /**
     * Lista todo el historial de límites (activos e inactivos).
     * 
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi con la lista de todos los límites creados
     */
    @GetMapping
    public ResponseEntity<ResultadoApi<List<RespuestaLimiteGasto>>> listarHistorial(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        List<RespuestaLimiteGasto> respuesta = servicio.listarHistorial(usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Historial de límites de gasto recuperado.", null));
    }

    /**
     * Realiza una eliminación lógica desactivando el límite activo. Esto
     * permite al usuario crear uno nuevo inmediatamente después.
     * 
     * @param request Petición HTTP para extraer la IP
     * @return ResultadoApi sin contenido confirmando la eliminación
     */
    @DeleteMapping
    public ResponseEntity<ResultadoApi<Void>> desactivarLimiteActivo(HttpServletRequest request) {
        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        servicio.eliminar(usuarioID, ip);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ResultadoApi.sinContenido("Límite de gasto desactivado exitosamente."));
    }
}
