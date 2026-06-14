package com.cliente.presentacion.controladores;

import com.cliente.aplicacion.dtos.respuestas.RespuestaDatosPersonales;
import com.cliente.aplicacion.dtos.solicitudes.SolicitudDatosPersonales;
import com.cliente.aplicacion.puertos.ServicioDatosPersonales;
import com.libreria.comun.utilidades.UtilidadIp;
import com.libreria.comun.utilidades.UtilidadSeguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.libreria.comun.respuesta.ResultadoApi;
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
 * POST /api/v1/clientes/perfil/inicial → crea perfil vacío (llamado por IAM)
 * PUT /api/v1/clientes/perfil/{usuarioId} → actualizar datos personales
 * GET /api/v1/clientes/perfil/{usuarioId} → consultar datos personales
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
     */
    @PostMapping("/inicial")
    @org.springframework.security.access.prepost.PreAuthorize("@seguridadService.esServicioInterno()")
    public ResponseEntity<ResultadoApi<RespuestaDatosPersonales>> crearPerfilInicial(
            @RequestParam UUID usuarioId) {
        log.info("Creando perfil inicial para usuarioId={}", usuarioId);
        RespuestaDatosPersonales respuesta = servicio.crearPerfil(usuarioId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ResultadoApi.creado(respuesta, "Perfil inicial creado exitosamente."));
    }

    /**
     * Actualiza los datos personales del cliente autenticado.
     */
    @PutMapping("/{usuarioId}")
    @org.springframework.security.access.prepost.PreAuthorize("@seguridadService.esElMismoUsuario(#usuarioId, authentication)")
    public ResponseEntity<ResultadoApi<RespuestaDatosPersonales>> actualizar(
            @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudDatosPersonales solicitud,
            HttpServletRequest request) {

        UUID usuarioToken = UtilidadSeguridad.obtenerUsuarioId();
        String ip = UtilidadIp.obtenerIpReal(request);
        RespuestaDatosPersonales respuesta = servicio.actualizar(usuarioId, usuarioToken, solicitud, ip);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Datos personales actualizados con éxito.", null));
    }

    /**
     * Consulta los datos personales del cliente autenticado.
     */
    @GetMapping("/{usuarioId}")
    @org.springframework.security.access.prepost.PreAuthorize("@seguridadService.esElMismoUsuario(#usuarioId, authentication)")
    public ResponseEntity<ResultadoApi<RespuestaDatosPersonales>> consultar(
            @PathVariable UUID usuarioId,
            HttpServletRequest request) {

        UUID usuarioID = UtilidadSeguridad.obtenerUsuarioId();
        RespuestaDatosPersonales respuesta = servicio.consultar(usuarioId, usuarioID);
        return ResponseEntity.ok(ResultadoApi.exito(respuesta, "Datos personales recuperados.", null));
    }
}
